package com.bookstore.service;

import com.bookstore.dto.order.CreateOrderRequest;
import com.bookstore.dto.order.OrderDto;
import com.bookstore.dto.order.OrderItemDto;
import com.bookstore.model.*;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderService(OrderRepository orderRepository, BookRepository bookRepository, UserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderDto placeOrder(String username, CreateOrderRequest req) {
        com.bookstore.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + username));

        Order order = new Order();
        order.setUser(user);

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }

        for (var itReq : req.getItems()) {
            Long bookId = itReq.getBookId();
            int qty = itReq.getQuantity() == null ? 1 : itReq.getQuantity();

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("book not found: " + bookId));

            if (book.getStock() == null || book.getStock() < qty) {
                throw new IllegalArgumentException("insufficient stock for book id: " + bookId);
            }

            // decrement stock
            book.setStock(book.getStock() - qty);
            bookRepository.save(book);

            OrderItem item = new OrderItem();
            item.setBook(book);
            item.setQuantity(qty);
            // If this is a RENT item, unit price should be the rent price (e.g. 20% of buy price)
            try {
                if (itReq.getItemType() != null && itReq.getItemType().equalsIgnoreCase("RENT")) {
                    java.math.BigDecimal rentUnit = book.getPrice().multiply(new java.math.BigDecimal("0.20")).setScale(2, java.math.RoundingMode.HALF_UP);
                    item.setUnitPrice(rentUnit);
                } else {
                    item.setUnitPrice(book.getPrice());
                }
            } catch (Exception ex) {
                item.setUnitPrice(book.getPrice());
            }
            try {
                item.setItemType(ItemType.valueOf(itReq.getItemType() == null ? "BUY" : itReq.getItemType()));
            } catch (Exception e) {
                item.setItemType(ItemType.BUY);
            }
            if (itReq.getRentalDays() != null) item.setRentalDays(itReq.getRentalDays());

            order.addItem(item);
        }

        Order saved = orderRepository.save(order);

        // publish event to send confirmation after transaction commits
        eventPublisher.publishEvent(new com.bookstore.event.OrderPlacedEvent(saved.getId()));

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public java.util.List<OrderDto> findOrdersForUser(String username) {
        com.bookstore.model.User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("user not found: " + username));
        return orderRepository.findByUser(user).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<OrderDto> findByIdForUser(Long id, String username) {
        return orderRepository.findById(id).map(o -> {
            if (!o.getUser().getUsername().equals(username)) return null;
            return toDto(o);
        });
    }

    public OrderDto toDto(Order o) {
        OrderDto dto = new OrderDto();
        dto.setId(o.getId());
        dto.setUsername(o.getUser() != null ? o.getUser().getUsername() : null);
        dto.setTotalAmount(o.getTotalAmount());
        dto.setOrderStatus(o.getOrderStatus());
        dto.setPaymentStatus(o.getPaymentStatus());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setEmailed(o.isEmailed());
        var items = o.getItems();
        var dtos = items.stream().map(it -> {
            OrderItemDto idto = new OrderItemDto();
            idto.setId(it.getId());
            idto.setBookId(it.getBook().getId());
            idto.setTitle(it.getBook().getTitle());
            idto.setQuantity(it.getQuantity());
            idto.setUnitPrice(it.getUnitPrice());
            idto.setSubtotal(it.getSubtotal());
            idto.setRentalDays(it.getRentalDays());
            idto.setItemType(it.getItemType().name());
            return idto;
        }).collect(Collectors.toList());
        dto.setItems(dtos);
        return dto;
    }
}
