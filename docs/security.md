# Security

The backend uses JWT-based authentication and role-scoped access for business-related operations.

## Baseline controls

- Password hashing with BCrypt.
- Short-lived access tokens and refresh tokens.
- Protected business endpoints tied to user membership.
- Validation on request payloads and tax calculations.
- Production profile for validated PostgreSQL configuration.
