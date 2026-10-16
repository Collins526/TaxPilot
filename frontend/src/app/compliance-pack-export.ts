import {
  BooksReconciliation,
  FilingPeriod,
  PreKraCheck,
  DocumentRecord
} from './taxpilot-api.service';

type CsvValue = string | number | null;

export function buildCompliancePackCsv(
  businessName: string,
  period: FilingPeriod,
  documents: DocumentRecord[],
  reconciliation: BooksReconciliation | null,
  preKra: PreKraCheck | null
): string {
  const rows: CsvValue[][] = [
    ['TaxPilot compliance pack'],
    ['Business', businessName],
    ['Period start', period.periodStart],
    ['Period end', period.periodEnd],
    ['Generated at (UTC)', new Date().toISOString()],
    ['Estimate disclaimer', 'Informational estimate only; not a final assessment or payment instruction.'],
    [],
    ['Filing inputs'],
    ['Field', 'Value', 'Unit']
  ];

  const inputs: Array<[string, number | null]> = [
    ['Taxable sales', period.taxableSales],
    ['Input VAT', period.inputVat],
    ['Gross payroll', period.payrollGross],
    ['Withholding base', period.withholdingBase],
    ['Taxable profit', period.taxableProfit],
    ['eTIMS declared sales', period.etimsDeclaredSales]
  ];
  for (const [label, value] of inputs) {
    rows.push([label, value, 'KES']);
  }
  rows.push(['Total estimated tax', period.totalEstimatedTax, 'KES']);

  rows.push([], ['Calculations'], ['Tax type', 'Amount (KES)', 'Rate', 'Basis', 'Rule code', 'Rule version']);
  for (const calculation of period.calculations) {
    rows.push([
      calculation.taxType,
      calculation.amount,
      calculation.rate === null ? 'Progressive tax bands' : `${calculation.rate * 100}%`,
      calculation.basis,
      calculation.ruleCode,
      calculation.ruleVersion
    ]);
  }
  if (period.calculations.length === 0) {
    rows.push(['No calculation available for this filing period']);
  }

  rows.push([], ['Books reconciliation']);
  if (reconciliation?.filingPeriodId === period.id) {
    rows.push(
      ['CSV file', reconciliation.fileName],
      ['Transactions', reconciliation.transactionCount],
      ['Sales transactions', reconciliation.salesCount],
      ['Purchase transactions', reconciliation.purchaseCount],
      ['Overall result', reconciliation.passed ? 'No mismatches in available comparisons' : 'Review mismatches'],
      ['Check', 'Books amount (KES)', 'Recorded amount (KES)', 'Difference (KES)', 'Status', 'Finding']
    );
    for (const check of reconciliation.checks) {
      rows.push([
        check.code,
        check.booksAmount,
        check.recordedAmount,
        check.difference,
        check.status,
        check.message
      ]);
    }
  } else {
    rows.push(['No in-session reconciliation result is available for this filing period.']);
  }

  rows.push([], ['Pre-KRA consistency check']);
  if (preKra?.filingPeriodId === period.id) {
    rows.push(
      ['Overall result', preKra.passed ? 'No findings' : `${preKra.findingsCount} finding(s)`],
      ['Severity', 'Tax type', 'Finding']
    );
    for (const finding of preKra.findings) {
      rows.push([finding.severity, finding.taxType, finding.message]);
    }
    if (preKra.findings.length === 0) {
      rows.push(['No findings']);
    }
  } else {
    rows.push(['No matching Pre-KRA check result is available.']);
  }

  rows.push(
    [],
    ['Supporting documents (business-wide; not linked to a specific filing period)'],
    ['File name', 'Category', 'Uploaded at (UTC)', 'Document URL']
  );
  if (documents.length === 0) {
    rows.push(['No supporting documents uploaded.']);
  } else {
    for (const document of documents) {
      rows.push([
        document.fileName,
        document.category ?? '',
        document.uploadedAt,
        document.url
      ]);
    }
  }
  rows.push(
    [],
    ['Important'],
    ['TaxPilot does not submit returns or payments to KRA. Verify the recorded information and applicable rules before filing.']
  );

  return '\uFEFF' + rows.map((row) => row.map(csvCell).join(',')).join('\r\n');
}

function csvCell(value: CsvValue): string {
  if (value === null || value === undefined) {
    return '';
  }
  let text = String(value);
  if (typeof value === 'string' && /^[\u0000-\u0020]*[=+\-@]/.test(text)) {
    text = `'${text}`;
  }
  return `"${text.replace(/"/g, '""')}"`;
}
