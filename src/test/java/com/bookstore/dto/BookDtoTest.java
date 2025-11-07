package com.bookstore.dto;

import com.bookstore.model.Book;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class BookDtoTest {

    @Test
    void fromEntity_and_toEntity_roundtrip() {
        Book b = new Book();
        b.setId(42L);
        b.setTitle("Test Title");
        b.setAuthor("Author Name");
        b.setIsbn("ISBN-12345");
        b.setPrice(new BigDecimal("19.99"));
        b.setStock(10);
        b.setDescription("A test book");
        // set timestamps via prePersist
        b.prePersist();
        Instant created = b.getCreatedAt();

        BookDto dto = BookDto.fromEntity(b);
        assertNotNull(dto);
        assertEquals(b.getId(), dto.getId());
        assertEquals(b.getTitle(), dto.getTitle());
        assertEquals(b.getAuthor(), dto.getAuthor());
        assertEquals(b.getIsbn(), dto.getIsbn());
        assertEquals(b.getPrice(), dto.getPrice());
        assertEquals(b.getStock(), dto.getStock());
        assertEquals(b.getDescription(), dto.getDescription());
        assertEquals(created, dto.getCreatedAt());

        Book back = dto.toEntity();
        assertNotNull(back);
        assertEquals(dto.getTitle(), back.getTitle());
        assertEquals(dto.getAuthor(), back.getAuthor());
        assertEquals(dto.getIsbn(), back.getIsbn());
        assertEquals(dto.getPrice(), back.getPrice());
        assertEquals(dto.getStock(), back.getStock());
        assertEquals(dto.getDescription(), back.getDescription());
    }
}
