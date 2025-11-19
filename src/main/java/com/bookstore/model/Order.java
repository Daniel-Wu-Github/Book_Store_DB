package com.bookstore.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    @Column(name = "emailed", nullable = false)
    private boolean emailed = false;

    // Constructors, getters, setters

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        recalcTotal();
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
        recalcTotal();
    }

    public void removeItem(OrderItem item) {
        this.items.remove(item);
        item.setOrder(null);
        recalcTotal();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEmailed() {
        return emailed;
    }

    public void setEmailed(boolean emailed) {
        this.emailed = emailed;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        recalcTotal();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        recalcTotal();
    }

    private void recalcTotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (this.items != null) {
            for (OrderItem it : this.items) {
                if (it.getSubtotal() != null) {
                    total = total.add(it.getSubtotal());
                }
            }
        }
        this.totalAmount = total;
    }
}
