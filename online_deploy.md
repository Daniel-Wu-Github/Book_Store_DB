**Online Deployment Plan**

This document outlines a practical, minimal-risk plan to deploy the repository's frontend (React/Vite) and backend (Spring Boot / Maven) to the public internet. It includes repository-accurate facts, recommended providers, concrete steps to prepare the codebase, and a CI/CD approach.

**Confirmed repo facts (from repository scan)**
- **Repo layout:** `frontend/` contains a Vite + React web app; there is also a `javafx_frontend/` module (desktop JavaFX client). The backend is a Maven Spring Boot application at the repo root with `pom.xml` and `src/main/java`.
- **Java / Spring Boot versions:** The project uses Java 11 and Spring Boot 2.7.x (see `pom.xml`).
- **Database:** The project is configured for MySQL (the project includes `mysql-connector-java` and `schema.sql` uses MySQL syntax). Development configs rely on `BOOK_DB_*` env vars and `application.properties`/`application-dev.properties`.
- **Run helper:** `run.sh` sources `.env` and supports `BOOK_DB_HOST/PORT/NAME/USER/PASS` and `APP_SEED_ENABLED` for local runs. It performs a MySQL TCP preflight and optionally launches the JavaFX frontend.
- **Health endpoint:** The project currently does not include `spring-boot-starter-actuator`; add it or a custom `/health` endpoint for platform probes.

**Goals**
- **Frontend (web):** Serve built static assets from `frontend/` using Vercel/Netlify/GitHub Pages or a CDN.
- **Backend:** Run the Spring Boot app (Java 11) on a managed container/PaaS (Render, Railway, Fly.io, Cloud Run, or DigitalOcean App Platform). The platform should run the built jar or a Docker image.
- **DB:** Use a managed MySQL instance (Cloud SQL, Amazon RDS, DigitalOcean Managed DB, Render DB, Railway DB) to match the application's schema.
- **CI/CD:** Automate build, test and deploy with GitHub Actions (or provider native integrations).

**Recommended Providers (quick reference)**
- **Frontend:** Vercel or Netlify for Vite + React (fast integration and automatic builds).
- **Backend / DB:** Render, Railway, Fly.io, DigitalOcean App Platform, or Google Cloud Run + Cloud SQL. For simplicity and rapid iteration, Render or Railway are good choices; they also offer managed databases.

**High-level Plan (phases)**
1. Prepare backend for production (externalize config, add health check/actuator).
2. Add containerization or provide a reliable jar-build path (Docker optional but recommended).
3. Provision managed MySQL and load `schema.sql` (or let the app seed if `APP_SEED_ENABLED=true`).
4. Deploy backend (via Docker image or direct jar) and enable HTTPS.
5. Build and deploy frontend to a static host or CDN and configure API base URL.
6. Wire DNS, set custom domain and TLS, test end-to-end.
7. Add monitoring, logs, backups, and rollback plan.

**Detailed Steps**

**1) Prep backend for production**
- **Profiles:** Add `application-prod.properties` and ensure the app reads `SPRING_PROFILES_ACTIVE` when appropriate.
- **Externalize config:** The repo already supports `BOOK_DB_HOST`, `BOOK_DB_PORT`, `BOOK_DB_NAME`, `BOOK_DB_USER`, and `BOOK_DB_PASS` via `application-dev.properties` and `run.sh`. For cloud deployments you can either provide a `SPRING_DATASOURCE_URL` (JDBC URL) or set the `BOOK_DB_*` env vars, for example:
  - `BOOK_DB_HOST`, `BOOK_DB_PORT`, `BOOK_DB_NAME`, `BOOK_DB_USER`, `BOOK_DB_PASS` OR
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- **CORS:** If deploying web frontend to a different origin, update Spring Security / WebMvc config to allow that origin (or pass allowed origin via env var `FRONTEND_ORIGIN`).
- **Health & readiness:** Add `spring-boot-starter-actuator` to `pom.xml` or implement a lightweight `/health` endpoint so the platform can probe readiness and liveness.

**2) Containerization (recommended)**
- **Backend Dockerfile** (Java 11):
```dockerfile
FROM eclipse-temurin:11-jre-slim
WORKDIR /app
COPY target/*.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
```
- **Frontend Dockerfile** (optional; Vercel/Netlify do not require Docker):
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

**3) Database provisioning & schema**
- **Provision MySQL:** Use a managed MySQL instance from your chosen provider. Ensure credentials and the public/private networking policy are set so your backend can reach the DB.
- **Schema:** Load `schema.sql` into the managed DB (provider console or `mysql` client). The repo includes sample data and expects MySQL types.
- **Seeding:** The app can seed data when `APP_SEED_ENABLED=true` (see `run.sh` and `application.properties`). In production you may prefer manual migration tooling and to disable auto-seeding.

