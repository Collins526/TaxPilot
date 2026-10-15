import { Injectable } from '@angular/core';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
}

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
