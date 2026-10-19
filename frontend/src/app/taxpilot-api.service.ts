import { Injectable } from '@angular/core';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];
}

export interface AdminOverview {
  users: number;
  enabledUsers: number;
  admins: number;
  businesses: number;
  filingPeriods: number;
}

export interface AdminFilingPeriod {
  id: string;
  businessId: string;
  businessName: string;
  periodStart: string;
  periodEnd: string;
  createdAt: string;
}

export interface AdminUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];
  enabled: boolean;
  businessCount: number;
}

export interface AdminMembership {
  userId: string;
  name: string;
  email: string;
  role: string;
  active: boolean;
}

export interface AdminBusiness {
  id: string;
  name: string;
  legalName: string;
  registrationNumber: string;
  kraPin: string;
  industry: string;
  businessType: string;
  annualTurnover: number;
  employeeCount: number;
  vatRegistered: boolean;
  etimsRegistered: boolean;
  countryCode: string;
  timezone: string;
  members: AdminMembership[];
}

export interface AdminAuditEntry {
  id: string;
  occurredAt: string;
  actorEmail: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  businessId: string | null;
  outcome: string;
  details: string | null;
}

export interface AdminTaxBand {
  bandOrder: number;
  lowerBound: number;
  upperBound: number | null;
  rate: number;
}

export interface AdminTaxRule {
  id: string;
  ruleCode: string;
  ruleVersion: string;
  frequency: string;
  dueDay: number | null;
  effectiveFrom: string;
  effectiveTo: string | null;
  rate: number | null;
  threshold: number | null;
  source: string;
  bands: AdminTaxBand[];
}

export interface AdminTaxRuleInput extends Omit<AdminTaxRule, 'id'> {}

export interface Business {
  id: string;
  name: string;
  legalName: string;
  registrationNumber: string;
  kraPin: string;
  industry: string;
  businessType: string;
  annualTurnover: number;
  employeeCount: number;
  vatRegistered: boolean;
  etimsRegistered: boolean;
  countryCode: string;
  timezone: string;
  memberships: string[];
}

export type BusinessMemberRole = 'ACCOUNTANT' | 'TAX_ADVISOR' | 'EMPLOYEE';

export interface BusinessMember {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
}

export interface TaxProfile {
  businessType: string;
  annualTurnover: number;
  monthlyTurnover: number;
  employeeCount: number;
  vatRegistered: boolean;
  vatRegistrationDate: string | null;
  etimsRegistered: boolean;
  hasEmployees: boolean;
  makesWithholdingPayments: boolean;
  receivesRentalIncome: boolean;
  importsGoods: boolean;
  exportsGoods: boolean;
  hasForeignTransactions: boolean;
  hasDigitalServices: boolean;
  hasExciseActivities: boolean;
  incomeTaxApplicable: boolean;
  corporationTaxApplicable: boolean;
  turnoverTaxApplicable: boolean;
}

export interface TaxObligation {
  taxType: string;
  applicable: boolean;
  reason: string;
  frequency: string;
  nextDueDate: string | null;
  estimatedAmount: number | null;
  ruleVersion: string | null;
}

export interface TaxCalendarEntry {
  taxType: string;
  amount: number | null;
  dueDate: string;
  status: string;
  daysRemaining: number;
  filingStatus: FilingStatus;
}

export type FilingStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'FILED' | 'PAID';

export interface DashboardSummary {
  businessName: string;
  taxHealthScore: number;
  taxHealthStatus: string;
  nextDeadlineLabel: string;
  estimatedTax: number | null;
  issuesFound: number;
  complianceIssues: string[];
  obligations: string[];
  upcomingDeadlines: TaxCalendarEntry[];
}

export interface TaxCalculation {
  taxType: string;
  ruleCode: string;
  ruleVersion: string;
  rate: number | null;
  amount: number;
  basis: string;
}

export interface FilingPeriod {
  id: string;
  periodStart: string;
  periodEnd: string;
  taxableSales: number | null;
  inputVat: number | null;
  payrollGross: number | null;
  withholdingBase: number | null;
  taxableProfit: number | null;
  etimsDeclaredSales: number | null;
  notes: string | null;
  createdAt: string;
  totalEstimatedTax: number | null;
  calculations: TaxCalculation[];
}

export interface FilingPeriodInput {
  periodStart: string;
  periodEnd: string;
  taxableSales: number | null;
  inputVat: number | null;
  payrollGross: number | null;
  withholdingBase: number | null;
  taxableProfit: number | null;
  etimsDeclaredSales: number | null;
  notes: string | null;
}

export interface PreKraFinding {
  severity: string;
  taxType: string;
  message: string;
}

export interface PreKraCheck {
  businessId: string;
  filingPeriodId: string;
  periodEnd: string;
  passed: boolean;
  findingsCount: number;
  findings: PreKraFinding[];
}

