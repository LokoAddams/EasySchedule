import { AfterViewInit, Component, ElementRef, NgZone, ViewChild } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router'; 
import { firstValueFrom } from 'rxjs';

import { FeatureToggleService } from '../../services/feature-toggle.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { PerfilService } from '../perfil/perfil.service';
import { ApiService } from '../../services/api.service';
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

interface LoginResponse {
  token?: string;
  username?: string;
  expiresInSeconds?: number;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, CommonModule, RouterModule],
  
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent implements AfterViewInit {

  loading = false;
  showPassword = false;

  googleLoading = false;
  googleButtonReady = false;

  @ViewChild('googleButtonContainer')
  private googleButtonContainer?: ElementRef<HTMLDivElement>;


  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private featureToggleService: FeatureToggleService,
    private authSessionService: AuthSessionService,
    private perfilService: PerfilService,
    private apiService: ApiService,
    private toastService: ToastService,
    private zone: NgZone
  ) {

    this.form = this.fb.group({
      identifier: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  ngAfterViewInit(): void {
    this.loadGoogleSignInScript()
      .then(() => this.renderGoogleButton())
      .catch(() => {
        this.toastService.error('login.error.googleUnavailable');
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
      text: 'signin_with',
      shape: 'rectangular',
      width: 320,
    });

    this.googleButtonReady = true;
  }

  private handleGoogleCredential(response: GoogleCredentialResponse): void {
    if (!response.credential) {
      this.toastService.error('login.error.googleCancelled');
      return;
    }

    this.toastService.success('login.success.googleCredentialReceived');
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  async login(): Promise<void> {

    // Validacion
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.authSessionService.clearSession();

    try {
      const data = await firstValueFrom(
        this.apiService.post<LoginResponse, { identifier: string; password: string }>(
          '/api/login',
          this.form.value,
        ),
      );
      const rawIdentifier = this.form.get('identifier')?.value;
      const identifier = typeof rawIdentifier === 'string' ? rawIdentifier.trim() : '';

      this.authSessionService.setAuthToken(
        String(data.token ?? ''),
        Number(data.expiresInSeconds ?? 3600),
      );

      const backendUsername = typeof data.username === 'string' ? data.username.trim() : '';
      const identifierToUse = backendUsername || identifier;

      if (!identifierToUse) {
        this.toastService.error('login.error.generic');
        this.authSessionService.clearSession();
        return;
      }

      const perfil = await firstValueFrom(this.perfilService.getPerfilByUsername(identifierToUse));

      this.authSessionService.setCurrentUsername(perfil.username);
      this.authSessionService.setProfileCompleted(perfil.profileCompleted ?? false);

      // Refrescar toggles para reflejar el estado en el navbar inmediatamente.
      await this.featureToggleService.loadFlags();

      if (perfil.profileCompleted) {
        this.toastService.success('login.success.loggedIn');
        this.router.navigate(['/home']);
      } else {
        this.toastService.success('login.success.completeProfile');
        this.router.navigate(['/perfil']);
      }

    } catch (error: any) {
      const status = Number(error?.status ?? 0);
      const messageKey = status === 401
        ? 'login.error.invalidCredentials'
        : 'login.error.generic';
      this.toastService.error(messageKey);
      this.authSessionService.clearSession();
    } finally {
      this.loading = false;
    }
  }
}
