import { CommonModule } from '@angular/common';
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import {
  AdminAuditEntry,
  AdminBusiness,
  AdminOverview,
  AdminTaxRule,
  AdminTaxRuleInput,
  AdminUser,
  TaxPilotApiService
} from './taxpilot-api.service';

type AdminSection = 'overview' | 'users' | 'businesses' | 'audit' | 'rules';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  @Output() readonly signOutRequested = new EventEmitter<void>();

  section: AdminSection = 'overview';
  overview: AdminOverview | null = null;
  users: AdminUser[] = [];
  businesses: AdminBusiness[] = [];
  auditLogs: AdminAuditEntry[] = [];
  taxRules: AdminTaxRule[] = [];
  selectedBusinessId = '';
  editingBusinessId = '';
  editingRuleId = '';
  loading = true;
  busy = false;
  errorMessage = '';
  successMessage = '';
  searchText = '';

  constructor(private readonly api: TaxPilotApiService) {}

  ngOnInit(): void {
    void this.loadAdminData();
  }

  get selectedBusiness(): AdminBusiness | undefined {
    return this.businesses.find((business) => business.id === this.selectedBusinessId);
  }

  get editingRule(): AdminTaxRule | undefined {
    return this.taxRules.find((rule) => rule.id === this.editingRuleId);
  }

  get editingRuleBandsJson(): string {
    return this.editingRule ? this.ruleBandsJson(this.editingRule) : '[]';
  }

  get filteredUsers(): AdminUser[] {
    const query = this.searchText.trim().toLowerCase();
    return query
      ? this.users.filter((user) =>
          `${user.firstName} ${user.lastName} ${user.email} ${user.roles.join(' ')}`.toLowerCase().includes(query))
      : this.users;
  }

  get filteredBusinesses(): AdminBusiness[] {
    const query = this.searchText.trim().toLowerCase();
    return query
      ? this.businesses.filter((business) =>
          `${business.name} ${business.legalName} ${business.registrationNumber} ${business.kraPin}`
            .toLowerCase().includes(query))
      : this.businesses;
  }

  async loadAdminData(): Promise<void> {
    this.loading = true;
    this.errorMessage = '';
    try {
      const [overview, users, businesses, auditLogs, taxRules] = await Promise.all([
        this.api.getAdminOverview(),
        this.api.listAdminUsers(),
        this.api.listAdminBusinesses(),
        this.api.listAdminAuditLogs(),
        this.api.listAdminTaxRules()
      ]);
      this.overview = overview;
      this.users = users;
      this.businesses = businesses;
      this.auditLogs = auditLogs;
      this.taxRules = taxRules;
      if (!this.businesses.some((business) => business.id === this.selectedBusinessId)) {
        this.selectedBusinessId = this.businesses[0]?.id ?? '';
      }
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.loading = false;
    }
  }

  setSection(section: AdminSection): void {
    this.section = section;
    this.errorMessage = '';
    this.successMessage = '';
    this.searchText = '';
  }

  async setUserEnabled(user: AdminUser): Promise<void> {
    this.errorMessage = '';
    this.successMessage = '';
    try {
      const updated = await this.api.setAdminUserEnabled(user.id, !user.enabled);
      this.users = this.users.map((current) => current.id === updated.id ? updated : current);
      this.successMessage = `${updated.email} was ${updated.enabled ? 'enabled' : 'disabled'}.`;
      await this.refreshOverview();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    }
  }

  async deleteUser(user: AdminUser): Promise<void> {
    if (!window.confirm(`Permanently delete ${user.email}? This action cannot be undone.`)) {
      return;
    }
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      await this.api.deleteAdminUser(user.id);
      this.users = this.users.filter((current) => current.id !== user.id);
      this.successMessage = `${user.email} was permanently deleted.`;
      await this.refreshOverview();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  selectBusiness(businessId: string): void {
    this.selectedBusinessId = businessId;
    this.editingBusinessId = '';
  }

  async saveBusiness(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const current = this.selectedBusiness;
    if (!current) {
      return;
    }
    const values = new FormData(event.currentTarget as HTMLFormElement);
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      const updated = await this.api.updateAdminBusiness(current.id, {
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
        countryCode: current.countryCode,
        timezone: current.timezone
      });
      this.businesses = this.businesses.map((business) => business.id === updated.id ? updated : business);
      this.editingBusinessId = '';
      this.successMessage = `Business "${updated.name}" was updated.`;
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  async deleteBusiness(business: AdminBusiness): Promise<void> {
    if (!window.confirm(
      `Permanently delete "${business.name}" and its associated memberships, tax profiles, filings, and documents? This action cannot be undone.`
    )) {
      return;
    }
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      await this.api.deleteAdminBusiness(business.id);
      this.businesses = this.businesses.filter((current) => current.id !== business.id);
      this.selectedBusinessId = this.businesses[0]?.id ?? '';
      this.successMessage = `Business "${business.name}" was permanently deleted.`;
      await this.refreshOverview();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  async addMember(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const business = this.selectedBusiness;
    if (!business) {
      return;
    }
    const form = event.currentTarget as HTMLFormElement;
    const values = new FormData(form);
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      const members = await this.api.addAdminBusinessMember(
        business.id,
        String(values.get('email') ?? ''),
        String(values.get('role') ?? '')
      );
      this.businesses = this.businesses.map((current) =>
        current.id === business.id ? { ...current, members } : current);
      form.reset();
      this.successMessage = 'Business membership added.';
      await this.refreshUsers();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  async removeMember(member: AdminBusiness['members'][number]): Promise<void> {
    const business = this.selectedBusiness;
    if (!business || !window.confirm(`Remove ${member.email} from ${business.name}?`)) {
      return;
    }
    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      const members = await this.api.removeAdminBusinessMember(business.id, member.userId);
      this.businesses = this.businesses.map((current) =>
        current.id === business.id ? { ...current, members } : current);
      this.successMessage = `${member.email} was removed from ${business.name}.`;
      await this.refreshUsers();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  startNewRule(): void {
    this.editingRuleId = 'new';
    this.successMessage = '';
    this.errorMessage = '';
  }

  editRule(rule: AdminTaxRule): void {
    this.editingRuleId = rule.id;
    this.successMessage = '';
    this.errorMessage = '';
  }

  async saveRule(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    const form = event.currentTarget as HTMLFormElement;
    const values = new FormData(form);
    let bands: AdminTaxRuleInput['bands'];
    try {
      bands = JSON.parse(String(values.get('bands') ?? '[]')) as AdminTaxRuleInput['bands'];
      if (!Array.isArray(bands)) {
        throw new Error('Tax bands must be a JSON array.');
      }
    } catch {
      this.errorMessage = 'Tax bands must be valid JSON in an array, for example [{"bandOrder":1,"lowerBound":0,"upperBound":null,"rate":0.1}].';
      return;
    }

    const payload: AdminTaxRuleInput = {
      ruleCode: String(values.get('ruleCode') ?? '').trim(),
      ruleVersion: String(values.get('ruleVersion') ?? '').trim(),
      frequency: String(values.get('frequency') ?? '').trim(),
      dueDay: values.get('dueDay') ? Number(values.get('dueDay')) : null,
      effectiveFrom: String(values.get('effectiveFrom') ?? ''),
      effectiveTo: String(values.get('effectiveTo') ?? '') || null,
      rate: values.get('rate') ? Number(values.get('rate')) : null,
      threshold: values.get('threshold') ? Number(values.get('threshold')) : null,
      source: String(values.get('source') ?? '').trim(),
      bands
    };

    this.busy = true;
    this.errorMessage = '';
    this.successMessage = '';
    try {
      if (this.editingRuleId === 'new') {
        await this.api.createAdminTaxRule(payload);
        this.successMessage = `Tax rule ${payload.ruleCode} ${payload.ruleVersion} created.`;
      } else {
        await this.api.updateAdminTaxRule(this.editingRuleId, payload);
        this.successMessage = `Tax rule ${payload.ruleCode} ${payload.ruleVersion} updated.`;
      }
      this.editingRuleId = '';
      await this.refreshTaxRules();
    } catch (error) {
      this.errorMessage = this.errorText(error);
    } finally {
      this.busy = false;
    }
  }

  cancelRuleEdit(): void {
    this.editingRuleId = '';
  }

  ruleBandsJson(rule: AdminTaxRule): string {
    return JSON.stringify(rule.bands, null, 2);
  }

  requestSignOut(): void {
    this.signOutRequested.emit();
  }

  private async refreshOverview(): Promise<void> {
    this.overview = await this.api.getAdminOverview();
  }

  private async refreshUsers(): Promise<void> {
    this.users = await this.api.listAdminUsers();
    await this.refreshOverview();
  }

  private async refreshTaxRules(): Promise<void> {
    this.taxRules = await this.api.listAdminTaxRules();
  }

  private errorText(error: unknown): string {
    return error instanceof Error ? error.message : 'The request could not be completed.';
  }
}
