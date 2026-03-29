import { Component } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule } from '@angular/router'; 
import { FeatureToggleService } from '../../services/feature-toggle.service';
import { AuthSessionService } from '../../core/services/auth-session.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, CommonModule, RouterModule],
  
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {

  loading = false;
  errorMessageKey: string | null = null;


  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private featureToggleService: FeatureToggleService,
    private authSessionService: AuthSessionService,
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
      this.errorMessageKey = 'login.validation.required';
      return;
    }

    this.loading = true;
    this.errorMessageKey = null;
    this.authSessionService.clearSession();

    try {
      const res = await fetch('http://localhost:8080/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.form.value)
      });

      // Manejo de errores HTTP
      if (!res.ok) {
        this.errorMessageKey = res.status === 401
          ? 'login.error.invalidCredentials'
          : 'login.error.generic';
        return;
      }

      const data = await res.json();
      const rawIdentifier = this.form.get('identifier')?.value;
      const identifier = typeof rawIdentifier === 'string' ? rawIdentifier.trim() : '';

      // Guardar token
      localStorage.setItem('token', data.token);

      // Guardar username real para que Perfil pueda resolver /perfil/{username}.
      const backendUsername = typeof data.username === 'string' ? data.username.trim() : '';
      const fallbackUsername = identifier && !identifier.includes('@') ? identifier : '';
      const usernameToStore = backendUsername || fallbackUsername;

      if (usernameToStore) {
        this.authSessionService.setCurrentUsername(usernameToStore);
      }

      // Refrescar toggles para reflejar el estado en el navbar inmediatamente.
      await this.featureToggleService.loadFlags();

      // Redirección
      this.router.navigate(['/perfil']);

    } catch {
      this.errorMessageKey = 'login.error.generic';
    } finally {
      this.loading = false;
    }
  }
}