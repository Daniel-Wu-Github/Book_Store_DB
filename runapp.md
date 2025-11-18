# Run the Book Store App (backend + JavaFX frontend)

This file documents up-to-date commands and environment variables to run the backend and the JavaFX frontend. It also describes the `run.sh` helper that starts both services and performs a MySQL preflight check (TCP + auth) so failures are detected early.

## Prereqs
- Java 11+ (project compiled with Java 11) and Maven installed
- MySQL server for persistent DB storage (or use H2 alternatives below)
- For JavaFX frontend on WSL: a working X server or run on a native Linux/GUI environment. Export `DISPLAY` and make sure GTK/OpenGL libraries are available.

Repository layout (relevant):
- project root: contains backend Spring Boot app and `pom.xml`
- `javafx_frontend/` (module) contains the JavaFX client (run with `mvn javafx:run` inside that module)

Files of interest:
- `run.sh` — helper script that starts backend + frontend, preserves environment overrides, performs a TCP+auth MySQL preflight, and tails logs in `run-logs/`.
- `.env` — recommended place for local DB credentials (sourced by `run.sh`, but command-line env vars override `.env`).

---

### 1) Run backend (development, in-memory H2 — ephemeral)
Use this when you want a fresh DB on each run (faster local dev).

```bash
# start with dev profile (this uses an in-memory H2 DB, non-persistent)
# from project root
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Notes:
- `application-dev.properties` sets `spring.datasource.url=jdbc:h2:mem:bookstore` and `spring.jpa.hibernate.ddl-auto=create-drop`.
- Data will NOT persist between restarts in this mode.

---

### 2) Run backend (persistent MySQL)
Use this when you want persistence across restarts.

```bash
# Example: export DB credentials then run
export BOOK_DB_HOST=127.0.0.1
export BOOK_DB_PORT=3306
export BOOK_DB_NAME=bookstore
export BOOK_DB_USER=book_user
export BOOK_DB_PASS=your_mysql_password
# Optionally disable seeders so they don't run every startup (recommended after initial seeding)
export APP_SEED_ENABLED=false
# Run (no 'dev' profile; picks up application.properties -> MySQL)
mvn spring-boot:run
```

Alternative: pass properties on the mvn command line

```bash
#mvn spring-boot:run -Dspring-boot.run.arguments="--app.seed.enabled=false"
```

---

### 3) Run backend from packaged JAR

```bash
mvn clean package
# then (set same env vars as above)
export BOOK_DB_USER=book_user
export BOOK_DB_PASS=your_mysql_password
export APP_SEED_ENABLED=false
java -jar target/*-SNAPSHOT.jar
```

---

### 4) Run backend with file-based H2 (persistent on disk)
If you prefer not to run MySQL locally, use an H2 file DB. This keeps data on disk between runs.

```bash
# override the datasource URL to use a file in the project directory
export SPRING_DATASOURCE_URL='jdbc:h2:file:./data/bookstore;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE'
# also avoid create-drop so data isn't wiped
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
# run
mvn spring-boot:run
```

---

### 5) Run the JavaFX frontend
The frontend is a Maven module using `javafx-maven-plugin`. Run it from the module directory or use `-pl` to specify the module.

```bash
# Option A: change into module and run
cd javafx_frontend
mvn javafx:run
# Option B: from project root (module path may vary)
mvn -pl javafx_frontend javafx:run
```

Notes for JavaFX on WSL / headless environments
- Ensure `DISPLAY` is set and an X server is running (e.g. X11, VcXsrv, X410). Example:

```bash
export DISPLAY=localhost:0.0
```

- If you encounter GTK/GLX errors, try running on a native Linux desktop or use X server settings that match your environment.

---

### 6) Quick verification (after backend starts)

```bash
# check health / sample endpoints
curl -s http://localhost:8080/api/books | jq '.'
curl -s http://localhost:8080/api/auth/me
```

---

### 7) Troubleshooting & tips

Important notes about `run.sh` and MySQL:

- `run.sh` now preserves any environment variables you set on the command line (for example `APP_SEED_ENABLED=true ./run.sh` will be honored). `.env` is sourced but does not override already set env vars.
- `run.sh` runs a MySQL preflight that:
	- checks TCP reachability to `${BOOK_DB_HOST:-127.0.0.1}:${BOOK_DB_PORT:-3306}`
	- runs a `mysql --protocol=TCP` auth check using `BOOK_DB_USER` and `BOOK_DB_PASS` (if the mysql client exists)
	- fails fast with actionable SQL statements if the auth check fails (CREATE/ALTER/GRANT examples)

This preflight mirrors how the Spring Boot app connects (JDBC over TCP). If you previously saw a `mysql` client test succeed but the app failed, it was likely because the client connected over the Unix socket (when host is `localhost`) while the app used TCP — creating mismatched results. Use `127.0.0.1` for TCP to avoid socket vs TCP differences.

To allow the app to seed (one-time) and then disable seeding, run:
```bash
# Run seeding once (force seeder execution)
APP_SEED_ENABLED=true ./run.sh

# After seed completes, set APP_SEED_ENABLED=false in .env
```

If you run into `ERROR 1045` (Access denied) or plugin mismatches, use `sudo mysql` (WSL) or run the MySQL admin flow and then run:
```sql
ALTER USER 'book_user'@'localhost' IDENTIFIED WITH caching_sha2_password BY 'strong_password';
ALTER USER 'book_user'@'127.0.0.1' IDENTIFIED WITH caching_sha2_password BY 'strong_password';
FLUSH PRIVILEGES;
```

Log files: `run-logs/backend.log` and `run-logs/frontend.log` (tailing is handled by `run.sh`).

---

### 8) Useful environment variable summary

```bash
# MySQL credentials
export BOOK_DB_USER=book_user
export BOOK_DB_PASS=your_mysql_password
# Toggle seeders
export APP_SEED_ENABLED=false
# Profile selector (use 'dev' for in-memory H2)
export SPRING_PROFILES_ACTIVE=dev
```

If you want, I can:
- change the `dev` profile to use a file-based H2 by default,
- update the `README.md` with a short run section, or
- scaffold Flyway migrations and an initial baseline migration.
