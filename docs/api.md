# API Overview

The backend exposes a REST API for authentication, business onboarding, and tax summaries.

## Planned endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/businesses`
- `POST /api/businesses`
- `GET /api/businesses/{id}`
- `GET /api/businesses/{id}/obligations`
- `GET /api/businesses/{id}/calendar`
- `GET /api/businesses/{id}/tax-health`
- `GET /api/businesses/{id}/dashboard`
- `GET /api/businesses/{id}/audit-logs?limit=50`

The exact contract can evolve as the product domain grows, but the API remains the source of truth for the frontend.
