package com.bookstore.event;

import com.bookstore.model.Order;

public class OrderPlacedEvent {
    private final Long orderId;

    public OrderPlacedEvent(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
