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
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final com.bookstore.service.EmailService emailService;
    private final com.bookstore.repository.OrderEmailAttemptRepository attemptRepository;

    @Autowired
    public AdminOrderController(OrderRepository orderRepository, OrderService orderService, com.bookstore.service.EmailService emailService, com.bookstore.repository.OrderEmailAttemptRepository attemptRepository) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.emailService = emailService;
        this.attemptRepository = attemptRepository;
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> listAll() {
    List<OrderDto> list = orderRepository.findAll().stream().map(orderService::toDto).collect(Collectors.toList());
    return ResponseEntity.ok(list);
    }

    @RequestMapping(path = "/{id}/payment", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<?> updatePayment(@PathVariable Long id, @RequestParam PaymentStatus status) {
        return orderRepository.findById(id).map(o -> {
            o.setPaymentStatus(status);
            if (status == PaymentStatus.PAID && o.getOrderStatus() == OrderStatus.PENDING) {
                o.setOrderStatus(OrderStatus.CONFIRMED);
                try {
                    emailService.sendOrderConfirmation(o);
                    o.setEmailed(true);
                } catch (Exception e) {
                    // keep emailed=false on failure
                }
            }
            Order saved = orderRepository.save(o);
            return ResponseEntity.ok(orderService.toDto(saved));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @RequestMapping(path = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return orderRepository.findById(id).map(o -> {
            o.setOrderStatus(status);
            Order saved = orderRepository.save(o);
            return ResponseEntity.ok(orderService.toDto(saved));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/resend-email")
    public ResponseEntity<?> resendEmail(@PathVariable Long id) {
        return orderRepository.findById(id).map(o -> {
            boolean success = false;
            String error = null;
            try {
                emailService.sendOrderConfirmation(o);
                o.setEmailed(true);
                Order saved = orderRepository.save(o);
                success = true;
                try {
                    com.bookstore.model.OrderEmailAttempt a = new com.bookstore.model.OrderEmailAttempt();
                    a.setOrderId(id);
                    a.setSuccess(true);
                    a.setProvider("manual-resend");
                    a.setSentAt(java.time.Instant.now());
                    attemptRepository.save(a);
                } catch (Exception ignore) { }
                return ResponseEntity.ok(orderService.toDto(saved));
            } catch (Exception e) {
                error = e.getMessage();
                try {
                    com.bookstore.model.OrderEmailAttempt a = new com.bookstore.model.OrderEmailAttempt();
                    a.setOrderId(id);
                    a.setSuccess(false);
                    a.setProvider("manual-resend");
                    a.setErrorMessage(error);
                    a.setSentAt(java.time.Instant.now());
                    attemptRepository.save(a);
                } catch (Exception ignore) { }
                return ResponseEntity.status(500).body("Failed to resend email: " + error);
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
