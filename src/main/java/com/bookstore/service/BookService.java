package com.bookstore.service;

import com.bookstore.dto.CreateBookRequest;
import com.bookstore.dto.UpdateBookRequest;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<Book> list(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public Optional<Book> getById(Long id) {
        return bookRepository.findById(id);
    }

    @Transactional
    public Book create(CreateBookRequest req) {
        Book b = new Book();
        b.setTitle(req.getTitle());
        b.setAuthor(req.getAuthor());
        b.setIsbn(req.getIsbn());
        b.setPrice(req.getPrice());
        b.setStock(req.getStock());
        b.setDescription(req.getDescription());
        return bookRepository.save(b);
    }

    @Transactional
    public Optional<Book> update(Long id, UpdateBookRequest req) {
        return bookRepository.findById(id).map(b -> {
            b.setTitle(req.getTitle());
            b.setAuthor(req.getAuthor());
            b.setIsbn(req.getIsbn());
            if (req.getPrice() != null) b.setPrice(req.getPrice());
            if (req.getStock() != null) b.setStock(req.getStock());
            b.setDescription(req.getDescription());
            return bookRepository.save(b);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (!bookRepository.existsById(id)) return false;
        bookRepository.deleteById(id);
        return true;
    }
}
