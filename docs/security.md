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
- On startup, create one system `ADMIN` only when neither an `ADMIN` nor `SUPER_ADMIN` account exists. The system generates a unique login email and high-entropy password; only the BCrypt hash is stored, while the generated credentials are printed once in startup logs. Anyone with log access can read those initial credentials, so operators must protect logs and store the credentials securely. Existing accounts are never modified or promoted, and later startups do not reveal the password again.
