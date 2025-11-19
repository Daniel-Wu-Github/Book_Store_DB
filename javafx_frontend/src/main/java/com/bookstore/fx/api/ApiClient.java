package com.bookstore.fx.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class ApiClient {
    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private static volatile String sessionCookie; // shared JSESSIONID=...

    public ApiClient() {
        this(System.getenv().getOrDefault("BOOKSTORE_API_BASE", "http://localhost:8080/api"));
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean login(String username, String password) throws IOException, InterruptedException {
        Map<String, String> body = Map.of("username", username, "password", password);
    HttpRequest req = requestBuilder("/auth/login")
        .header("Content-Type", "application/json")
        .POST(jsonBody(body))
        .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        captureCookie(resp);
        if (resp.statusCode() != 200) {
            System.err.println("Login failed: status=" + resp.statusCode() + ", body=" + resp.body());
            return false;
        }
        return true;
    }

    public boolean register(String username, String email, String password) throws IOException, InterruptedException {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        body.put("roles", "ROLE_USER");
        body.put("enabled", true);
    HttpRequest req = requestBuilder("/auth/register")
        .header("Content-Type", "application/json")
        .POST(jsonBody(body))
        .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 201) {
            System.err.println("Register failed: status=" + resp.statusCode() + ", body=" + resp.body());
            return false;
        }
        return true;
    }

    public List<Map<String, Object>> searchBooks(String query) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/books/search?q=" + url(query))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return List.of();
        // Controller returns a Spring Page object; extract 'content' if present.
        Map<String, Object> page = mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>(){});
        Object content = page.get("content");
        if (content instanceof List) {
            //noinspection unchecked
            return (List<Map<String, Object>>) content;
        }
        return List.of();
    }

    public Map<String, Object> placeOrder(List<Map<String, Object>> items) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of("items", items);
        HttpRequest req = requestBuilder("/orders")
                .header("Content-Type", "application/json")
                .POST(jsonBody(body))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return mapper.readValue(resp.body(), new TypeReference<>(){});
        }
        throw new IOException("Order failed: status=" + resp.statusCode() + ", body=" + resp.body());
    }

    public List<Map<String, Object>> adminListOrders() throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/admin/orders").GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            return mapper.readValue(resp.body(), new TypeReference<>(){});
        }
        if (resp.statusCode() == 403 || resp.statusCode() == 401) return List.of();
        throw new IOException("List orders failed: status=" + resp.statusCode());
    }

    public boolean adminMarkPaymentPaid(long orderId) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/admin/orders/" + orderId + "/payment?status=PAID")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200;
    }

    public boolean adminResendOrderEmail(long orderId) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/admin/orders/" + orderId + "/resend-email")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200;
    }

    public Map<String, Object> me() throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/auth/me").GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            return mapper.readValue(resp.body(), new TypeReference<>(){});
        }
        return Map.of();
    }

    // --- Admin users ---
    public List<Map<String,Object>> adminListUsers() throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/admin/users").GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            return mapper.readValue(resp.body(), new TypeReference<>(){});
        }
        if (resp.statusCode() == 403 || resp.statusCode() == 401) return List.of();
        throw new IOException("List users failed: status=" + resp.statusCode());
    }

    public Map<String,Object> adminCreateUser(Map<String,Object> reqBody) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/admin/users")
                .header("Content-Type","application/json")
                .POST(jsonBody(reqBody))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 201) return mapper.readValue(resp.body(), new TypeReference<>(){});
        throw new IOException("Create user failed: status=" + resp.statusCode() + ", body=" + resp.body());
    }

    public Map<String,Object> adminUpdateUser(long id, Map<String,Object> reqBody) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/admin/users/"+id)
                .header("Content-Type","application/json")
                .PUT(jsonBody(reqBody))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) return mapper.readValue(resp.body(), new TypeReference<>(){});
        throw new IOException("Update user failed: status=" + resp.statusCode() + ", body=" + resp.body());
    }

    public boolean adminDeleteUser(long id) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/admin/users/"+id).DELETE().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 204;
    }

    // --- Admin books (create/update/delete) ---
    public Map<String,Object> createBook(Map<String,Object> reqBody) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/books")
                .header("Content-Type","application/json")
                .POST(jsonBody(reqBody))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 201) return mapper.readValue(resp.body(), new TypeReference<>(){});
        throw new IOException("Create book failed: status=" + resp.statusCode() + ", body=" + resp.body());
    }

    public Map<String,Object> updateBook(long id, Map<String,Object> reqBody) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/books/"+id)
                .header("Content-Type","application/json")
                .PUT(jsonBody(reqBody))
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) return mapper.readValue(resp.body(), new TypeReference<>(){});
        throw new IOException("Update book failed: status=" + resp.statusCode() + ", body=" + resp.body());
    }

    public boolean deleteBook(long id) throws IOException, InterruptedException {
        HttpRequest req = requestBuilder("/books/"+id).DELETE().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 204;
    }

    private HttpRequest.Builder requestBuilder(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
        if (sessionCookie != null) b.header("Cookie", sessionCookie);
        return b;
    }

    private HttpRequest.BodyPublisher jsonBody(Object obj) throws IOException {
        String json = mapper.writeValueAsString(obj);
        return HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
    }

    private void captureCookie(HttpResponse<?> resp) {
        Optional<String> setCookie = resp.headers().firstValue("Set-Cookie");
        setCookie.ifPresent(v -> {
            int semi = v.indexOf(';');
            sessionCookie = semi > 0 ? v.substring(0, semi) : v;
        });
    }

    private static String url(String s) { return s == null ? "" : s.replace(" ", "+"); }
}
