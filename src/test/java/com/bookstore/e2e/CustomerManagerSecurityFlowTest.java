package com.bookstore.e2e;

import com.bookstore.dto.CreateBookRequest;
import com.bookstore.dto.CreateUserRequest;
import com.bookstore.dto.order.CreateOrderRequest;
import com.bookstore.dto.order.OrderItemRequest;
import com.bookstore.service.DevEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.bookstore.model.Book;
import com.bookstore.model.User;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CustomerManagerSecurityFlowTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    DevEmailService devEmailService;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    String baseUrl;
    String sessionCookie;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port + "/api";
        // Test schema is initialized deterministically via src/test/resources/schema.sql

    // Clean existing rows and seed admin+books
    bookRepository.deleteAll();
    userRepository.deleteAll();

    User admin = new User();
    admin.setUsername("admin");
    admin.setEmail("admin@example.com");
    admin.setPassword(passwordEncoder.encode("admin"));
    admin.setRoles("ROLE_ADMIN");
    admin.setEnabled(true);
    userRepository.save(admin);

    Book bb1 = new Book();
    bb1.setTitle("Clean Code");
    bb1.setAuthor("Robert C. Martin");
    bb1.setIsbn("978-0132350884");
    bb1.setPrice(new BigDecimal("40.00"));
    bb1.setStock(7);
    bb1.setDescription("A Handbook of Agile Software Craftsmanship");

    Book bb2 = new Book();
    bb2.setTitle("Effective Java");
    bb2.setAuthor("Joshua Bloch");
    bb2.setIsbn("978-0134685991");
    bb2.setPrice(new BigDecimal("45.00"));
    bb2.setStock(10);
    bb2.setDescription("Best practices for Java");

    bookRepository.save(bb1);
    bookRepository.save(bb2);
    }

    private ResponseEntity<String> login(String username, String password) {
        Map<String,String> body = Map.of("username", username, "password", password);
        ResponseEntity<String> resp = restTemplate.postForEntity(baseUrl + "/auth/login", body, String.class);
        List<String> set = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (set != null && !set.isEmpty()) {
            // store only the cookie pair (name=value) without attributes
            String first = set.get(0);
            int sem = first.indexOf(';');
            sessionCookie = sem > 0 ? first.substring(0, sem) : first;
        }
        return resp;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sessionCookie != null) headers.add(HttpHeaders.COOKIE, sessionCookie);
        return headers;
    }

    @Test
    void fullFlows() {
        // Ensure seeded admin exists (test profile seeds an admin). Login with known seeded credentials.
        assertEquals(HttpStatus.OK, login("admin", "admin").getStatusCode());

        // Use the two books seeded in @BeforeEach
        List<Book> seeded = bookRepository.findAll();
        assertTrue(seeded.size() >= 2, "Expected at least two seeded books");
        long book1Id = seeded.get(0).getId();
        long book2Id = seeded.get(1).getId();

        // Customer flow
        CreateUserRequest reg = new CreateUserRequest();
        reg.setUsername("cust" + System.currentTimeMillis());
        reg.setPassword("pw12345");
        reg.setEmail("cust" + System.currentTimeMillis() + "@example.com");
        reg.setRoles("ROLE_USER");
        reg.setEnabled(true);
        ResponseEntity<String> regResp = restTemplate.postForEntity(baseUrl + "/auth/register", reg, String.class);
        assertEquals(HttpStatus.CREATED, regResp.getStatusCode());

        String custUser = reg.getUsername();
        assertEquals(HttpStatus.OK, login(custUser, reg.getPassword()).getStatusCode());

    ResponseEntity<String> searchResp = restTemplate.exchange(baseUrl + "/books/search?q=Code", HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
    assertEquals(HttpStatus.OK, searchResp.getStatusCode());
    assertTrue(searchResp.getBody().contains("Clean Code"));

        CreateOrderRequest orderReq = new CreateOrderRequest();
        OrderItemRequest buyItem = new OrderItemRequest();
    buyItem.setBookId(book1Id);
        buyItem.setQuantity(1);
        buyItem.setItemType("BUY");
        OrderItemRequest rentItem = new OrderItemRequest();
    rentItem.setBookId(book2Id);
        rentItem.setQuantity(1);
        rentItem.setItemType("RENT");
        rentItem.setRentalDays(7);
        orderReq.setItems(List.of(buyItem, rentItem));
    ResponseEntity<String> orderResp = restTemplate.postForEntity(baseUrl + "/orders", new HttpEntity<>(orderReq, authHeaders()), String.class);
    assertEquals(HttpStatus.CREATED, orderResp.getStatusCode());
    assertTrue(orderResp.getBody().contains("PENDING"));

        assertFalse(devEmailService.getSent().isEmpty(), "Expected at least one dev email sent");

        // Manager flow (already admin logged in earlier; ensure again)
        assertEquals(HttpStatus.OK, login("admin", "admin").getStatusCode());
    ResponseEntity<String> allOrders = restTemplate.exchange(baseUrl + "/admin/orders", HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
    assertEquals(HttpStatus.OK, allOrders.getStatusCode());
    assertTrue(allOrders.getBody().contains("paymentStatus"));

        // Extract order id
    String body = allOrders.getBody();
    Long orderId = extractFirstId(body);

        ResponseEntity<String> payResp = restTemplate.exchange(baseUrl + "/admin/orders/" + orderId + "/payment?status=PAID", HttpMethod.POST, new HttpEntity<>(null, authHeaders()), String.class);
        assertEquals(HttpStatus.OK, payResp.getStatusCode());
        assertTrue(payResp.getBody().contains("PAID"));

        // Security checks
        assertEquals(HttpStatus.OK, login(custUser, reg.getPassword()).getStatusCode());
    ResponseEntity<String> forbiddenResp = restTemplate.exchange(baseUrl + "/admin/orders", HttpMethod.GET, new HttpEntity<>(authHeaders()), String.class);
    assertEquals(HttpStatus.FORBIDDEN, forbiddenResp.getStatusCode());

        TestRestTemplate anon = new TestRestTemplate();
        ResponseEntity<String> unauthResp = anon.getForEntity(baseUrl + "/books/search?q=Java", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, unauthResp.getStatusCode());
    }

    private long extractFirstId(String json) {
        if (json == null) return -1;
        int idx = json.indexOf("\"id\":");
        if (idx < 0) return -1;
        int start = idx + 5; // position at ':'; adjust to digit
        while (start < json.length() && !Character.isDigit(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return Long.parseLong(json.substring(start, end));
    }
}