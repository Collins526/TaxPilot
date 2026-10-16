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
- `PUT /api/businesses/{businessId}/filing-status` — body `{ "taxType": "VAT", "dueDate": "2026-11-20", "filingStatus": "IN_PROGRESS" }`; supported statuses are `NOT_STARTED`, `IN_PROGRESS`, `FILED`, and `PAID`. Status is stored per business, tax type, and due date.
- `GET /api/businesses/{businessId}/members` — list active business members (owner only).
- `POST /api/businesses/{businessId}/members` — add an existing TaxPilot user by email; body `{ "email": "accountant@example.com", "role": "ACCOUNTANT" }`. Supported roles: `ACCOUNTANT`, `TAX_ADVISOR`, `EMPLOYEE`. Access is immediate; no email is sent. Owner only.
- `DELETE /api/businesses/{businessId}/members/{memberUserId}` — revoke a member's access (owner only). Business owner membership cannot be removed.
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
- `POST /api/businesses/{businessId}/filing-periods/{periodId}/reconcile` — multipart CSV upload using the `file` field. Required headers: `transaction_date`, `transaction_type`, `amount`, `vat_amount`. Dates use `YYYY-MM-DD`; transaction type is `SALE` or `PURCHASE`; amounts are non-negative KES values with up to two decimals and no thousands separators. Rows must fall within the filing period. Maximum file size is 10 MB and maximum row count is 20,000. Compares book sales to recorded taxable/eTIMS sales and purchase VAT to input VAT, within KES 0.01.

Example CSV:

```csv
transaction_date,transaction_type,amount,vat_amount
2026-01-05,SALE,1000.00,160.00
2026-01-06,PURCHASE,500.00,80.00
```

Filing-period calculation responses include each calculation's `basis`, `rate` (null
for progressive-band calculations), `ruleCode`, and `ruleVersion`. The frontend
displays these fields when a calculation is expanded. Estimates use the versioned
rules effective on the filing period end date and are not final assessments or
payment instructions.

The frontend can download a CSV compliance pack for a filing period. It includes
recorded inputs, calculation breakdowns and rule identifiers, any in-session
reconciliation or Pre-KRA results for that period, and business-wide supporting
document links. Reconciliation and Pre-KRA results are not persisted; the export
marks results unavailable when they are not present in the current session.

## Documents

- `POST /api/businesses/{businessId}/documents` — `multipart/form-data` with `file` (required) and `category` (optional). Allowed types: PDF, PNG, JPEG, WEBP, CSV, XLSX; max 10 MB. Stored via Cloudinary.
- `GET /api/businesses/{businessId}/documents`

## Assistant

- `POST /api/businesses/{businessId}/assistant` — body `{ "message": "..." }`. Returns general explanation only; the assistant never calculates tax amounts or invents rules.

## Utility

- `GET /api/health`

Upcoming deadlines within seven days and overdue deadlines that are not filed or paid
appear as in-app reminders on the dashboard. This release does not send email or SMS.
Updating a filing status is a tracking action only; it does not submit a return or
payment to KRA. Changes are included in the business audit log.

The exact contract can evolve as the product domain grows, but the API remains the source of truth for the frontend.

Collaborators can manage tax profiles, filing periods, filing statuses, documents,
and reconciliation for businesses where they have active membership. Only the
business owner can edit business details or manage members.

Reconciliation reads CSV rows in memory and does not store transaction-level data or
change the filing period or tax calculation. Checks without a recorded filing value
are returned as `NOT_AVAILABLE`; a `passed` result means no available comparison
found a mismatch. Reconciliation results are not tax advice or a KRA assessment.
