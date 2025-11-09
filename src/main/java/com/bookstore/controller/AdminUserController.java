package com.bookstore.controller;

import com.bookstore.dto.UpdateUserRequest;
import com.bookstore.dto.UserDto;
import com.bookstore.model.User;
import com.bookstore.repository.UserRepository;
import com.bookstore.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminUserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> listAll() {
        List<UserDto> list = userRepository.findAll().stream().map(u -> {
            UserDto dto = new UserDto();
            dto.setId(u.getId()); dto.setUsername(u.getUsername()); dto.setEmail(u.getEmail());
            dto.setRoles(u.getRoles()); dto.setEnabled(u.isEnabled()); dto.setCreatedAt(u.getCreatedAt());
            dto.setUpdatedAt(u.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping(path = "{id}")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        Optional<User> u = userRepository.findById(id);
        return u.map(user -> {
            UserDto dto = userService.findByUsername(user.getUsername()).orElse(null);
            return ResponseEntity.ok(dto);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody com.bookstore.dto.CreateUserRequest req) {
        UserDto dto = userService.createUser(req);
        return ResponseEntity.status(201).body(dto);
    }

    @PutMapping(path = "{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        return userRepository.findById(id).map(u -> {
            if (req.getEmail() != null) u.setEmail(req.getEmail());
            if (req.getRoles() != null) u.setRoles(req.getRoles());
            if (req.getEnabled() != null) u.setEnabled(req.getEnabled());
            User saved = userRepository.save(u);
            UserDto dto = userService.findByUsername(saved.getUsername()).orElse(null);
            return ResponseEntity.ok(dto);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping(path = "{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
