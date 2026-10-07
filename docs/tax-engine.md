# Tax Engine

Tax calculations must remain deterministic and auditable.

## Rules

- VAT is computed as a fixed percentage of taxable sales.
- Income tax is calculated from taxable income after relief.
- Compliance health depends on outstanding tax liabilities against paid amounts.

## Constraints

- Tax logic must live in the backend service layer.
- No direct LLM-driven tax calculation is allowed.
- Engine rules should be unit-tested and reviewed before release.
