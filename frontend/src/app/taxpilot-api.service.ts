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
}

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

  private async request<T>(
    path: string,
    options: { method?: string; body?: object } = {}
  ): Promise<T> {
    const token = localStorage.getItem(this.tokenKey);
    const headers: Record<string, string> = {};
    if (options.body) {
      headers['Content-Type'] = 'application/json';
    }
    if (token) {
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
      const errorBody = await response.json().catch(() => null) as { message?: string } | null;
      throw new Error(errorBody?.message ?? `Request failed (${response.status})`);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return response.json() as Promise<T>;
  }
}
