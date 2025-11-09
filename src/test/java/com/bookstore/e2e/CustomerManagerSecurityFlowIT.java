package com.bookstore.e2e;

import com.bookstore.dto.CreateBookRequest;
import com.bookstore.dto.CreateUserRequest;
import com.bookstore.dto.order.CreateOrderRequest;
import com.bookstore.dto.order.OrderItemRequest;
import com.bookstore.service.DevEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CustomerManagerSecurityFlowIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    DevEmailService devEmailService;

    String baseUrl;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    private ResponseEntity<String> login(String username, String password) {
        Map<String,String> body = Map.of("username", username, "password", password);
        return restTemplate.postForEntity(baseUrl + "/auth/login", body, String.class);
    }

    private HttpHeaders withSession() { // TestRestTemplate retains cookies automatically
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    void fullFlows() {
        // 1. Register new customer
        CreateUserRequest reg = new CreateUserRequest();
        reg.setUsername("cust" + System.currentTimeMillis());
        reg.setPassword("pw12345");
        reg.setEmail("cust" + System.currentTimeMillis() + "@example.com");
        reg.setRoles("ROLE_USER");
        reg.setEnabled(true);
        ResponseEntity<String> regResp = restTemplate.postForEntity(baseUrl + "/auth/register", reg, String.class);
        assertEquals(HttpStatus.CREATED, regResp.getStatusCode());

        // 2. Login as customer
        String custUser = reg.getUsername();
        ResponseEntity<String> loginResp = login(custUser, reg.getPassword());
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());

        // 3. Search books (should return page JSON) - use q=code expecting seeded books like Clean Code
        ResponseEntity<String> searchResp = restTemplate.getForEntity(baseUrl + "/books/search?q=Code", String.class);
        assertEquals(HttpStatus.OK, searchResp.getStatusCode());
        assertTrue(searchResp.getBody().contains("Clean Code"));

        // 4. Place order with BUY and RENT items
        CreateOrderRequest orderReq = new CreateOrderRequest();
        OrderItemRequest buyItem = new OrderItemRequest();
        buyItem.setBookId(1L);
        buyItem.setQuantity(1);
        buyItem.setItemType("BUY");
        OrderItemRequest rentItem = new OrderItemRequest();
        rentItem.setBookId(2L);
        rentItem.setQuantity(1);
        rentItem.setItemType("RENT");
        rentItem.setRentalDays(7);
        orderReq.setItems(List.of(buyItem, rentItem));
        ResponseEntity<String> orderResp = restTemplate.postForEntity(baseUrl + "/orders", orderReq, String.class);
        assertEquals(HttpStatus.CREATED, orderResp.getStatusCode());
        assertTrue(orderResp.getBody().contains("PENDING"));

        // 5. Email confirmation captured
        assertFalse(devEmailService.getSent().isEmpty(), "Expected at least one dev email sent");
        String lastEmail = devEmailService.getSent().get(devEmailService.getSent().size() - 1);
        assertTrue(lastEmail.contains("Order #"), "Email should contain order id");

        // Manager flow
        ResponseEntity<String> adminLogin = login("admin", "admin");
        assertEquals(HttpStatus.OK, adminLogin.getStatusCode());
        ResponseEntity<String> allOrders = restTemplate.getForEntity(baseUrl + "/admin/orders", String.class);
        assertEquals(HttpStatus.OK, allOrders.getStatusCode());
        assertTrue(allOrders.getBody().contains("paymentStatus"));

        // Extract order id simple (assumes id":<number>)
        String body = allOrders.getBody();
        int idx = body.indexOf("\"id\":");
        assertTrue(idx > -1);
        int start = idx + 6;
        int end = start;
        while (end < body.length() && Character.isDigit(body.charAt(end))) end++;
        Long orderId = Long.parseLong(body.substring(start, end));

        // Update payment status
        ResponseEntity<String> payResp = restTemplate.exchange(baseUrl + "/admin/orders/" + orderId + "/payment?status=PAID", HttpMethod.PATCH, new HttpEntity<>(withSession()), String.class);
        assertEquals(HttpStatus.OK, payResp.getStatusCode());
        assertTrue(payResp.getBody().contains("PAID"));

        // Add new book as admin
        CreateBookRequest createBook = new CreateBookRequest();
        createBook.setTitle("New Admin Book");
        createBook.setAuthor("Mgr");
        createBook.setIsbn("ISBN-" + System.currentTimeMillis());
        createBook.setPrice(new BigDecimal("12.34"));
        createBook.setStock(5);
        createBook.setDescription("Added via test");
        ResponseEntity<String> addBookResp = restTemplate.postForEntity(baseUrl + "/books", createBook, String.class);
        assertEquals(HttpStatus.CREATED, addBookResp.getStatusCode());
        assertTrue(addBookResp.getBody().contains("New Admin Book"));

        // Security: customer tries admin endpoint again (need re-login as customer resets session)
        loginResp = login(custUser, reg.getPassword());
        ResponseEntity<String> forbiddenResp = restTemplate.getForEntity(baseUrl + "/admin/orders", String.class);
        // In test profile with non-dev security, should be 403
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResp.getStatusCode());

        // Unauthenticated access to protected endpoint -> 401 (logout by clearing cookies)
        // TestRestTemplate doesn't expose direct cookie clear; we create a new instance
        TestRestTemplate anon = new TestRestTemplate();
        ResponseEntity<String> unauthResp = anon.getForEntity(baseUrl + "/books/search?q=Java", String.class);
        // Expect 401 because search requires authentication by global security (non-dev profile)
        assertEquals(HttpStatus.UNAUTHORIZED, unauthResp.getStatusCode());
    }
}