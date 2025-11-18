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
