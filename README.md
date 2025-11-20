# Online Bookstore (Backend + JavaFX Frontend)

This repository contains a Spring Boot backend and a JavaFX frontend for a small online bookstore project used for CSCE 310 coursework.

Key features implemented:
- Book catalog with both buy and computed rent prices (`rentPrice` = 20% of buy price) shown in public and admin UIs.
- Admin book management: add / edit books via Admin UI (uses `PUT /api/books/{id}`).
- Orders support BUY and RENT items; RENT subtotals are calculated as rent unit price * quantity * rentalDays.
- Email sending and auditing: order confirmation emails are sent after commit and all attempts (automatic and manual resends) are recorded in the `order_emails` table.

Security note: Do not commit `.env` or any credentials. If any secrets are committed, rotate them immediately.

Build (backend):

1. Ensure Java and Maven are installed.
2. From the project root:

```bash
mvn -DskipTests package
```

The built jar will be in `target/` (e.g. `target/online-bookstore-backend-0.0.1-SNAPSHOT.jar`).

Running the application:

- This project provides `./run.sh` which sources the `.env` file and starts the backend. See `runapp.md` for detailed run & env instructions.

Environment variables (`.env`):
- The app reads configuration (database, SMTP, etc.) from a local `.env` file. `run.sh` sources it when launching.
- For Gmail SMTP use an App Password (if the account has 2FA enabled). If you see `535 BadCredentials` in the logs or audit rows in `order_emails`, create a new Google App Password and update `SPRING_MAIL_PASSWORD` in `.env`.

Database:
- Default is MySQL (see `.env` variables `BOOK_DB_*`). The app's seeding is controlled by `APP_SEED_ENABLED`.

Where to look next:
- Run instructions and troubleshooting: `runapp.md` (new file).
- Email send/audit code: backend `OrderPlacedListener`, `SmtpEmailService`, and `OrderEmailAttempt` entity/repository.

If you want, I can restart the app here and tail logs to verify the new `.env` is applied.
# Book Store DB — Project Plan & Maven scaffold (Java stack)

This repository contains planning files and a starter Maven scaffold for the Book Store DB project.

What's included:
- `PLAN.md` — Detailed project plan and phased roadmap (Java-first).
- `pom.xml` — Maven build file with Spring Boot starters and recommended plugins (scaffolded).
- `.gitignore` — Standard Java/Maven/IDE ignores.
- `requirements.txt` — Notes about build and runtime dependencies (Java-focused).

Quick prerequisites

1. Install Java 17 or later (OpenJDK recommended).
2. Install Maven (or Gradle if you prefer; this repo currently includes a Maven `pom.xml`).
3. Install MySQL and create a database for the project.

Run helper (`run.sh`)
---------------------

This project includes a `run.sh` helper in the repository root that starts the backend and the JavaFX frontend together. Highlights:

- `run.sh` sources `.env` (if present) but preserves any environment variables you pass on the command line. Example: `APP_SEED_ENABLED=true ./run.sh`.
- Before starting the backend it performs a MySQL preflight check over TCP (uses `mysql --protocol=TCP` if the client is available). If TCP/auth fails the script exits with actionable SQL hints.
- Logs are written to `run-logs/backend.log` and `run-logs/frontend.log`, and `run.sh` tails them for convenience.

This helper is convenient for development; see `runapp.md` for full run instructions and troubleshooting.

How to build and run the backend (development)

1. Build the project and run tests:

```bash
mvn clean package
```

2. Run the Spring Boot app in development:

```bash
mvn spring-boot:run
```

3. The app will read DB credentials and secrets from `src/main/resources/application.properties` or environment variables. Do NOT commit real secrets to git.

Notes

- The provided `pom.xml` includes common Spring Boot starters (web, data-jpa, security, mail), JWT libs (jjwt) and Lombok configuration. You can switch to Spring Security's resource-server / jose modules later if you prefer tighter framework integration for JWTs.
- If you want a Gradle build instead, tell me and I'll convert the POM to `build.gradle`.

Next steps I can take for you

- Scaffold a minimal Spring Boot app (User entity, repository, auth controller) and seed data.
- Or scaffold a JavaFX client that connects to the API.
