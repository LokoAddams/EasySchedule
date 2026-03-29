import { Component } from '@angular/core';
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

interface LoginResponse {
  token?: string;
  username?: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, CommonModule, RouterModule],
  
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {

  loading = false;


  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private featureToggleService: FeatureToggleService,
    private authSessionService: AuthSessionService,
    private perfilService: PerfilService,
    private apiService: ApiService,
    private toastService: ToastService,
  ) {

    this.form = this.fb.group({
      identifier: ['', Validators.required],
      password: ['', Validators.required]
    });
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

      this.authSessionService.setAuthToken(String(data.token ?? ''));

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