export interface BooksReconciliationCheck {
  code: string;
  booksAmount: number;
  recordedAmount: number | null;
  difference: number | null;
  status: 'MATCH' | 'MISMATCH' | 'NOT_AVAILABLE';
  message: string;
}

export interface BooksReconciliation {
  filingPeriodId: string;
  periodStart: string;
  periodEnd: string;
  fileName: string;
  transactionCount: number;
  salesCount: number;
  purchaseCount: number;
  bookSales: number;
  purchaseInputVat: number;
  passed: boolean;
  checks: BooksReconciliationCheck[];
}

export interface DocumentRecord {
  id: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  category: string | null;
  storageProvider: string;
  url: string;
  uploadedAt: string;
}

export interface AssistantAnswer {
  answer: string;
  disclaimer: string;
  model: string;
}

@Injectable({ providedIn: 'root' })
export class TaxPilotApiService {
  private readonly apiBaseUrl = 'http://localhost:8080';
  private readonly tokenKey = 'taxpilot.access-token';
  private readonly userKey = 'taxpilot.user';

  get hasSession(): boolean {
    return Boolean(localStorage.getItem(this.tokenKey));
  }

  saveSession(response: AuthResponse): void {
    localStorage.setItem(this.tokenKey, response.accessToken);
    localStorage.setItem(this.userKey, JSON.stringify(response));
  }

  getStoredUser(): AuthResponse | null {
    const stored = localStorage.getItem(this.userKey);
    if (!stored) {
      return null;
    }
    try {
      return JSON.parse(stored) as AuthResponse;
    } catch {
      this.clearSession();
      return null;
    }
  }

  clearSession(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }

  login(email: string, password: string): Promise<AuthResponse> {
    return this.request('/api/auth/login', { method: 'POST', body: { email, password } });
  }

  register(payload: {
    firstName: string;
    lastName: string;
    email: string;
    phone: string;
    password: string;
  }): Promise<AuthResponse> {
    return this.request('/api/auth/register', { method: 'POST', body: payload });
  }

  logout(refreshToken: string | null): Promise<void> {
    return this.request('/api/auth/logout', {
      method: 'POST',
      body: refreshToken ? { refreshToken } : undefined
    });
  }

  listBusinesses(): Promise<Business[]> {
    return this.request('/api/businesses');
  }

  createBusiness(payload: Record<string, unknown>): Promise<Business> {
    return this.request('/api/businesses', { method: 'POST', body: payload });
  }

  updateBusiness(businessId: string, payload: Record<string, unknown>): Promise<Business> {
    return this.request(`/api/businesses/${businessId}`, { method: 'PUT', body: payload });
  }

  listBusinessMembers(businessId: string): Promise<BusinessMember[]> {
    return this.request(`/api/businesses/${businessId}/members`);
  }

  addBusinessMember(businessId: string, email: string, role: BusinessMemberRole): Promise<BusinessMember> {
    return this.request(`/api/businesses/${businessId}/members`, { method: 'POST', body: { email, role } });
  }

  removeBusinessMember(businessId: string, userId: string): Promise<void> {
    return this.request(`/api/businesses/${businessId}/members/${userId}`, { method: 'DELETE' });
  }

  getTaxProfile(businessId: string): Promise<TaxProfile> {
    return this.request(`/api/businesses/${businessId}/tax-profile`);
  }

  updateTaxProfile(businessId: string, payload: TaxProfile): Promise<TaxProfile> {
    return this.request(`/api/businesses/${businessId}/tax-profile`, { method: 'PUT', body: payload });
  }

  getDashboard(businessId: string): Promise<DashboardSummary> {
    return this.request(`/api/businesses/${businessId}/dashboard`);
  }

  getObligations(businessId: string): Promise<TaxObligation[]> {
    return this.request(`/api/businesses/${businessId}/obligations`);
  }

  updateFilingStatus(businessId: string, taxType: string, dueDate: string, filingStatus: FilingStatus): Promise<TaxCalendarEntry> {
    return this.request(`/api/businesses/${businessId}/filing-status`, {
      method: 'PUT',
      body: { taxType, dueDate, filingStatus }
    });
  }

  listFilingPeriods(businessId: string): Promise<FilingPeriod[]> {
    return this.request(`/api/businesses/${businessId}/filing-periods`);
  }

  createFilingPeriod(businessId: string, payload: FilingPeriodInput): Promise<FilingPeriod> {
    return this.request(`/api/businesses/${businessId}/filing-periods`, { method: 'POST', body: payload });
  }

  getPreKraCheck(businessId: string): Promise<PreKraCheck> {
    return this.request(`/api/businesses/${businessId}/pre-kra-check`);
  }

