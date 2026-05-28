import { AfterViewInit, Component, ElementRef, NgZone, ViewChild } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { ToastService } from '../../core/services/toast.service';
import { environment } from '../../../environments/environment';

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (response: GoogleCredentialResponse) => void;
          }) => void;
          renderButton: (
            parent: HTMLElement,
            options: {
              theme?: string;
              size?: string;
              text?: string;
              shape?: string;
              width?: number;
            }
          ) => void;
        };
      };
    };
  }
}

interface GoogleCredentialResponse {
  credential?: string;
}

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    TranslatePipe,
  ],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class Registro implements AfterViewInit {

  successMessageKey = '';
  errorMessageKey = '';
  loading = false;
  showPassword = false;
  showConfirmPassword = false;

  googleLoading = false;
  googleButtonReady = false;

  @ViewChild('googleButtonContainer')
  private googleButtonContainer?: ElementRef<HTMLDivElement>;
  private readonly primaryRegisterPath = '/api/estudiantes/registro';
  private readonly fallbackRegisterPath = '/api/registro';

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly apiService: ApiService,
    private readonly authSessionService: AuthSessionService,
    private readonly router: Router,
    private readonly toastService: ToastService,
    private readonly zone: NgZone,
  ) {

    this.form = this.fb.group({
      nombre: ['', Validators.required],
      correo: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    }, { validators: this.passwordMatch });

  }

  ngAfterViewInit(): void {
    this.loadGoogleSignInScript()
      .then(() => this.renderGoogleButton())
      .catch(() => {
        this.toastService.error('registro.error.googleUnavailable');
      });
  }

  private loadGoogleSignInScript(): Promise<void> {
    if (window.google?.accounts?.id) {
      return Promise.resolve();
    }

    const existingScript = document.getElementById('google-signin-client');

    if (existingScript) {
      return new Promise((resolve, reject) => {
        existingScript.addEventListener('load', () => resolve(), { once: true });
        existingScript.addEventListener('error', () => reject(), { once: true });
      });
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.id = 'google-signin-client';
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => reject();

      document.head.appendChild(script);
    });
  }

  private renderGoogleButton(): void {
    const container = this.googleButtonContainer?.nativeElement;

    if (!container || !window.google?.accounts?.id) {
      return;
    }

    window.google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response: GoogleCredentialResponse) => {
        this.zone.run(() => this.handleGoogleCredential(response));
      },
    });

    container.innerHTML = '';

    window.google.accounts.id.renderButton(container, {
      theme: 'outline',
      size: 'large',
      text: 'signup_with',
      shape: 'rectangular',
      width: 320,
    });

    this.googleButtonReady = true;
  }

  private handleGoogleCredential(response: GoogleCredentialResponse): void {
    if (!response.credential) {
      this.toastService.error('registro.error.googleCancelled');
      return;
    }

    this.toastService.success('registro.googleCredentialReceived');
  }

  passwordMatch(control: AbstractControl): ValidationErrors | null {

    const pass = control.get('password')?.value;
    const confirm = control.get('confirmPassword')?.value;

    if (!confirm) {
      return null;
    }

    return pass === confirm ? null : { mismatch: true };
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPasswordVisibility() {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  registrar() {

    if (this.form.invalid) return;

    this.loading = true;
    this.successMessageKey = '';
    this.errorMessageKey = '';

    const payload = {
      username: this.form.value.nombre,
      email: this.form.value.correo,
      password: this.form.value.password
    };

    this.tryRegister(payload, this.primaryRegisterPath, true);

  }

  private tryRegister(
    payload: { username: string; email: string; password: string },
    endpoint: string,
    canFallback: boolean,
  ) {
    this.apiService.post<void, typeof payload>(endpoint, payload).subscribe({
      next: () => {
        this.loading = false;
        this.successMessageKey = 'registro.success';
        this.toastService.success('registro.success');
        this.authSessionService.setCurrentUsername(payload.username);

        setTimeout(() => {
          this.router.navigateByUrl('/login');
        }, 2000);
      },
      error: (err) => {
        if (canFallback && this.shouldFallbackToAuthEndpoint(err)) {
          this.tryRegister(payload, this.fallbackRegisterPath, false);
          return;
        }

        this.loading = false;
        const backendMessage = this.extractBackendMessage(err);

        if (err.status === 409 && backendMessage.includes('usuario')) {
          this.errorMessageKey = 'registro.error.userExists';
          this.toastService.error('registro.error.userExists');
        } else if (err.status === 409 && backendMessage.includes('correo')) {
          this.errorMessageKey = 'registro.error.emailExists';
          this.toastService.error('registro.error.emailExists');
        } else if (err.status === 400) {
          this.errorMessageKey = 'registro.error.invalidData';
          this.toastService.error('registro.error.invalidData');
        } else if (err.status === 0) {
          this.errorMessageKey = 'registro.error.backendConnection';
          this.toastService.error('registro.error.backendConnection');
        } else {
          this.errorMessageKey = 'registro.error.server';
          this.toastService.error('registro.error.server');
        }
      },
    });
  }

  private shouldFallbackToAuthEndpoint(err: any): boolean {
    const message = this.extractBackendMessage(err);
    return (
      err.status === 401 ||
      err.status === 403 ||
      err.status === 404 ||
      err.status === 405 ||
      (err.status === 500 && message.includes('malla default'))
    );
  }

  private extractBackendMessage(err: any): string {
    const rawError = err?.error;

    if (typeof rawError === 'string') {
      return rawError.toLowerCase();
    }

    if (rawError && typeof rawError.message === 'string') {
      return rawError.message.toLowerCase();
    }

    return '';
  }

}
