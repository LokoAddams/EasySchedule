import { Component } from '@angular/core';
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
export class Registro {

  successMessageKey = '';
  errorMessageKey = '';
  loading = false;
  private readonly primaryRegisterPath = '/api/estudiantes/registro';
  private readonly fallbackRegisterPath = '/api/registro';

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly apiService: ApiService,
    private readonly authSessionService: AuthSessionService,
    private readonly router: Router,
    private readonly toastService: ToastService,
  ) {

    this.form = this.fb.group({
      nombre: ['', Validators.required],
      correo: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required],
    }, { validators: this.passwordMatch });

  }

  passwordMatch(control: AbstractControl): ValidationErrors | null {

    const pass = control.get('password')?.value;
    const confirm = control.get('confirmPassword')?.value;

    if (!confirm) {
      return null;
    }

    return pass === confirm ? null : { mismatch: true };
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
