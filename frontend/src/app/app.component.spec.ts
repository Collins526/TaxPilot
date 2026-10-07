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
  });
});
