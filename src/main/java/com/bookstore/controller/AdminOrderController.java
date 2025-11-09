package com.bookstore.controller;

import com.bookstore.model.Order;
import com.bookstore.dto.order.OrderDto;
import com.bookstore.service.OrderService;
import com.bookstore.model.OrderStatus;
import com.bookstore.model.PaymentStatus;
import com.bookstore.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Autowired
    public AdminOrderController(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> listAll() {
    List<OrderDto> list = orderRepository.findAll().stream().map(orderService::toDto).collect(Collectors.toList());
    return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<?> updatePayment(@PathVariable Long id, @RequestParam PaymentStatus status) {
        return orderRepository.findById(id).map(o -> {
            o.setPaymentStatus(status);
            Order saved = orderRepository.save(o);
            return ResponseEntity.ok(orderService.toDto(saved));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return orderRepository.findById(id).map(o -> {
            o.setOrderStatus(status);
            Order saved = orderRepository.save(o);
            return ResponseEntity.ok(orderService.toDto(saved));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
