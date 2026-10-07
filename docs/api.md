# API Overview

The backend exposes a REST API for authentication, business onboarding, and tax summaries.

## Planned endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /api/businesses`
- `POST /api/businesses`
- `GET /api/businesses/{id}`
- `GET /api/businesses/{id}/tax-summary`

The exact contract can evolve as the product domain grows, but the API remains the source of truth for the frontend.
