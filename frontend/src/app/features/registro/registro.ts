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
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
  ],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class Registro {

  successMessage = '';
  errorMessage = '';
  loading = false;
  private readonly primaryRegisterPath = '/api/estudiantes/registro';
  private readonly fallbackRegisterPath = '/api/estudiantes/registro';

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly apiService: ApiService,
    private readonly router: Router,
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
    this.successMessage = '';
    this.errorMessage = '';

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
        this.successMessage = $localize`:@@registro.success:Registro exitoso. Redirigiendo...`;

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
          this.errorMessage = $localize`:@@registro.error.userExists:El usuario ya está registrado`;
        } else if (err.status === 409 && backendMessage.includes('correo')) {
          this.errorMessage = $localize`:@@registro.error.emailExists:El correo ya está registrado`;
        } else if (err.status === 400) {
          this.errorMessage = $localize`:@@registro.error.invalidData:Datos inválidos`;
        } else if (err.status === 0) {
          this.errorMessage = $localize`:@@registro.error.backendConnection:No se pudo conectar con el backend`;
        } else {
          this.errorMessage = $localize`:@@registro.error.server:Error del servidor. Intenta nuevamente`;
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
