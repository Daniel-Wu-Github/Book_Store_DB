package com.bookstore.repository;

import com.bookstore.model.OrderEmailAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEmailAttemptRepository extends JpaRepository<OrderEmailAttempt, Long> {
}
