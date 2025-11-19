package com.bookstore.event;

import com.bookstore.model.Order;
import com.bookstore.repository.OrderRepository;
import com.bookstore.service.EmailService;
import com.bookstore.repository.OrderEmailAttemptRepository;
import com.bookstore.model.OrderEmailAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderPlacedListener {
    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final OrderEmailAttemptRepository attemptRepository;

    @Autowired
    public OrderPlacedListener(OrderRepository orderRepository, EmailService emailService, OrderEmailAttemptRepository attemptRepository) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.attemptRepository = attemptRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent evt) {
        Long id = evt.getOrderId();
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            log.warn("OrderPlacedListener: order not found id={}", id);
            return;
        }
        try {
            emailService.sendOrderConfirmation(order);
            order.setEmailed(true);
            orderRepository.save(order);
            recordAttempt(id, true, null);
        } catch (Exception e) {
            log.error("failed to send order confirmation for orderId={}", id, e);
            recordAttempt(id, false, e.getMessage());
        }
    }

    private void recordAttempt(Long orderId, boolean success, String error) {
        try {
            OrderEmailAttempt a = new OrderEmailAttempt();
            a.setOrderId(orderId);
            a.setSuccess(success);
            a.setErrorMessage(error);
            a.setProvider("smtp");
            a.setSentAt(java.time.Instant.now());
            attemptRepository.save(a);
        } catch (Exception ignore) {
            // don't block on audit failures
        }
    }
}
