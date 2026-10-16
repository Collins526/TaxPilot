import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the TaxPilot landing page when signed out', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand')?.textContent).toContain('TaxPilot');
    expect(compiled.querySelector('.brand-mark')?.textContent).toBe('T');
    expect(compiled.querySelector('h1')?.textContent).toContain('Tax compliance');
    expect(compiled.querySelector('#features')).toBeTruthy();
    expect(compiled.querySelector('#how-it-works')).toBeTruthy();
    const footer = compiled.querySelector('.landing-footer');
    expect(footer?.textContent).toContain('Dashboard');
    expect(footer?.textContent).toContain('Pre-KRA Check');
    expect(footer?.textContent).toContain('© 2026 TaxPilot. All rights reserved.');
    expect(footer?.textContent).toContain('It is not a KRA service');
    expect(compiled.querySelector('input[name="email"]')).toBeFalsy();
  });

  it('should open registration from the landing page CTA', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const getStarted = Array.from(compiled.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Create your account'));
    expect(getStarted).toBeTruthy();
    getStarted?.click();
    fixture.detectChanges();
    expect(compiled.querySelector('h1')?.textContent).toContain('Create your TaxPilot account');
    expect(compiled.querySelector('input[name="firstName"]')).toBeTruthy();
    expect(compiled.querySelector('input[name="email"]')).toBeTruthy();
    expect(compiled.querySelector('input[name="confirmPassword"]')).toBeTruthy();
  });

  it('should open sign-in from the landing page', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const signIn = Array.from(compiled.querySelectorAll('button'))
      .find((button) => button.textContent?.trim() === 'Sign in');
    expect(signIn).toBeTruthy();
    signIn?.click();
    fixture.detectChanges();
    expect(compiled.querySelector('h1')?.textContent).toContain('Welcome back');
    expect(compiled.querySelector('input[name="email"]')).toBeTruthy();
    expect(compiled.querySelector('input[name="confirmPassword"]')).toBeFalsy();
  });

  it('toggles password visibility with accessible controls', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const getStarted = Array.from(compiled.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Create your account'));
    getStarted?.click();
    fixture.detectChanges();

    const password = compiled.querySelector<HTMLInputElement>('input[name="password"]');
    const confirmPassword = compiled.querySelector<HTMLInputElement>('input[name="confirmPassword"]');
    const showPassword = compiled.querySelector<HTMLButtonElement>('button[aria-label="Show password"]');
    const showConfirmPassword = compiled.querySelector<HTMLButtonElement>('button[aria-label="Show confirm password"]');
    expect(password?.type).toBe('password');
    expect(confirmPassword?.type).toBe('password');

    showPassword?.click();
    fixture.detectChanges();
    expect(password?.type).toBe('text');
    expect(compiled.querySelector('button[aria-label="Hide password"]')?.getAttribute('aria-pressed')).toBe('true');
    compiled.querySelector<HTMLButtonElement>('button[aria-label="Hide password"]')?.click();
    fixture.detectChanges();
    expect(password?.type).toBe('password');

    showConfirmPassword?.click();
    fixture.detectChanges();
    expect(confirmPassword?.type).toBe('text');
    compiled.querySelector<HTMLButtonElement>('button[aria-label="Hide confirm password"]')?.click();
    fixture.detectChanges();
    expect(confirmPassword?.type).toBe('password');
  });

  it('rejects registration when the password confirmation does not match', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const getStarted = Array.from(compiled.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Create your account'));
    getStarted?.click();
    fixture.detectChanges();

    compiled.querySelector<HTMLInputElement>('input[name="password"]')!.value = 'correct-password';
    compiled.querySelector<HTMLInputElement>('input[name="confirmPassword"]')!.value = 'different-password';
    const form = compiled.querySelector('form')!;
    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();

    expect(compiled.querySelector('[role="alert"]')?.textContent).toContain('Passwords do not match.');
  });
});
