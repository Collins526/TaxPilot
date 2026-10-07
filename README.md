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

The first end-to-end slice covers registration/login, business onboarding, a business tax profile, profile-driven obligations, a personalized calendar, and a dashboard. Tax schedule rules are stored with versions and effective dates. Estimates remain explicitly unavailable until the calculation rules and filing inputs are implemented; the dashboard does not invent amounts. AI, KRA/eTIMS integrations, Pre-KRA reconciliation, and document uploads are not part of this phase.

## Quick start

1. Copy `.env.example` to `.env` and set private database and JWT credentials. For Supabase, use the project host, database, username, password and `DB_SSLMODE=require`. `.env` is git-ignored.
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
  # Set JWT_SECRET to a private value of at least 32 characters before starting.
  mvn spring-boot:run
  ```

- Frontend:
  ```bash
  cd frontend
  npm install
  npm start -- --host 0.0.0.0 --port 4200
  ```
