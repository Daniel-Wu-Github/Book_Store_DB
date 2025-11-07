package com.bookstore.service;

import com.bookstore.dto.CreateBookRequest;
import com.bookstore.dto.UpdateBookRequest;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BookServiceTest {

    private BookRepository repo;
    private BookService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(BookRepository.class);
        service = new BookService(repo);
    }

    @Test
    void list_delegatesToRepository() {
        when(repo.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<Book>(List.of()));
        var page = service.list(PageRequest.of(0, 10));
        assertNotNull(page);
        verify(repo).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void create_savesEntity() {
        CreateBookRequest req = new CreateBookRequest();
        req.setTitle("T");
        req.setAuthor("A");
        req.setPrice(new BigDecimal("9.99"));
        req.setStock(5);

        Book saved = new Book();
        saved.setId(1L);
        saved.setTitle(req.getTitle());
        when(repo.save(any())).thenReturn(saved);

        Book result = service.create(req);
        assertNotNull(result);
        assertEquals(1L, result.getId());

        ArgumentCaptor<Book> cap = ArgumentCaptor.forClass(Book.class);
        verify(repo).save(cap.capture());
        assertEquals(req.getTitle(), cap.getValue().getTitle());
    }

    @Test
    void update_existing_updates() {
        Book existing = new Book();
        existing.setId(2L);
        existing.setTitle("Old");
        existing.setAuthor("Author");
        existing.setPrice(new BigDecimal("5.00"));
        existing.setStock(1);

        when(repo.findById(2L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateBookRequest req = new UpdateBookRequest();
        req.setTitle("New");
        req.setAuthor("NewAuthor");
        req.setPrice(new BigDecimal("6.00"));
        req.setStock(3);

        Optional<Book> out = service.update(2L, req);
        assertTrue(out.isPresent());
        assertEquals("New", out.get().getTitle());
        verify(repo).findById(2L);
    }

    @Test
    void delete_nonexistent_returnsFalse() {
        when(repo.existsById(99L)).thenReturn(false);
        boolean deleted = service.delete(99L);
        assertFalse(deleted);
        verify(repo, never()).deleteById(99L);
    }

}
