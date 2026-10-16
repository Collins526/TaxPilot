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
- Each stored calculation exposes its basis, rate when applicable, rule code, and rule version so users can inspect the recorded calculation.
- Filing period calculations use the effective rules for the period end date; users should verify the configured rule version and inputs before relying on an estimate.
- A displayed estimate is informational and is not a final assessment or payment instruction.
- Users can export a CSV compliance pack with the period inputs, calculations, available matching check findings, and business document links. Checks not available for that period are identified as such; document links are business-wide and not asserted to belong to that filing period.
