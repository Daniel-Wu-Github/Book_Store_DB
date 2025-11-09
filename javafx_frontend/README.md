## JavaFX Frontend

Standalone JavaFX client for the Book Store backend. Provides:

- Login & registration
- Book search & listing
- Place order (mix BUY / RENT items with rental days)
- View own orders
- Admin/Manager: list all orders & update payment status

Run:

```bash
mvn -f javafx_frontend/pom.xml javafx:run
```

Configure backend URL via environment variable `BOOKSTORE_API_BASE` (default: `http://localhost:8080/api`).

Profiles / settings:
- Session cookie stored in memory on successful login and reused for subsequent calls.
- Simple API client with Jackson for JSON mapping.

This module intentionally kept separate from the main backend build.

run back end
```bash
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```