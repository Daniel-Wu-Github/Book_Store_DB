package com.bookstore.service;

import com.bookstore.dto.CreateBookRequest;
import com.bookstore.dto.UpdateBookRequest;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<Book> list(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public Page<Book> search(String q, String title, String author, Pageable pageable) {
        // naive in-memory filtering as a minimal implementation; for production, add repository methods
        List<Book> all = bookRepository.findAll();
        String qn = StringUtils.hasText(q) ? q.toLowerCase() : null;
        String tn = StringUtils.hasText(title) ? title.toLowerCase() : null;
        String an = StringUtils.hasText(author) ? author.toLowerCase() : null;
        List<Book> filtered = all.stream().filter(b -> {
            boolean ok = true;
            if (qn != null) {
                ok &= (b.getTitle() != null && b.getTitle().toLowerCase().contains(qn)) ||
                      (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(qn));
            }
            if (tn != null) {
                ok &= (b.getTitle() != null && b.getTitle().toLowerCase().contains(tn));
            }
            if (an != null) {
                ok &= (b.getAuthor() != null && b.getAuthor().toLowerCase().contains(an));
            }
            return ok;
        }).collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<Book> pageContent = start >= filtered.size() ? List.of() : filtered.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filtered.size());
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
