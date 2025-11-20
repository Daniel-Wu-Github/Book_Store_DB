package com.bookstore.service;

import com.bookstore.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "mail.smtp.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from.address:no-reply@localhost}")
    private String fromAddress;

    @Value("${mail.override.to:}")
    private String overrideTo;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOrderConfirmation(Order order) {
        if (order == null || order.getUser() == null || order.getUser().getEmail() == null) {
            log.warn("Skipping email send: order/user/email missing (orderId={})", order == null ? null : order.getId());
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        String recipient = (overrideTo != null && !overrideTo.isBlank()) ? overrideTo : order.getUser().getEmail();
        msg.setTo(recipient);
        msg.setSubject("Order Confirmation #" + order.getId());
        msg.setText(buildBody(order, recipient));
        try {
            mailSender.send(msg);
            log.info("SMTP email sent for orderId={} to {}", order.getId(), recipient);
        } catch (org.springframework.mail.MailException me) {
            log.error("SMTP send failed for orderId={} to {}: {}", order.getId(), recipient, me.getMessage(), me);
            throw me;
        }
    }

    private String buildBody(Order order, String recipient) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thank you for your order!\n\n");
        sb.append("Order #").append(order.getId()).append("\n");
        sb.append("Total: ").append(order.getTotalAmount()).append("\n");
        sb.append("Items:\n");
        order.getItems().forEach(i -> sb.append(" - ")
                .append(i.getBook().getTitle())
                .append(" x").append(i.getQuantity())
                .append(" = ").append(i.getSubtotal())
                .append("\n"));
        if (overrideTo != null && !overrideTo.isBlank()) {
            sb.append("\n(Note: overridden recipient ").append(recipient).append(")\n");
        }
        return sb.toString();
    }
}
