Alright, Daniel — this looks like a classic, well-scoped CS project. I've read the Product Requirements Document (PRD)[cite: 1] and translated it into a focused, Java-first implementation plan that fits your goal of a full-stack (backend + database + desktop client) portfolio project.

This plan omits Python options and assumes a Java stack (Spring Boot backend + JavaFX or Swing desktop GUI). It breaks the work into phases with concrete tasks and acceptance criteria.

Scaffold note: I added a `pom.xml` (Maven) to this repository that includes Spring Boot starters, JWT libs, Lombok, and compiler/plugin configuration (Java 17). The POM includes the maven-compiler-plugin with Lombok as an annotation processor and the Spring Boot Maven plugin so you can build a runnable JAR with `mvn clean package`.


## High-level contract
- Inputs: user actions from the desktop client (register, login, search, add-to-cart, place order, manager actions).
- Outputs: REST API responses (JSON), emails for order receipts, persistent data in MySQL.
- Error modes: invalid auth, DB errors, network timeouts, validation failures.
- Success: full end-to-end flows for CUSTOMER and MANAGER roles work, GUI stays responsive, and basic security (hashed passwords, JWTs, role checks) is implemented.

Alright, Daniel — this looks like a classic, well-scoped CS project. I've read the Product Requirements Document (PRD)[cite: 1] and translated it into a focused, Java-first implementation plan that fits your goal of a full-stack (backend + database + desktop client) portfolio project.

This plan is explicitly Java-only (no Python options). It assumes a Spring Boot backend and a JavaFX or Swing desktop GUI. The document breaks the work into phases with concrete tasks and acceptance criteria.

## High-level contract
- Inputs: user actions from the desktop client (register, login, search, add-to-cart, place order, manager actions).
- Outputs: REST API responses (JSON), emails for order receipts, persistent data in MySQL.
- Error modes: invalid auth, DB errors, network timeouts, validation failures.
- Success: full end-to-end flows for CUSTOMER and MANAGER roles work, GUI stays responsive, and basic security (hashed passwords, JWTs, role checks) is implemented.

## Phase 0: Setup, Design, and Tech Stack Choice

1. Choose your tech stack (Java-only):
   - Backend: Java with Spring Boot (RESTful API, embedded Tomcat, easy dependency management via Maven or Gradle).
   - Frontend (desktop client): JavaFX (recommended) or Swing for the GUI.
   - Database: MySQL (as specified in PRD)[cite: 27].

2. Database Schema (initial):
   - `Users`:
     - `user_id` INT AUTO_INCREMENT PRIMARY KEY
     - `username` VARCHAR(...) UNIQUE
     - `password_hash` VARCHAR(...) -- store bcrypt hashes
     - `email` VARCHAR(...) UNIQUE
     - `role` ENUM('CUSTOMER','MANAGER')
   - `Books`:
     - `book_id` INT AUTO_INCREMENT PRIMARY KEY
     - `title` VARCHAR(...)
     - `author` VARCHAR(...)
     - `buy_price` DECIMAL(...)
     - `rent_price` DECIMAL(...)
     - `stock` INT -- track availability
   - `Orders`:
     - `order_id` INT AUTO_INCREMENT PRIMARY KEY
     - `user_id` INT REFERENCES Users(user_id)
     - `order_date` DATETIME
     - `total_amount` DECIMAL(...)
     - `payment_status` VARCHAR(...) -- e.g., 'Pending','Paid'
   - `Order_Items`:
     - `item_id` INT AUTO_INCREMENT PRIMARY KEY
     - `order_id` INT REFERENCES Orders(order_id)
     - `book_id` INT REFERENCES Books(book_id)
     - `type` ENUM('BUY','RENT')
     - `price_at_purchase` DECIMAL(...)