  async reconcileBooks(businessId: string, periodId: string, file: File): Promise<BooksReconciliation> {
    const form = new FormData();
    form.append('file', file);
    const token = localStorage.getItem(this.tokenKey);
    const response = await fetch(
      `${this.apiBaseUrl}/api/businesses/${businessId}/filing-periods/${periodId}/reconcile`,
      {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: form
      }
    );
    if (!response.ok) {
      if (response.status === 401) {
        this.clearSession();
      }
      throw new Error(await this.readError(response));
    }
    return response.json() as Promise<BooksReconciliation>;
  }

  listDocuments(businessId: string): Promise<DocumentRecord[]> {
    return this.request(`/api/businesses/${businessId}/documents`);
  }

  async uploadDocument(businessId: string, file: File, category: string): Promise<DocumentRecord> {
    const form = new FormData();
    form.append('file', file);
    if (category) {
      form.append('category', category);
    }
    const token = localStorage.getItem(this.tokenKey);
    const response = await fetch(`${this.apiBaseUrl}/api/businesses/${businessId}/documents`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form
    });
    if (!response.ok) {
      if (response.status === 401) {
        this.clearSession();
      }
      throw new Error(await this.readError(response));
    }
    return response.json() as Promise<DocumentRecord>;
  }

  askAssistant(businessId: string, message: string): Promise<AssistantAnswer> {
    return this.request(`/api/businesses/${businessId}/assistant`, { method: 'POST', body: { message } });
  }

  getAdminOverview(): Promise<AdminOverview> {
    return this.request('/api/admin/overview');
  }

  listAdminFilingPeriods(): Promise<AdminFilingPeriod[]> {
    return this.request('/api/admin/filing-periods');
  }

  listAdminUsers(): Promise<AdminUser[]> {
    return this.request('/api/admin/users?limit=500');
  }

  setAdminUserEnabled(userId: string, enabled: boolean): Promise<AdminUser> {
    return this.request(`/api/admin/users/${userId}/enabled`, { method: 'PATCH', body: { enabled } });
  }

  deleteAdminUser(userId: string): Promise<void> {
    return this.request(`/api/admin/users/${userId}`, { method: 'DELETE' });
  }

  listAdminBusinesses(): Promise<AdminBusiness[]> {
    return this.request('/api/admin/businesses?limit=500');
  }

  updateAdminBusiness(businessId: string, payload: Record<string, unknown>): Promise<AdminBusiness> {
    return this.request(`/api/admin/businesses/${businessId}`, { method: 'PUT', body: payload });
  }

  deleteAdminBusiness(businessId: string): Promise<void> {
    return this.request(`/api/admin/businesses/${businessId}`, { method: 'DELETE' });
  }

  addAdminBusinessMember(businessId: string, email: string, role: string): Promise<AdminMembership[]> {
    return this.request(`/api/admin/businesses/${businessId}/members`, {
      method: 'POST',
      body: { email, role }
    });
  }

  removeAdminBusinessMember(businessId: string, userId: string): Promise<AdminMembership[]> {
    return this.request(`/api/admin/businesses/${businessId}/members/${userId}`, { method: 'DELETE' });
  }

  listAdminAuditLogs(): Promise<AdminAuditEntry[]> {
    return this.request('/api/admin/audit-logs?limit=200');
  }

  listAdminTaxRules(): Promise<AdminTaxRule[]> {
    return this.request('/api/admin/tax-rules');
  }

  createAdminTaxRule(payload: AdminTaxRuleInput): Promise<AdminTaxRule> {
    return this.request('/api/admin/tax-rules', { method: 'POST', body: payload });
  }

  updateAdminTaxRule(ruleId: string, payload: AdminTaxRuleInput): Promise<AdminTaxRule> {
    return this.request(`/api/admin/tax-rules/${ruleId}`, { method: 'PUT', body: payload });
  }

  private async request<T>(
    path: string,
    options: { method?: string; body?: object } = {}
  ): Promise<T> {
    const token = localStorage.getItem(this.tokenKey);
    const omitsBearer = /\/api\/auth\/(login|register|refresh)$/.test(path);
    const headers: Record<string, string> = {};
    if (options.body) {
      headers['Content-Type'] = 'application/json';
    }
    if (token && !omitsBearer) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${this.apiBaseUrl}${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined
    });

    if (!response.ok) {
      if (response.status === 401) {
        this.clearSession();
      }
      throw new Error(await this.readError(response));
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return response.json() as Promise<T>;
  }

  private async readError(response: Response): Promise<string> {
    const responseBody = await response.text();
    try {
      const errorBody = JSON.parse(responseBody) as { message?: string };
      if (errorBody.message) {
        return errorBody.message;
      }
    } catch {
      // fall through to raw text
    }
    return responseBody.trim().slice(0, 300) || `Request failed (${response.status})`;
  }
}
