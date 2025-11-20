package com.bookstore.dto;

import com.bookstore.model.Book;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;


public class BookDto {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private BigDecimal price;
    private BigDecimal rentPrice;
    private Integer stock;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public BookDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getRentPrice() { return rentPrice; }
    public void setRentPrice(BigDecimal rentPrice) { this.rentPrice = rentPrice; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static BookDto fromEntity(Book b) {
        if (b == null) return null;
        BookDto d = new BookDto();
        d.setId(b.getId());
        d.setTitle(b.getTitle());
        d.setAuthor(b.getAuthor());
        d.setIsbn(b.getIsbn());
        d.setPrice(b.getPrice());
        // Compute a default rent price (20% of buy price) if price is present
        try {
            if (b.getPrice() != null) {
                d.setRentPrice(b.getPrice().multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP));
            }
        } catch (Exception ignore) { }
        d.setStock(b.getStock());
        d.setDescription(b.getDescription());
        d.setCreatedAt(b.getCreatedAt());
        d.setUpdatedAt(b.getUpdatedAt());
        return d;
    }

    public Book toEntity() {
        Book b = new Book();
        b.setId(this.id);
        b.setTitle(this.title);
        b.setAuthor(this.author);
        b.setIsbn(this.isbn);
        b.setPrice(this.price);
        b.setStock(this.stock);
        b.setDescription(this.description);
        return b;
    }
}
