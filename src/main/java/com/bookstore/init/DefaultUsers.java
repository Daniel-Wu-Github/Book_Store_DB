package com.bookstore.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DefaultUsers implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public DefaultUsers(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create users table if it doesn't exist
        jdbc.execute("CREATE TABLE IF NOT EXISTS users ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "username VARCHAR(100) NOT NULL,"
                + "password VARCHAR(255) NOT NULL,"
                + "email VARCHAR(200) NOT NULL,"
                + "roles VARCHAR(200),"
                + "enabled BOOLEAN NOT NULL DEFAULT TRUE,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NULL,"
                + "version BIGINT," 
                + "UNIQUE KEY ux_users_username (username),"
                + "UNIQUE KEY ux_users_email (email)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (count != null && count > 0) {
            System.out.println("DefaultUsers: users table already has data (count=" + count + ")");
            // Still ensure admin exists/updated: we'll upsert using ON DUPLICATE KEY UPDATE
        } else {
            System.out.println("DefaultUsers: users table created (if it did not exist)");
        }

        // Seed an admin user (idempotent)
        String rawPassword = "admin"; // dev password
        String encoded = passwordEncoder.encode(rawPassword);
        String insertSql = "INSERT INTO users (username, password, email, roles, enabled, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, ?, ?, NOW(), NOW(), 0) "
                + "ON DUPLICATE KEY UPDATE password=VALUES(password), email=VALUES(email), roles=VALUES(roles), enabled=VALUES(enabled), updated_at=NOW()";

        jdbc.update(insertSql, "admin", encoded, "admin@example.com", "ROLE_ADMIN,ROLE_USER", true);

        System.out.println("DefaultUsers: ensured admin user exists (username=admin)");
    }
}
