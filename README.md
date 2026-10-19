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

The application covers registration/login, business onboarding, a business tax profile, profile-driven obligations, a personalized calendar and dashboard, filing-period tax calculations and explanations, CSV compliance-pack exports, filing-status tracking, in-app deadline reminders, CSV books-to-tax reconciliation, existing-user business collaboration, document uploads, Pre-KRA consistency checks, and a role-protected system administration console for user/business management, versioned tax rules, and platform audit activity. Tax schedule rules are stored with versions and effective dates, and calculations are deterministic and auditable. Filing-status updates are for tracking only; TaxPilot does not submit returns or payments to KRA. Email/SMS reminders and direct KRA/eTIMS integrations are not currently available.

## Quick start

1. Copy `.env.example` to `.env` and set private database and JWT credentials. For Supabase, use the project host, database, username, password and `DB_SSLMODE=require`. Set `CORS_ALLOWED_ORIGINS` to the frontend's origin (comma-separated if there are multiple). `.env` is git-ignored.
2. Start the stack:
   ```bash
   docker compose up --build
   ```
   When no `ADMIN` or `SUPER_ADMIN` exists, startup creates `admin@taxpilot.com`, generates a random password, hashes it, and prints the credentials once to backend startup logs. Save them securely: anyone who can read application logs can see these initial credentials. Later startups do not change existing accounts or reveal the password again. If that email already belongs to a non-admin, startup fails rather than promoting the account. With Docker Compose, retrieve startup output using `docker compose logs backend`.
   To replace the generated account credentials with your own, set `ADMIN_CREDENTIAL_ROTATION_ENABLED=true`, `ADMIN_CREDENTIAL_ROTATION_EMAIL=admin@taxpilot.com`, and a strong `ADMIN_CREDENTIAL_ROTATION_PASSWORD` (at least 12 characters, at most 72 UTF-8 bytes) in your untracked `.env`, then start the backend once. Rotation targets only the generated `system-admin-…@taxpilot.local` account. After a successful run, set the flag back to `false` and clear the password from `.env`. Do not share or commit the password.
   To reset an existing `admin@taxpilot.com` password, set `ADMIN_PASSWORD_RESET_ENABLED=true` and a new UUID in `ADMIN_PASSWORD_RESET_REQUEST_ID` in `.env`, then start the backend. A new random password is generated and printed once after the database transaction commits. After success, disable the flag and clear the request ID. The database records each request ID, so reusing one cannot reset the password again; use a fresh UUID for a future authorized reset. Protect startup logs because they contain the temporary password.
3. Frontend: http://localhost:4200
4. Backend: http://localhost:8080

## Development

- Backend:
  ```bash
  cd backend
  # Set DB_HOST=localhost when connecting to a local PostgreSQL server.
  # The backend loads the repository-level .env. If a new admin is created, credentials appear in startup output.
  mvn spring-boot:run
  ```

- Frontend:
  ```bash
  cd frontend
  npm install
  npm start -- --host 0.0.0.0 --port 4200
  ```
