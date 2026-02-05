package com.bookstore.service;

import com.bookstore.model.LoginToken;
import com.bookstore.model.User;
import com.bookstore.repository.LoginTokenRepository;
import com.bookstore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class MagicLinkService {
    private static final Logger log = LoggerFactory.getLogger(MagicLinkService.class);

    private final LoginTokenRepository tokenRepo;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${mail.from.address:no-reply@localhost}")
    private String fromAddress;

    @Value("${app.frontend.base-url:}")
    private String frontendBase;

    public MagicLinkService(LoginTokenRepository tokenRepo, UserRepository userRepository, JavaMailSender mailSender) {
        this.tokenRepo = tokenRepo;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public Optional<String> createAndSendToken(String email, HttpServletRequest req) {
        log.debug("createAndSendToken called for email={}", email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("No user found for magic link email={}", email);
            return Optional.empty();
        }
        log.info("Found user for email={}", email);

        String token = UUID.randomUUID().toString();
        LoginToken lt = new LoginToken();
        lt.setToken(token);
        lt.setEmail(email);
        lt.setCreatedAt(Instant.now());
        lt.setExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES));
        lt.setUsed(false);
        tokenRepo.save(lt);

        // Prefer a frontend-hosted consume page when configured so the client can
        // call the backend via XHR and establish the session in-place.
        String link;
        if (frontendBase != null && !frontendBase.isBlank()) {
            String frontendConsume = frontendBase;
            if (!frontendConsume.endsWith("/")) frontendConsume += "";
            frontendConsume += "/magic?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            link = frontendConsume;
        } else {
            // Fallback: consume via backend endpoint (keeps previous behavior)
            String base = req.getRequestURL().toString().replace(req.getRequestURI(), "") + req.getContextPath();
            String consumePath = "/api/auth/magic/consume?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            String redirectTo = (frontendBase != null && !frontendBase.isBlank()) ? frontendBase : "";
            if (!redirectTo.isBlank()) consumePath += "&redirect=" + URLEncoder.encode(redirectTo, StandardCharsets.UTF_8);
            link = base + consumePath;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(email);
            msg.setSubject("Your login link for The Cozy Bookshop");
            msg.setText("Click the link below to sign in. This link expires in 15 minutes.\n\n" + link + "\n\nIf you did not request this, ignore this email.");
            mailSender.send(msg);
            log.info("Magic link sent to {}", email);
            return Optional.of(link);
        } catch (Exception ex) {
            log.error("Failed to send magic link to {}: {}", email, ex.getMessage(), ex);
            return Optional.ofNullable(null);
        }
    }

    @Transactional
    public Optional<User> consumeToken(String token) {
        Optional<LoginToken> ltOpt = tokenRepo.findByToken(token);
        if (ltOpt.isEmpty()) return Optional.empty();
        LoginToken lt = ltOpt.get();
        if (lt.isUsed()) return Optional.empty();
        if (lt.getExpiresAt().isBefore(Instant.now())) return Optional.empty();

        Optional<User> userOpt = userRepository.findByEmail(lt.getEmail());
        if (userOpt.isEmpty()) return Optional.empty();

        lt.setUsed(true);
        tokenRepo.save(lt);
        return userOpt;
    }
}
