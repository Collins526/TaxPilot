import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AssistantAnswer,
  BooksReconciliation,
  DocumentRecord,
  FilingPeriod,
  FilingPeriodInput,
  PreKraCheck,
  TaxPilotApiService
} from './taxpilot-api.service';
import { buildCompliancePackCsv } from './compliance-pack-export';

@Component({
  selector: 'app-tax-tools',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6">
      @if (errorMessage) {
        <p class="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{{ errorMessage }}</p>
      }
      @if (loading) {
        <p class="text-sm text-slate-500">Loading…</p>
      }

      <!-- Filing periods -->
      <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <header class="mb-4 flex items-center justify-between">
          <div>
            <h2 class="text-lg font-semibold text-slate-900">Filing periods &amp; tax calculation</h2>
            <p class="text-sm text-slate-500">Record a period's figures. Expand a calculation to review its basis, rate, and rule identifiers.</p>
          </div>
          <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">{{ filingPeriods.length }} recorded</span>
        </header>

        <form class="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-4" (submit)="createPeriod($event)">
          <label class="text-sm text-slate-600">Period start
            <input name="periodStart" type="date" required class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <label class="text-sm text-slate-600">Period end
            <input name="periodEnd" type="date" required class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <label class="text-sm text-slate-600">Taxable sales (KES)
            <input name="taxableSales" type="number" min="0" step="0.01" class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <label class="text-sm text-slate-600">Input VAT (KES)
            <input name="inputVat" type="number" min="0" step="0.01" class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <label class="text-sm text-slate-600">Gross payroll (KES)
            <input name="payrollGross" type="number" min="0" step="0.01" class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <label class="text-sm text-slate-600">Withholding base (KES)
            <input name="withholdingBase" type="number" min="0" step="0.01" class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <label class="text-sm text-slate-600">Taxable profit (KES)
            <input name="taxableProfit" type="number" min="0" step="0.01" class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <label class="text-sm text-slate-600">eTIMS declared sales (KES)
            <input name="etimsDeclaredSales" type="number" min="0" step="0.01" class="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          </label>
          <div class="md:col-span-2 lg:col-span-4">
            <button type="submit" [disabled]="busy" class="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60">
              {{ busy ? 'Saving…' : 'Record period & calculate' }}
            </button>
          </div>
        </form>

        <div class="mt-6 space-y-3">
          @for (period of filingPeriods; track period.id) {
            <article class="rounded-lg border border-slate-200 p-4">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <strong class="text-sm text-slate-800">{{ period.periodStart }} → {{ period.periodEnd }}</strong>
                <div class="flex items-center gap-3">
                  <span class="text-sm font-semibold text-brand-700">
                    {{ period.totalEstimatedTax === null ? 'No calculation available' : (period.totalEstimatedTax | number:'1.0-0') + ' KES' }}
                  </span>
                  <button type="button" (click)="downloadCompliancePack(period)"
                          class="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50">
                    Download compliance pack
                  </button>
                </div>
              </div>
              @if (period.totalEstimatedTax !== null) {
                <p class="mt-1 text-xs text-slate-500">Estimate based on the recorded inputs and rule versions shown below. Not a final assessment or payment instruction.</p>
              }
              @if (period.calculations.length > 0) {
                <ul class="mt-3 divide-y divide-slate-100">
                  @for (calc of period.calculations; track calc.taxType) {
                    <li class="py-2">
                      <details class="group">
                        <summary class="flex cursor-pointer list-none items-center justify-between gap-3 text-sm">
                          <span class="text-slate-600">
                            <strong class="text-slate-800">{{ calc.taxType }}</strong>
                            <span class="ml-2 text-xs text-slate-400">{{ calc.ruleVersion }}</span>
                          </span>
                          <span class="flex items-center gap-2 text-slate-700">
                            {{ calc.amount | number:'1.2-2' }} KES
                            <span class="text-xs text-brand-600 group-open:rotate-90" aria-hidden="true">›</span>
                          </span>
                        </summary>
                        <dl class="mt-3 grid gap-2 rounded-md bg-slate-50 p-3 text-xs sm:grid-cols-2">
                          <div>
                            <dt class="font-semibold text-slate-600">Calculation basis</dt>
                            <dd class="mt-1 text-slate-700">{{ calc.basis }}</dd>
                          </div>
                          <div>
                            <dt class="font-semibold text-slate-600">Rate or method</dt>
                            <dd class="mt-1 text-slate-700">
                              {{ calc.rate === null ? 'Progressive tax bands' : ((calc.rate * 100 | number:'1.0-4') + '%') }}
                            </dd>
                          </div>
                          <div>
                            <dt class="font-semibold text-slate-600">Rule code</dt>
                            <dd class="mt-1 text-slate-700">{{ calc.ruleCode }}</dd>
                          </div>
                          <div>
                            <dt class="font-semibold text-slate-600">Rule version</dt>
                            <dd class="mt-1 text-slate-700">{{ calc.ruleVersion }}</dd>
                          </div>
                        </dl>
                      </details>
                    </li>
                  }
                </ul>
              } @else {
                <p class="mt-2 text-xs text-slate-400">No obligations produced a calculation for this period.</p>
              }
            </article>
          } @empty {
            <p class="text-sm text-slate-400">No filing periods recorded yet.</p>
          }
        </div>
      </section>

      <!-- Books-to-tax reconciliation -->
      <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <header class="mb-4">
          <h2 class="text-lg font-semibold text-slate-900">Books-to-tax reconciliation</h2>
          <p class="text-sm text-slate-500">Compare sales and purchase VAT from a CSV against a recorded filing period. Uploaded rows are checked in memory and are not saved or used to change tax calculations.</p>
        </header>
        <form class="flex flex-wrap items-end gap-3" (submit)="reconcileBooks($event)">
          <label class="text-sm text-slate-600">Filing period
            <select name="reconciliationPeriod" [(ngModel)]="reconciliationPeriodId" required
                    (ngModelChange)="reconciliation = null"
                    class="mt-1 block min-w-56 rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900">
              <option value="" disabled>Select a period</option>
              @for (period of filingPeriods; track period.id) {
                <option [value]="period.id">{{ period.periodStart }} → {{ period.periodEnd }}</option>
              }
            </select>
          </label>
          <label class="text-sm text-slate-600">Transactions CSV
            <input name="reconciliationFile" type="file" accept=".csv,text/csv" required
                   (change)="reconciliation = null"
                   class="mt-1 block text-sm text-slate-700">
          </label>
          <button type="submit" [disabled]="busy || filingPeriods.length === 0"
                  class="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60">
            {{ busy ? 'Checking…' : 'Run reconciliation' }}
          </button>
        </form>
        <p class="mt-3 text-xs text-slate-500">
          CSV headers: <code>transaction_date,transaction_type,amount,vat_amount</code>.
          Use YYYY-MM-DD dates, SALE or PURCHASE, KES amounts without thousands separators, and values in the selected period.
          Sales amounts should be exclusive of VAT; purchase VAT is compared with recorded input VAT.
        </p>
        @if (reconciliation) {
          <div class="mt-5 rounded-lg border p-4"
               [class]="reconciliation.passed ? 'border-emerald-200 bg-emerald-50' : 'border-amber-200 bg-amber-50'">
            <p class="font-semibold text-slate-800">
              {{ reconciliation.passed ? 'No mismatches found in available comparisons' : 'Review the differences below' }}
            </p>
            <p class="mt-1 text-xs text-slate-600">
              {{ reconciliation.fileName }} · {{ reconciliation.transactionCount }} transactions
              ({{ reconciliation.salesCount }} sales, {{ reconciliation.purchaseCount }} purchases)
            </p>
            <ul class="mt-3 divide-y divide-slate-200">
              @for (check of reconciliation.checks; track check.code) {
                <li class="flex flex-wrap items-start justify-between gap-2 py-2 text-sm">
                  <span class="text-slate-700">{{ check.message }}</span>
                  <strong class="text-xs" [class]="reconciliationClass(check.status)">{{ check.status.replace('_', ' ') }}</strong>
                </li>
              }
            </ul>
            <p class="mt-3 text-xs text-slate-500">This is a consistency check, not tax advice or a KRA assessment. Verify source records before filing.</p>
          </div>
        }
      </section>

      <!-- Pre-KRA check -->
      <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <header class="mb-4 flex items-center justify-between">
          <div>
            <h2 class="text-lg font-semibold text-slate-900">Pre-KRA check</h2>
            <p class="text-sm text-slate-500">Runs consistency checks against your latest filing period.</p>
          </div>
          <button type="button" [disabled]="busy" (click)="runPreKra()" class="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60">
            {{ busy ? 'Checking…' : (preKra ? 'Re-run check' : 'Run check') }}
          </button>
        </header>
        @if (preKra) {
          <p class="mb-3 inline-flex rounded-full px-3 py-1 text-xs font-medium"
             [class]="preKra.passed ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'">
            {{ preKra.passed ? 'No findings' : preKra.findingsCount + ' finding(s)' }} · period ending {{ preKra.periodEnd }}
          </p>
          <ul class="space-y-2">
            @for (finding of preKra.findings; track $index) {
              <li class="flex gap-3 rounded-lg border border-slate-200 p-3 text-sm">
                <span class="rounded px-2 py-0.5 text-xs font-semibold"
                      [class]="severityClass(finding.severity)">{{ finding.severity }}</span>
                <span class="text-slate-600"><strong class="text-slate-800">{{ finding.taxType }}</strong> — {{ finding.message }}</span>
              </li>
            }
          </ul>
        }
      </section>

      <!-- Documents -->
      <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <header class="mb-4">
          <h2 class="text-lg font-semibold text-slate-900">Documents</h2>
          <p class="text-sm text-slate-500">PDF, PNG, JPEG, WEBP, CSV or XLSX up to 10 MB. Stored via Cloudinary.</p>
        </header>
        <form class="flex flex-wrap items-end gap-3" (submit)="uploadDocument($event)">
          <label class="text-sm text-slate-600">File
            <input name="file" type="file" required class="mt-1 block text-sm text-slate-700">
          </label>
          <label class="text-sm text-slate-600">Category
            <input name="category" type="text" placeholder="e.g. VAT returns" class="mt-1 block rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900">
          </label>
          <button type="submit" [disabled]="busy" class="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60">
            {{ busy ? 'Uploading…' : 'Upload' }}
          </button>
        </form>
        <ul class="mt-4 divide-y divide-slate-100">
          @for (document of documents; track document.id) {
            <li class="flex items-center justify-between py-2 text-sm">
              <span class="text-slate-700">{{ document.fileName }}
                <span class="ml-2 text-xs text-slate-400">{{ document.category || 'uncategorised' }}</span>
              </span>
              <a class="text-brand-600 hover:underline" [href]="document.url" target="_blank" rel="noopener">Open</a>
            </li>
          } @empty {
            <li class="py-2 text-sm text-slate-400">No documents uploaded yet.</li>
          }
        </ul>
      </section>

      <!-- Assistant -->
      <section class="rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <header class="mb-4">
          <h2 class="text-lg font-semibold text-slate-900">Ask TaxPilot</h2>
          <p class="text-sm text-slate-500">General guidance only. The assistant explains obligations; it never calculates amounts.</p>
        </header>
        <form class="flex flex-col gap-3 sm:flex-row" (submit)="ask($event)">
          <input [(ngModel)]="question" name="question" placeholder="e.g. When is VAT due for a registered business?"
                 class="flex-1 rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-brand-500 focus:outline-none">
          <button type="submit" [disabled]="busy" class="rounded-md bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60">
            {{ busy ? 'Thinking…' : 'Ask' }}
          </button>
        </form>
        @if (answer) {
          <div class="mt-4 rounded-lg border border-slate-200 bg-slate-50 p-4">
            <p class="whitespace-pre-line text-sm text-slate-800">{{ answer.answer }}</p>
            <p class="mt-3 border-t border-slate-200 pt-3 text-xs text-slate-500">{{ answer.disclaimer }}</p>
          </div>
        }
      </section>
    </div>
  `
})
export class TaxToolsComponent implements OnChanges {
  @Input() businessId = '';
  @Input() businessName = 'Business';

  filingPeriods: FilingPeriod[] = [];
  reconciliationPeriodId = '';
  reconciliation: BooksReconciliation | null = null;
  preKra: PreKraCheck | null = null;
  documents: DocumentRecord[] = [];
  answer: AssistantAnswer | null = null;
  question = '';
  busy = false;
  loading = false;
  errorMessage = '';

  constructor(private readonly api: TaxPilotApiService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['businessId']) {
      void this.refresh();
    }
  }

  async refresh(): Promise<void> {
    if (!this.businessId) {
      this.filingPeriods = [];
      this.reconciliationPeriodId = '';
      this.reconciliation = null;
      this.documents = [];
      this.preKra = null;
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.reconciliation = null;
    try {
      const [periods, documents] = await Promise.all([
        this.api.listFilingPeriods(this.businessId),
        this.api.listDocuments(this.businessId)
      ]);
      this.filingPeriods = periods;
      if (!periods.some((period) => period.id === this.reconciliationPeriodId)) {
        this.reconciliationPeriodId = periods[0]?.id ?? '';
      }
      this.documents = documents;
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.loading = false;
    }
  }

  async createPeriod(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const form = event.currentTarget as HTMLFormElement;
    const values = new FormData(form);
    const payload: FilingPeriodInput = {
      periodStart: String(values.get('periodStart') ?? ''),
      periodEnd: String(values.get('periodEnd') ?? ''),
      taxableSales: this.number(values.get('taxableSales')),
      inputVat: this.number(values.get('inputVat')),
      payrollGross: this.number(values.get('payrollGross')),
      withholdingBase: this.number(values.get('withholdingBase')),
      taxableProfit: this.number(values.get('taxableProfit')),
      etimsDeclaredSales: this.number(values.get('etimsDeclaredSales')),
      notes: null
    };
    await this.run(async () => {
      const created = await this.api.createFilingPeriod(this.businessId, payload);
      form.reset();
      this.filingPeriods = await this.api.listFilingPeriods(this.businessId);
      this.reconciliationPeriodId = created.id;
      this.reconciliation = null;
    });
  }

  async reconcileBooks(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const values = new FormData(event.currentTarget as HTMLFormElement);
    const file = values.get('reconciliationFile');
    if (!(file instanceof File) || file.size === 0) {
      this.errorMessage = 'Choose a CSV file to reconcile.';
      return;
    }
    if (!this.reconciliationPeriodId) {
      this.errorMessage = 'Select a filing period first.';
      return;
    }
    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.errorMessage = 'Upload a .csv file.';
      return;
    }
    await this.run(async () => {
      this.reconciliation = await this.api.reconcileBooks(this.businessId, this.reconciliationPeriodId, file);
    });
  }

  async runPreKra(): Promise<void> {
    await this.run(async () => {
      this.preKra = await this.api.getPreKraCheck(this.businessId);
    });
  }

  async uploadDocument(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const form = event.currentTarget as HTMLFormElement;
    const values = new FormData(form);
    const file = values.get('file');
    if (!(file instanceof File) || file.size === 0) {
      this.errorMessage = 'Choose a file to upload.';
      return;
    }
    const category = String(values.get('category') ?? '').trim();
    await this.run(async () => {
      await this.api.uploadDocument(this.businessId, file, category);
      form.reset();
      this.documents = await this.api.listDocuments(this.businessId);
    });
  }

  async ask(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    if (!this.question.trim()) {
      return;
    }
    await this.run(async () => {
      this.answer = await this.api.askAssistant(this.businessId, this.question.trim());
    });
  }

  severityClass(severity: string): string {
    switch (severity) {
      case 'HIGH':
        return 'bg-red-50 text-red-700';
      case 'WARNING':
        return 'bg-amber-50 text-amber-700';
      default:
        return 'bg-slate-100 text-slate-600';
    }
  }

  reconciliationClass(status: string): string {
    switch (status) {
      case 'MATCH':
        return 'text-emerald-700';
      case 'MISMATCH':
        return 'text-amber-700';
      default:
        return 'text-slate-500';
    }
  }

  downloadCompliancePack(period: FilingPeriod): void {
    const matchingReconciliation = this.reconciliation?.filingPeriodId === period.id
      ? this.reconciliation
      : null;
    const matchingPreKra = this.preKra?.filingPeriodId === period.id ? this.preKra : null;
    const csv = buildCompliancePackCsv(
      this.businessName,
      period,
      this.documents,
      matchingReconciliation,
      matchingPreKra
    );
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const business = this.businessName.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'business';
    link.href = url;
    link.download = `taxpilot-${business}-${period.periodStart}-to-${period.periodEnd}-compliance-pack.csv`;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy = true;
    this.errorMessage = '';
    try {
      await action();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  private number(value: FormDataEntryValue | null): number | null {
    const text = String(value ?? '').trim();
    if (text === '') {
      return null;
    }
    const parsed = Number(text);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private errorText(error: unknown): string {
    return error instanceof Error ? error.message : 'The request could not be completed.';
  }
}