**4) CI/CD & Deployment**
- **Build steps (GitHub Actions recommended):**
  - Build & test backend: `mvn -B package` (target Java 11, Spring Boot 2.7).
  - Build frontend: `npm ci && npm run build` inside `frontend/` (set `VITE_API_BASE_URL` before build).
  - Optionally build/push Docker images to GHCR or Docker Hub, or let the provider build from your repo.
  - Deploy via provider integration or provider API (Render, Railway, Fly.io, Cloud Run deploy commands / GitHub integration).
- **Secrets:** Store DB credentials, SMTP credentials, and registry tokens in GitHub Actions secrets or provider secret stores.
- **Sample workflow:** create `.github/workflows/deploy.yml` that builds, runs tests, publishes artifacts/images, and triggers deployment.

**5) Frontend production settings**
- **API URL:** For Vite, use an env var like `VITE_API_BASE_URL` at build time to point to the backend (CI should set this before `npm run build`).
- **Routing:** If the React app uses client-side routing, configure rewrites on the static host to serve `index.html` for unmatched routes.

**6) DNS, TLS, and domain**
- **Attach domain** via provider UI and configure DNS records (A/CNAME). For Vercel/Netlify this is straightforward.
- **TLS:** Most providers manage Let's Encrypt automatically; enable automatic cert issuance.

**7) Monitoring, logging, and alerting**
- **Logs:** Use provider logs and aggregate them if needed (Papertrail, Datadog, etc.). The app writes to stdout (Spring Boot) and can be captured by the platform.
- **Backups & metrics:** Enable managed DB backups and basic uptime checks.

**8) Rollout & rollback**
- **Strategy:** Deploy to a staging environment first. Keep a known-good Docker image/tag for quick rollback.
- **Backups:** Enable automated DB backups on the managed DB.

**Minimal checklist of files to add (suggested)**
- `backend/Dockerfile` (or root `Dockerfile`) — uses Java 11 base image as shown above.
- `frontend/Dockerfile` (optional) or rely on Vercel/Netlify.
- `src/main/resources/application-prod.properties` — keep sensitive values in env vars and document required env names: `BOOK_DB_HOST`, `BOOK_DB_PORT`, `BOOK_DB_NAME`, `BOOK_DB_USER`, `BOOK_DB_PASS`, `APP_SEED_ENABLED`, `VITE_API_BASE_URL`.
- `.github/workflows/deploy.yml` — GitHub Actions workflow to build/test and deploy.

**Quick commands (local verification)**
- Build backend jar:
```bash
mvn -B package
```
- Build frontend locally:
```bash
cd frontend
npm ci
npm run build
```
- Run backend locally with environment variables (example using `BOOK_DB_*` vars used by `run.sh`):
```bash
BOOK_DB_HOST=127.0.0.1 \
BOOK_DB_PORT=3306 \
BOOK_DB_NAME=bookstore \
BOOK_DB_USER=book_user \
BOOK_DB_PASS=secret \
APP_SEED_ENABLED=true \
mvn spring-boot:run
```

**Notes & gotchas (repo-specific)**
- The repo supplies `run.sh` which prefers `BOOK_DB_*` env vars and performs a MySQL preflight. For cloud deploys, either set the JDBC URL via `SPRING_DATASOURCE_URL` or set the `BOOK_DB_*` envs on the platform.
- Add `spring-boot-starter-actuator` to `pom.xml` if you want `/actuator/health` for readiness/liveness probes.
- The project uses Java 11; ensure your Docker images and provider runtime match Java 11 / Spring Boot 2.7 compatibility.
- The repo contains both a web frontend (`frontend/`) and a desktop JavaFX app (`javafx_frontend/`). If you plan to publish a web UI, deploy the Vite `frontend/` app. The JavaFX client is a separate desktop artifact and not part of a web deployment.

**Next steps I can take for you**
- Add `backend/Dockerfile` and `frontend/Dockerfile` (or a targeted `Dockerfile` for chosen provider).
- Scaffold `.github/workflows/deploy.yml` that builds backend and frontend, pushes images (optional), and triggers deployment to a provider you choose.
- Add `spring-boot-starter-actuator` and a minimal `application-prod.properties` that relies on env vars.

Tell me which provider you'd like for the backend and frontend (examples: Render, Railway, Fly.io, Google Cloud Run for backend; Vercel/Netlify for frontend). I will scaffold the Dockerfiles and a GitHub Actions workflow tailored to that choice.
