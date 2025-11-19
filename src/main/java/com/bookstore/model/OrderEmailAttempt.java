package com.bookstore.model;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "order_emails")
public class OrderEmailAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
