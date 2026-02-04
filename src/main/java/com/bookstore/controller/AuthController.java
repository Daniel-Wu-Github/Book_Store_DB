package com.bookstore.controller;

import com.bookstore.dto.CreateUserRequest;
import com.bookstore.dto.UserDto;
import com.bookstore.service.UserService;
import com.bookstore.service.MagicLinkService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MagicLinkService magicLinkService;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, MagicLinkService magicLinkService, UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.magicLinkService = magicLinkService;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/magic-link")
    public ResponseEntity<?> requestMagicLink(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = body.get("email");
        if (email == null || email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "email required"));

        // create and send token; service returns Optional.empty() if user not found
        try {
            var result = magicLinkService.createAndSendToken(email, request);
            if (result.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "email not found"));
            }
            // If result contains null link -> sending failed
            if (result.get() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "failed to send email"));
            }
            return ResponseEntity.ok(Map.of("message", "magic link sent"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/magic/consume")
    public ResponseEntity<?> consumeMagic(@RequestParam("token") String token, @RequestParam(value = "redirect", required = false) String redirect, HttpServletRequest request) {
        if (token == null || token.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "token required"));
        var userOpt = magicLinkService.consumeToken(token);
        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid or expired token"));

        var user = userOpt.get();
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(user.getUsername());
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession(true);
            String dest = (redirect == null || redirect.isBlank()) ? "/" : redirect;
            return ResponseEntity.status(HttpStatus.FOUND).header("Location", dest).build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "failed to sign in user"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody CreateUserRequest req) {
        try {
            UserDto dto = userService.createUser(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        Authentication auth = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
        // ensure session created
        request.getSession(true);
        return userService.findByUsername(username).map(ResponseEntity::ok).orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return userService.findByUsername(principal.getName()).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
