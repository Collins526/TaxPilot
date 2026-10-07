# Architecture

This repository follows a layered monorepo structure to keep product, platform, and operational concerns independent.

## Components

- `backend/`: domain and application services for tax compliance, authorization, and business onboarding.
- `frontend/`: Angular client for users and compliance dashboards.
- `infrastructure/`: deployment assets, Docker, and reverse proxy configuration.
- `docs/`: design and product reference material.

## Principles

- Deterministic tax logic lives in the backend rules engine.
- LLMs are assistant-only for explanation and suggestions; they do not drive tax calculations.
- The API is authoritative for all business and compliance data flows.
