package com.bookstore.service;

import com.bookstore.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("dev")
public class DevEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(DevEmailService.class);

    // Keep an in-memory list for debugging in dev
    private final List<String> sent = new ArrayList<>();

    @Override
    public void sendOrderConfirmation(Order order) {
        String body = buildBody(order);
        // In dev we just log and keep the message in memory
        log.info("[DEV EMAIL] order confirmation for orderId={}\n{}", order.getId(), body);
        sent.add(body);
    }

    private String buildBody(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(order.getId()).append("\n");
        sb.append("Total: ").append(order.getTotalAmount()).append("\n");
        sb.append("Items:\n");
        order.getItems().forEach(i -> sb.append(" - ").append(i.getBook().getTitle()).append(" x").append(i.getQuantity()).append(" = ").append(i.getSubtotal()).append("\n"));
        return sb.toString();
    }

    public List<String> getSent() {
        return sent;
    }
}
