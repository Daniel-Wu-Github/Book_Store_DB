package com.bookstore.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Profile("dev")
public class DefaultBooks implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DefaultBooks(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create table if it doesn't exist (matches the JPA entity)
        jdbc.execute("CREATE TABLE IF NOT EXISTS books ("
                + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "title VARCHAR(255) NOT NULL,"
                + "author VARCHAR(255) NOT NULL,"
                + "isbn VARCHAR(50),"
                + "price DECIMAL(19,2) NOT NULL,"
                + "stock INT NOT NULL,"
                + "description TEXT,"
                + "created_at DATETIME NOT NULL,"
                + "updated_at DATETIME NULL,"
                + "version BIGINT,"
                + "UNIQUE KEY idx_books_isbn (isbn)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM books", Integer.class);
        if (count != null && count > 0) {
            System.out.println("DefaultBooks: books table already has data (count=" + count + ")");
            return;
        }

        System.out.println("DefaultBooks: seeding default books...");

        List<Object[]> rows = List.of(
                new Object[]{"Effective Java", "Joshua Bloch", "978-0134685991", new BigDecimal("45.00"), 10, "Best practices for Java"},
                new Object[]{"Clean Code", "Robert C. Martin", "978-0132350884", new BigDecimal("40.00"), 7, "A Handbook of Agile Software Craftsmanship"},
                new Object[]{"Design Patterns", "Erich Gamma et al.", "978-0201633610", new BigDecimal("55.00"), 5, "Classic design patterns"}
        );

        // Use ON DUPLICATE KEY UPDATE so seeding is idempotent (unique index on isbn)
        String insertSql = "INSERT INTO books (title, author, isbn, price, stock, description, created_at, updated_at, version) "
                + "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0) "
                + "ON DUPLICATE KEY UPDATE title=VALUES(title), author=VALUES(author), price=VALUES(price), stock=VALUES(stock), description=VALUES(description), updated_at=NOW()";

        for (Object[] r : rows) {
            jdbc.update(insertSql, r);
        }

        System.out.println("DefaultBooks: inserted/updated " + rows.size() + " books.");
    }
}
