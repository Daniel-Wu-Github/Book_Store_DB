package com.bookstore.dto.order;

import com.bookstore.model.OrderStatus;
import com.bookstore.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OrderDto {
    private Long id;
    private BigDecimal totalAmount;
    private String username;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
        private Instant createdAt;
        private Boolean emailed;
    private List<OrderItemDto> items = new ArrayList<>();

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public OrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(OrderStatus orderStatus) { this.orderStatus = orderStatus; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Boolean getEmailed() { return emailed; }
    public void setEmailed(Boolean emailed) { this.emailed = emailed; }

    public List<OrderItemDto> getItems() { return items; }
    public void setItems(List<OrderItemDto> items) { this.items = items; }
}
