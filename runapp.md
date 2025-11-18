# Run the Book Store App (backend + JavaFX frontend)

This file lists the common commands and environment variables you can use to run the backend and the JavaFX frontend from this repository. Pick the variant you want (dev in-memory H2, file-based H2, or persistent MySQL).

## Prereqs
- Java 17+ and Maven installed
- (Optional) MySQL server if you want persistent DB storage
- For JavaFX frontend on WSL: a working X server or run on a native Linux/GUI environment. Export `DISPLAY` and make sure GTK/OpenGL libraries are available.

Repository layout (relevant):
- project root: contains backend Spring Boot app and `pom.xml`
- `javafx_frontend/` (module) contains the JavaFX client (run with `mvn javafx:run` inside that module)

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
curl -s http://localhost:8080/auth/me
```

---

### 7) Troubleshooting & tips
- If your data keeps “disappearing”, confirm which profile you started with:
	- `SPRING_PROFILES_ACTIVE=dev` -> in-memory H2 (ephemeral)
	- no dev profile -> uses MySQL (persistent)
- To stop seeders from running each start, set `APP_SEED_ENABLED=false` or `--app.seed.enabled=false`.
- Consider adding Flyway/Liquibase migrations for deterministic schema management instead of runtime seeders.

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
