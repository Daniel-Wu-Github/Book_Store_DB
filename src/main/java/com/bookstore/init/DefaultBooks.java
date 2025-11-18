package com.bookstore.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DefaultBooks implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final Environment env;
    private final BookRepository bookRepository;

    public DefaultBooks(JdbcTemplate jdbc, Environment env, BookRepository bookRepository) {
        this.jdbc = jdbc;
        this.env = env;
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Allow disabling seeds via config: app.seed.enabled=false
        if (!Boolean.parseBoolean(env.getProperty("app.seed.enabled", "true"))) {
            System.out.println("DefaultBooks: seeding disabled by property app.seed.enabled=false");
            return;
        }
        String url = env.getProperty("spring.datasource.url", "");
        boolean isH2 = url.startsWith("jdbc:h2:");

        if (isH2) {
            // Let Hibernate create schema; use repository-based seeding to avoid DB-specific DDL
            try {
                Long existing = bookRepository.count();
                if (existing != null && existing > 0) {
                    System.out.println("DefaultBooks: books table already has data (count=" + existing + ")");
                    return;
                }
            } catch (Exception ex) {
                // Table may not yet exist or schema generation ordering caused an issue — we'll attempt to seed anyway
                System.out.println("DefaultBooks: could not determine books count (will attempt to seed): " + ex.getMessage());
            }
            System.out.println("DefaultBooks: seeding default books via JPA repository...");
            List<Book> rows = List.of(
                    createBook("Effective Java", "Joshua Bloch", "978-0134685991", new BigDecimal("45.00"), 10, "Best practices for Java"),
                    createBook("Clean Code", "Robert C. Martin", "978-0132350884", new BigDecimal("40.00"), 7, "A Handbook of Agile Software Craftsmanship"),
                    createBook("Design Patterns", "Erich Gamma et al.", "978-0201633610", new BigDecimal("55.00"), 5, "Classic design patterns")
            );
            bookRepository.saveAll(rows);
            System.out.println("DefaultBooks: inserted/updated " + rows.size() + " books.");
            return;
        }

    // Fallback: run MySQL-compatible DDL and seed ONLY if table empty (do not overwrite)
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

        // Ensure orders and order_items tables exist for MySQL path so Hibernate/JPA can persist orders
        jdbc.execute("CREATE TABLE IF NOT EXISTS orders ("
            + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
            + "user_id BIGINT NOT NULL,"
            + "total_amount DECIMAL(19,2) NOT NULL DEFAULT 0.00,"
            + "order_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',"
            + "payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',"
            + "created_at DATETIME NOT NULL,"
            + "updated_at DATETIME NULL,"
            + "version BIGINT,"
            + "INDEX idx_orders_user (user_id),"
            + "CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

        jdbc.execute("CREATE TABLE IF NOT EXISTS order_items ("
            + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
            + "order_id BIGINT NOT NULL,"
            + "book_id BIGINT NOT NULL,"
            + "quantity INT NOT NULL DEFAULT 1,"
            + "unit_price DECIMAL(19,2) NOT NULL DEFAULT 0.00,"
            + "subtotal DECIMAL(19,2) NOT NULL DEFAULT 0.00,"
            + "item_type VARCHAR(50) NOT NULL DEFAULT 'BUY',"
            + "rental_days INT NULL,"
            + "created_at DATETIME NOT NULL,"
            + "updated_at DATETIME NULL,"
            + "version BIGINT,"
            + "INDEX idx_order_items_order (order_id),"
            + "INDEX idx_order_items_book (book_id),"
            + "CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE ON UPDATE CASCADE,"
            + "CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT ON UPDATE CASCADE"
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

    // Insert only if not existing (do not modify existing rows)
    String insertSql = "INSERT IGNORE INTO books (title, author, isbn, price, stock, description, created_at, updated_at, version) "
        + "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)";

    for (Object[] r : rows) { jdbc.update(insertSql, r); }

        System.out.println("DefaultBooks: inserted/updated " + rows.size() + " books.");
    }

    private Book createBook(String title, String author, String isbn, BigDecimal price, int stock, String desc) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setPrice(price);
        b.setStock(stock);
        b.setDescription(desc);
        return b;
    }
}
