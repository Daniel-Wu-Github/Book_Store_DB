package com.bookstore.controller;

import com.bookstore.dto.order.CreateOrderRequest;
import com.bookstore.dto.order.OrderDto;
import com.bookstore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody CreateOrderRequest req, Principal principal, Authentication authentication) {
        String username = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            username = authentication.getName();
        } else if (principal != null) {
            username = principal.getName();
        }
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("authentication required");
        }
        try {
            OrderDto dto = orderService.placeOrder(username, req);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> listMyOrders(Principal principal, Authentication authentication) {
        String username = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            username = authentication.getName();
        } else if (principal != null) {
            username = principal.getName();
        }
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(orderService.findOrdersForUser(username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id, Principal principal, Authentication authentication) {
        String username = null;
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            username = authentication.getName();
        } else if (principal != null) {
            username = principal.getName();
        }
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return orderService.findByIdForUser(id, username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
