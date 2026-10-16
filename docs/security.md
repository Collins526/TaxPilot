# Security

The backend uses JWT-based authentication and role-scoped access for business-related operations.

## Baseline controls

- Password hashing with BCrypt.
- Short-lived access tokens and refresh tokens, distinguished by a `type` claim.
- Redis-backed revocation list so logout invalidates an access/refresh token until it expires.
- Protected business endpoints tied to user membership.
- Validation on request payloads and tax calculations.
- Production profile for validated PostgreSQL configuration.
- Audit logging of authentication events and business/tax-profile mutations, scoped per business.
- On startup, create one system `ADMIN` only when neither an `ADMIN` nor `SUPER_ADMIN` account exists. Seed credentials are supplied through `BOOTSTRAP_ADMIN_*` environment variables, the password is BCrypt-hashed, and startup fails clearly if no admin exists and required values are missing or invalid. Existing accounts are never modified or promoted.
