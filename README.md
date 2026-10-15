# TaxPilot

TaxPilot is a Kenyan SME tax compliance platform built as a monorepo with a Spring Boot backend, an Angular frontend, infrastructure assets, and project documentation.

## Structure

- `backend/` - Java 21 / Spring Boot 4.1.1 REST API
- `frontend/` - Angular application
- `infrastructure/` - deployment and reverse-proxy configuration
- `docs/` - architecture and compliance documentation
- `docker-compose.yml` - local orchestration for app services
- `.env.example` - environment variable template

## Current implementation scope

The application covers registration/login, business onboarding, a business tax profile, profile-driven obligations, a personalized calendar and dashboard, filing-period tax calculations, filing-status tracking, in-app deadline reminders, document uploads, and Pre-KRA consistency checks. Tax schedule rules are stored with versions and effective dates, and calculations are deterministic and auditable. Filing-status updates are for tracking only; TaxPilot does not submit returns or payments to KRA. Email/SMS reminders and direct KRA/eTIMS integrations are not currently available.

## Quick start

1. Copy `.env.example` to `.env` and set private database and JWT credentials. For Supabase, use the project host, database, username, password and `DB_SSLMODE=require`. Set `CORS_ALLOWED_ORIGINS` to the frontend's origin (comma-separated if there are multiple). `.env` is git-ignored.
2. Start the stack:
   ```bash
   docker compose up --build
   ```
3. Frontend: http://localhost:4200
4. Backend: http://localhost:8080

## Development

- Backend:
  ```bash
  cd backend
  # Set DB_HOST=localhost when connecting to a local PostgreSQL server.
  # The backend loads the repository-level .env; set JWT_SECRET there as well.
  mvn spring-boot:run
  ```

- Frontend:
  ```bash
  cd frontend
  npm install
  npm start -- --host 0.0.0.0 --port 4200
  ```
