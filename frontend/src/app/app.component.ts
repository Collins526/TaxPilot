import { Component } from '@angular/core';
import {
  AuthResponse,
  Business,
  DashboardSummary,
  TaxCalendarEntry,
  TaxObligation,
  TaxPilotApiService,
  TaxProfile
} from './taxpilot-api.service';
import { TaxToolsComponent } from './tax-tools.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [TaxToolsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  showLanding = true;
  isLogin = true;
  activePage: 'overview' | 'obligations' | 'profile' | 'deadline' | 'tools' = 'overview';
  addingBusiness = false;
  busy = false;
  loadingDashboard = false;
  errorMessage = '';
  successMessage = '';
  userName = '';
  userEmail = '';
  businesses: Business[] = [];
  selectedBusinessId = '';
  dashboard: DashboardSummary | null = null;
  obligations: TaxObligation[] = [];
  profile: TaxProfile | null = null;
  selectedDeadline: TaxCalendarEntry | null = null;

  constructor(private readonly api: TaxPilotApiService) {
    if (this.api.hasSession) {
      const user = this.api.getStoredUser();
      this.userName = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim();
      this.userEmail = user?.email ?? '';
      void this.loadBusinesses();
    }
  }

  get isAuthenticated(): boolean {
    return this.api.hasSession;
  }

  get selectedBusiness(): Business | undefined {
    return this.businesses.find((business) => business.id === this.selectedBusinessId);
  }

  openPage(page: 'overview' | 'obligations' | 'profile' | 'tools'): void {
    this.activePage = page;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  openDeadline(entry: TaxCalendarEntry): void {
    this.selectedDeadline = entry;
    this.activePage = 'deadline';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  get selectedDeadlineObligation(): TaxObligation | undefined {
    return this.obligations.find((obligation) => obligation.taxType === this.selectedDeadline?.taxType);
  }

  get selectedDeadlineAmount(): number | null {
    return this.selectedDeadline?.amount ?? this.selectedDeadlineObligation?.estimatedAmount ?? null;
  }

  returnToOverview(): void {
    this.selectedDeadline = null;
    this.openPage('overview');
  }

  openOverviewSection(sectionId: 'calendar'): void {
    this.activePage = 'overview';
    window.setTimeout(() => {
      document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  }

  openAuth(mode: 'login' | 'register'): void {
    this.isLogin = mode === 'login';
    this.showLanding = false;
    this.errorMessage = '';
    this.successMessage = '';
  }

  returnToLanding(): void {
    this.showLanding = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  toggleAuthMode(): void {
    this.isLogin = !this.isLogin;
    this.errorMessage = '';
    this.successMessage = '';
  }

  async submitAuth(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const form = event.currentTarget as HTMLFormElement;
    const values = new FormData(form);
    this.busy = true;
    this.errorMessage = '';
    try {
      let result: AuthResponse;
      if (this.isLogin) {
        result = await this.api.login(
          String(values.get('email') ?? ''),
          String(values.get('password') ?? '')
        );
      } else {
        result = await this.api.register({
          firstName: String(values.get('firstName') ?? ''),
          lastName: String(values.get('lastName') ?? ''),
          email: String(values.get('email') ?? ''),
          phone: String(values.get('phone') ?? ''),
          password: String(values.get('password') ?? '')
        });
      }
      this.api.saveSession(result);
      this.userName = `${result.firstName} ${result.lastName}`.trim();
      this.userEmail = result.email;
      await this.loadBusinesses();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  async submitBusiness(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const values = new FormData(event.currentTarget as HTMLFormElement);
    this.busy = true;
    this.errorMessage = '';
    try {
      const business = await this.api.createBusiness({
        name: values.get('name'),
        legalName: values.get('legalName'),
        registrationNumber: values.get('registrationNumber'),
        kraPin: values.get('kraPin'),
        industry: values.get('industry'),
        businessType: values.get('businessType'),
        annualTurnover: Number(values.get('annualTurnover') || 0),
        employeeCount: Number(values.get('employeeCount') || 0),
        vatRegistered: values.has('vatRegistered'),
        etimsRegistered: values.has('etimsRegistered'),
        countryCode: 'KE',
        timezone: 'Africa/Nairobi'
      });
      this.businesses = [...this.businesses, business];
      this.selectedBusinessId = business.id;
      this.addingBusiness = false;
      await this.loadSelectedBusiness();
      this.successMessage = 'Business profile created. Review your tax profile below.';
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  async saveBusiness(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    if (!this.selectedBusinessId) {
      return;
    }
    const values = new FormData(event.currentTarget as HTMLFormElement);
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      const updated = await this.api.updateBusiness(this.selectedBusinessId, {
        name: values.get('name'),
        legalName: values.get('legalName'),
        registrationNumber: values.get('registrationNumber'),
        kraPin: values.get('kraPin'),
        industry: values.get('industry'),
        businessType: values.get('businessType'),
        annualTurnover: Number(values.get('annualTurnover') || 0),
        employeeCount: Number(values.get('employeeCount') || 0),
        vatRegistered: values.has('vatRegistered'),
        etimsRegistered: values.has('etimsRegistered'),
        countryCode: 'KE',
        timezone: 'Africa/Nairobi'
      });
      this.businesses = this.businesses.map((business) => business.id === updated.id ? updated : business);
      await this.loadSelectedBusiness();
      this.successMessage = 'Business profile updated.';
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  cancelAddingBusiness(): void {
    this.addingBusiness = false;
    this.errorMessage = '';
  }

  async saveTaxProfile(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    if (!this.selectedBusinessId || !this.profile) {
      return;
    }
    const values = new FormData(event.currentTarget as HTMLFormElement);
    const annualTurnover = Number(values.get('annualTurnover') || 0);
    const updated: TaxProfile = {
      ...this.profile,
      businessType: this.selectedBusiness?.businessType ?? this.profile.businessType,
      annualTurnover,
      monthlyTurnover: Number(values.get('monthlyTurnover') || annualTurnover / 12),
      employeeCount: Number(values.get('employeeCount') || 0),
      vatRegistered: values.has('vatRegistered'),
      vatRegistrationDate: String(values.get('vatRegistrationDate') || '') || null,
      etimsRegistered: values.has('etimsRegistered'),
      hasEmployees: values.has('hasEmployees'),
      makesWithholdingPayments: values.has('makesWithholdingPayments'),
      receivesRentalIncome: values.has('receivesRentalIncome'),
      importsGoods: values.has('importsGoods'),
      exportsGoods: values.has('exportsGoods'),
      hasForeignTransactions: values.has('hasForeignTransactions'),
      hasDigitalServices: values.has('hasDigitalServices'),
      hasExciseActivities: values.has('hasExciseActivities'),
      incomeTaxApplicable: values.has('incomeTaxApplicable'),
      corporationTaxApplicable: values.has('corporationTaxApplicable'),
      turnoverTaxApplicable: values.has('turnoverTaxApplicable')
    };
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      this.profile = await this.api.updateTaxProfile(this.selectedBusinessId, updated);
      await this.loadSelectedBusiness();
      this.successMessage = 'Tax profile saved. Obligations and dashboard have been refreshed.';
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  async switchBusiness(event: Event): Promise<void> {
    this.selectedBusinessId = (event.currentTarget as HTMLSelectElement).value;
    this.selectedDeadline = null;
    if (this.activePage === 'deadline') {
      this.activePage = 'overview';
    }
    await this.loadSelectedBusiness();
  }

  signOut(): void {
    const refreshToken = this.api.getStoredUser()?.refreshToken ?? null;
    void this.api.logout(refreshToken).catch(() => undefined);
    this.api.clearSession();
    this.activePage = 'overview';
    this.businesses = [];
    this.selectedBusinessId = '';
    this.dashboard = null;
    this.obligations = [];
    this.profile = null;
    this.selectedDeadline = null;
    this.userName = '';
    this.userEmail = '';
    this.errorMessage = '';
  }

  formatMoney(value: number | null): string {
    if (value === null || value === undefined) {
      return 'Not estimated yet';
    }
    return new Intl.NumberFormat('en-KE', {
      style: 'currency',
      currency: 'KES',
      maximumFractionDigits: 0
    }).format(value);
  }

  formatDate(date: string | null): string {
    if (!date) {
      return 'Date depends on filing/payment activity';
    }
    return new Intl.DateTimeFormat('en-KE', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      timeZone: 'UTC'
    }).format(new Date(date));
  }

  statusClass(entry: TaxCalendarEntry): string {
    return entry.status.toLowerCase().replace('_', '-');
  }

  private async loadBusinesses(): Promise<void> {
    this.loadingDashboard = true;
    this.errorMessage = '';
    try {
      this.businesses = await this.api.listBusinesses();
      if (this.businesses.length === 0) {
        this.selectedBusinessId = '';
        return;
      }
      const stillAvailable = this.businesses.some((business) => business.id === this.selectedBusinessId);
      if (!stillAvailable) {
        this.selectedBusinessId = this.businesses[0].id;
      }
      await this.loadSelectedBusiness();
    } catch (error) {
      this.errorMessage = this.errorText(error);
      if (this.errorMessage.toLowerCase().includes('token')) {
        this.signOut();
      }
    } finally {
      this.loadingDashboard = false;
    }
  }

  private async loadSelectedBusiness(): Promise<void> {
    if (!this.selectedBusinessId) {
      this.dashboard = null;
      this.obligations = [];
      this.profile = null;
      return;
    }
    this.loadingDashboard = true;
    try {
      const [dashboard, obligations, profile] = await Promise.all([
        this.api.getDashboard(this.selectedBusinessId),
        this.api.getObligations(this.selectedBusinessId),
        this.api.getTaxProfile(this.selectedBusinessId)
      ]);
      this.dashboard = dashboard;
      this.obligations = obligations;
      this.profile = profile;
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.loadingDashboard = false;
    }
  }

  private errorText(error: unknown): string {
    return error instanceof Error ? error.message : 'The request could not be completed.';
  }
}