3. Environment setup:
   - Initialize Git repo (if not already done).
   - Install Java 17+ (OpenJDK recommended), Maven or Gradle, MySQL server.
   - Create a database schema in MySQL and add seed data to `Books` for testing[cite: 37].
   - Add a `.env` or `application.properties` file to hold DB credentials locally (don't commit secrets).

### Build tool: Do we have to use Maven? What is Maven?

Short answer: No, you don't have to use Maven — but it's the most common and beginner-friendly choice. Gradle is the other popular alternative. Both are modern build tools that handle dependencies, compilation, testing, packaging, and plugins.

What Maven is (brief):
- Maven is a build automation and dependency management tool for Java projects. It uses a declarative XML file called `pom.xml` to describe the project, dependencies (libraries), build lifecycle (compile, test, package), and plugins (for running Spring Boot, creating JARs, etc.).
- Key benefits:
  - Declarative dependency management (resolve transitive libraries automatically).
  - Standardized lifecycle (clean, compile, test, package, install, deploy).
  - Large plugin ecosystem (Spring Boot plugin, JavaFX packaging, jlink/jpackage, etc.).

Alternatives:
- Gradle: uses a Groovy or Kotlin DSL, generally faster and more flexible. Many teams prefer it for performance and expressive build scripts.
- Ant + Ivy: older; more manual; rarely recommended for new projects.
- Manual javac/jar classpath management: possible for tiny projects but becomes unmanageable quickly.

When to pick which:
- Use Maven if you want convention, wide documentation, and simple XML configuration. It's great for learning and standard projects.
- Use Gradle if you want faster incremental builds, more concise scripts, and advanced customization.

Common Maven commands you'll see in docs (for reference):
- `mvn -v` (shows Maven version)
- `mvn clean package` (builds and packages the app into a JAR/WAR)
- `mvn spring-boot:run` (run a Spring Boot app in development)

If you'd like, I can scaffold a Maven `pom.xml` and a Spring Boot project structure for you. Or I can scaffold a Gradle build instead — tell me which you prefer.

## Phase 1: Backend (RESTful API) Development

Notes: Build and unit-test API endpoints first. Use Postman/Insomnia for manual verification.

1. Authentication (FR1.x, NFR2.x)
   - `POST /api/auth/register` — register new users. Hash passwords with bcrypt (NFR2.1)[cite: 32].
   - `POST /api/auth/login` — verify credentials and return a JWT access token (NFR2.2) on success.

2. Book Catalog (FR2.x)
   - `GET /api/books/search?q=keyword` — search by title OR author; return JSON list of books (FR2.2, FR2.3)[cite: 18]. Require Authorization header (Bearer token).

3. Ordering (FR3.x, FR4.x)
   - `POST /api/orders` — place an order (authenticated). Request body: list of {bookId, type: 'BUY'|'RENT'}.
     - Create `Orders` row, create `Order_Items`, calculate `total_amount`, decrement `Books.stock` where applicable, set `payment_status` (e.g., 'Pending').
     - Trigger email receipt via an email service (SMTP or third-party) with order summary (FR4.2)[cite: 22, 35].

4. Manager endpoints (FR5.x)
   - Protect with role checks (ensure JWT includes `role` claim; deny access if not MANAGER) (NFR2.3)[cite: 32].
   - `GET /api/manager/orders` — list all orders (FR5.2).
   - `PUT /api/manager/orders/{order_id}` — update `payment_status` (FR5.3).
   - `POST /api/manager/books` — add book (FR5.4).
   - `PUT /api/manager/books/{book_id}` — update book info (FR5.4).

Implementation details & small contract for endpoints:
- Authentication: JWT with reasonable expiry; refresh tokens optional.
- Passwords: bcrypt (work factor configurable via properties).
- Input validation: use DTOs and validation annotations; return proper 4xx error codes.

Edge cases to consider:
- Concurrent orders consuming the same book stock (use DB transactions / SELECT ... FOR UPDATE or optimistic checks).
- Partial failures during order creation (ensure DB transactions wrap Orders + Order_Items + stock updates).
- Email failures should not block order creation — enqueue retry or log and surface to manager.

## Phase 2: Frontend (Desktop GUI) Development

Requirements:
- GUI must not freeze during network calls (use background threads / JavaFX Task / ExecutorService) (NFR1.2)[cite: 30].

Views to implement:
- Login/Register view: forms to create account and login; on success, store JWT in memory for session.
- Customer main view:
  - Search bar -> `GET /api/books/search`.
  - Results list with "Add to Cart (Buy)" / "Add to Cart (Rent)" actions.
  - Cart view with ability to Place Order -> calls `POST /api/orders`.
- Manager main view: separate role-based UI (or a switch after manager login).
  - Orders management: table of orders from `GET /api/manager/orders` and ability to change payment status (PUT).
  - Book management: add/update book forms.

UX notes:
- Keep network calls asynchronous.
- Display transient errors and retries gracefully.

## Phase 3: Integration, Testing, and Packaging

1. Integration tests / manual QA:
   - Register, login, search, add to cart, place order (buy + rent), verify DB rows and email receipt.
   - Manager login -> verify that manager can see and update orders; customer cannot access manager endpoints.
   - Check GUI responsiveness under slow network conditions.

2. Packaging:
   - Backend: build a runnable JAR via Maven/Gradle with profile for production.
   - Desktop client: produce a runnable JAR (JavaFX bundling) or a native installer using jpackage if desired.

## Minimal development roadmap (sprints)
1. Sprint 1 (Days 1–3): Project scaffolding, DB schema, seed data, bare Spring Boot app, register/login + JWT.
2. Sprint 2 (Days 4–7): Book search endpoints, simple client UI for search.
3. Sprint 3 (Days 8–12): Place order flow, Order_Items, email integration.
4. Sprint 4 (Days 13–16): Manager endpoints and manager UI.
5. Sprint 5 (Days 17–20): Integration testing, packaging, README and polishing.

## Acceptance Criteria
- All API endpoints listed above return correct HTTP status codes and JSON bodies on success/failure.
- Passwords are stored hashed with bcrypt; JWTs guard protected endpoints.
- GUI stays responsive during network calls.
- Managers are blocked from manager endpoints if they are not in the MANAGER role.

---
If you'd like, I can now:
- scaffold a Maven-based Spring Boot project with basic auth and a sample model,
- or scaffold a JavaFX client that talks to a mocked backend.

Tell me which of those you'd prefer me to create next and I'll scaffold it.
