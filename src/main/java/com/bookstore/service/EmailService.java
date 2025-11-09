package com.bookstore.service;

import com.bookstore.model.Order;

public interface EmailService {
    void sendOrderConfirmation(Order order);
}
