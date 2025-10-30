# Book Store DB — Project Plan (Java stack)

This repository contains planning files and scaffolding notes for the Book Store DB project. The chosen stack for this plan is Java (Spring Boot backend + JavaFX desktop client) with MySQL as the database.

What's included:
- `PLAN.md` — Detailed project plan and phased roadmap (Java-first).
- `.gitignore` — Standard Java/Maven/IDE ignores.
- `requirements.txt` — Placeholder (Java projects typically use Maven/Gradle; see notes below).

Getting started (local dev):

1. Install Java 17 or later (OpenJDK recommended).
2. Install Maven or Gradle. The plan assumes Maven but you can use Gradle if you prefer.
3. Install MySQL and create a database for the project.
4. Use `application.properties` (or environment variables) to configure DB credentials and JWT secrets. Do NOT commit secrets to the repo.

Recommended next steps:
1. Scaffold a Spring Boot project (Maven) with dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, mysql-connector-j, jjwt (or spring-security-oauth2-jose), spring-boot-starter-mail.
2. Scaffold a JavaFX client module that authenticates and calls the REST API.

If you'd like, I can scaffold the Maven project with the starter dependencies and create an initial `User` model + auth endpoints.
