package com.bookstore.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = true) // nullable so book can be deleted
    private Book book;

    // Denormalized book info - preserved even if book is deleted
    @Column(name = "book_title", length = 255)
    private String bookTitle;

    @Column(name = "book_author", length = 255)
    private String bookAuthor;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType = ItemType.BUY;

    @Column(name = "rental_days")
    private Integer rentalDays;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    public OrderItem() {
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
        // Copy denormalized info at order creation time
        if (book != null) {
            this.bookTitle = book.getTitle();
            this.bookAuthor = book.getAuthor();
        }
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        recalcSubtotal();
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        recalcSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public Integer getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(Integer rentalDays) {
        this.rentalDays = rentalDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        recalcSubtotal();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        recalcSubtotal();
    }

    private void recalcSubtotal() {
        if (this.unitPrice == null) this.unitPrice = BigDecimal.ZERO;
        if (this.quantity == null) this.quantity = 1;
        // For RENT items, subtotal = unitPrice * quantity * rentalDays (if rentalDays present)
        if (this.itemType == ItemType.RENT && this.rentalDays != null && this.rentalDays > 0) {
            this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity)).multiply(BigDecimal.valueOf(this.rentalDays));
        } else {
            this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }
}
