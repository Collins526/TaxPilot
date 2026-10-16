import {
  BooksReconciliation,
  DocumentRecord,
  FilingPeriod,
  PreKraCheck
} from './taxpilot-api.service';
import { buildCompliancePackCsv } from './compliance-pack-export';

describe('buildCompliancePackCsv', () => {
  const period: FilingPeriod = {
    id: 'period-1',
    periodStart: '2026-01-01',
    periodEnd: '2026-01-31',
    taxableSales: 1000,
    inputVat: 80,
    payrollGross: null,
    withholdingBase: null,
    taxableProfit: 200,
    etimsDeclaredSales: 1000,
    notes: null,
    createdAt: '2026-02-01T10:00:00Z',
    totalEstimatedTax: 240,
    calculations: [{
      taxType: 'VAT',
      ruleCode: 'VAT_STANDARD',
      ruleVersion: 'KE-2026.1',
      rate: 0.16,
      amount: 80,
      basis: 'Output VAT 160.00 minus input VAT 80.00'
    }]
  };

  it('includes inputs, calculations, matching checks, and supporting document links', () => {
    const reconciliation: BooksReconciliation = {
      filingPeriodId: 'period-1',
      periodStart: period.periodStart,
      periodEnd: period.periodEnd,
      fileName: 'books.csv',
      transactionCount: 2,
      salesCount: 1,
      purchaseCount: 1,
      bookSales: 1000,
      purchaseInputVat: 80,
      passed: true,
      checks: [{
        code: 'BOOKS_VS_TAXABLE_SALES',
        booksAmount: 1000,
        recordedAmount: 1000,
        difference: 0,
        status: 'MATCH',
        message: 'Sales match'
      }]
    };
    const preKra: PreKraCheck = {
      businessId: 'business-1',
      filingPeriodId: 'period-1',
      periodEnd: period.periodEnd,
      passed: false,
      findingsCount: 1,
      findings: [{ severity: 'WARNING', taxType: 'VAT', message: 'Review VAT figures' }]
    };
    const documents: DocumentRecord[] = [{
      id: 'doc-1',
      fileName: 'vat-return.pdf',
      contentType: 'application/pdf',
      sizeBytes: 1024,
      category: 'VAT',
      storageProvider: 'cloudinary',
      url: 'https://example.test/vat-return.pdf',
      uploadedAt: '2026-02-01T12:00:00Z'
    }];

    const csv = buildCompliancePackCsv('Njeri Traders', period, documents, reconciliation, preKra);

    expect(csv.charCodeAt(0)).toBe(0xFEFF);
    expect(csv).toContain('"Taxable sales","1000","KES"');
    expect(csv).toContain('"VAT","80","16%","Output VAT 160.00 minus input VAT 80.00","VAT_STANDARD","KE-2026.1"');
    expect(csv).toContain('"BOOKS_VS_TAXABLE_SALES","1000","1000","0","MATCH","Sales match"');
    expect(csv).toContain('"WARNING","VAT","Review VAT figures"');
    expect(csv).toContain('"vat-return.pdf","VAT","2026-02-01T12:00:00Z","https://example.test/vat-return.pdf"');
  });

  it('marks checks unavailable when no result for the selected period exists', () => {
    const csv = buildCompliancePackCsv('Njeri Traders', period, [], null, null);
    expect(csv).toContain('No in-session reconciliation result is available for this filing period.');
    expect(csv).toContain('No matching Pre-KRA check result is available.');
  });

  it('escapes CSV delimiters and neutralizes spreadsheet formulas in user-controlled text', () => {
    const unsafeDocument: DocumentRecord = {
      id: 'doc-2',
      fileName: '=IMPORTXML("https://example.test","//x")',
      contentType: 'text/csv',
      sizeBytes: 1,
      category: 'VAT, returns',
      storageProvider: 'cloudinary',
      url: 'https://example.test/file.csv',
      uploadedAt: '2026-02-01T12:00:00Z'
    };
    const csv = buildCompliancePackCsv('=SUM(1,1)', period, [unsafeDocument], null, null);

    expect(csv).toContain("\"'=SUM(1,1)\"");
    expect(csv).toContain("\"'=IMPORTXML(\"\"https://example.test\"\",\"\"//x\"\")\"");
    expect(csv).toContain('"VAT, returns"');
  });
});
