# API Overview

The backend exposes a REST API for authentication, business onboarding, tax summaries,
filing periods, document uploads, and the explanation-only assistant.

All endpoints except `POST /api/auth/register` and `POST /api/auth/login` require an
`Authorization: Bearer <accessToken>` header. Business-scoped endpoints enforce active
membership of the caller for the target business.

## Authentication

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout` — revokes the presented access token (and optional `refreshToken` body field) in Redis.

## Businesses

- `GET /api/businesses`
- `POST /api/businesses`
- `GET /api/businesses/{businessId}`
- `PUT /api/businesses/{businessId}`
- `GET /api/businesses/{businessId}/obligations`
- `GET /api/businesses/{businessId}/calendar`
- `GET /api/businesses/{businessId}/tax-health`
- `GET /api/businesses/{businessId}/dashboard`
- `GET /api/businesses/{businessId}/audit-logs?limit=50`

## Tax profile

- `GET /api/businesses/{businessId}/tax-profile`
- `PUT /api/businesses/{businessId}/tax-profile`

## Filing periods and tax calculation

- `POST /api/businesses/{businessId}/filing-periods` — records a period's figures and runs the deterministic rules engine.
- `GET /api/businesses/{businessId}/filing-periods`
- `GET /api/businesses/{businessId}/filing-periods/{periodId}`
- `GET /api/businesses/{businessId}/pre-kra-check` — consistency checks against the latest filing period.

## Documents

- `POST /api/businesses/{businessId}/documents` — `multipart/form-data` with `file` (required) and `category` (optional). Allowed types: PDF, PNG, JPEG, WEBP, CSV, XLSX; max 10 MB. Stored via Cloudinary.
- `GET /api/businesses/{businessId}/documents`

## Assistant

- `POST /api/businesses/{businessId}/assistant` — body `{ "message": "..." }`. Returns general explanation only; the assistant never calculates tax amounts or invents rules.

## Utility

- `GET /api/health`

The exact contract can evolve as the product domain grows, but the API remains the source of truth for the frontend.
