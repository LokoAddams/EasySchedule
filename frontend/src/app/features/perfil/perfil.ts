import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { NgbDateStruct, NgbDatepickerModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/services/auth-session.service';
import { LanguageService } from '../../core/services/language.service';
import { ToastService } from '../../core/services/toast.service';
import { ChangePasswordRequest, PerfilResponse, PerfilUpdateRequest } from './perfil.model';
import { PerfilService } from './perfil.service';

type PerfilEditForm = FormGroup<{
  username: FormControl<string>;
  nombre: FormControl<string>;
  apellido: FormControl<string>;
  email: FormControl<string>;
  carnetIdentidad: FormControl<string>;
  fechaNacimiento: FormControl<NgbDateStruct | null>;
  carrera: FormControl<string>;
  universidad: FormControl<string>;
}>;

type PasswordChangeForm = FormGroup<{
  currentPassword: FormControl<string>;
  newPassword: FormControl<string>;
  confirmNewPassword: FormControl<string>;
}>;

@Component({
  selector: 'app-perfil',
  imports: [CommonModule, ReactiveFormsModule, NgbDatepickerModule, TranslatePipe],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil implements OnInit {
  protected perfil: PerfilResponse | null = null;
  protected editMode = false;
  protected loading = true;
  protected saving = false;
  protected changingPassword = false;
  protected errorKey = '';
  protected showIdentityConfirmModal = false;
  protected showChangePasswordModal = false;
  protected readonly fechaNacimientoMinDate: NgbDateStruct = { year: 1950, month: 1, day: 1 };
  protected readonly fechaNacimientoMaxDate: NgbDateStruct;
  protected readonly editForm: PerfilEditForm;
  protected readonly passwordForm: PasswordChangeForm;
  private pendingUpdatePayload: PerfilUpdateRequest | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly perfilService: PerfilService,
    private readonly authSessionService: AuthSessionService,
    private readonly languageService: LanguageService,
    private readonly toastService: ToastService,
    private readonly translateService: TranslateService,
  ) {
    const today = new Date();
    this.fechaNacimientoMaxDate = {
      year: today.getFullYear(),
      month: today.getMonth() + 1,
      day: today.getDate(),
    };

    this.editForm = this.fb.group({
      username: this.fb.nonNullable.control('', [Validators.required]),
      nombre: this.fb.nonNullable.control('', [Validators.required]),
      apellido: this.fb.nonNullable.control('', [Validators.required]),
      email: this.fb.nonNullable.control('', [Validators.required, Validators.email]),
      carnetIdentidad: this.fb.nonNullable.control('', [Validators.required]),
      fechaNacimiento: this.fb.control<NgbDateStruct | null>(null, [Validators.required]),
      carrera: this.fb.nonNullable.control(''),
      universidad: this.fb.nonNullable.control(''),
    }) as PerfilEditForm;

    this.passwordForm = this.fb.group(
      {
        currentPassword: this.fb.nonNullable.control('', [Validators.required]),
        newPassword: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(8)]),
        confirmNewPassword: this.fb.nonNullable.control('', [Validators.required]),
      },
      { validators: this.passwordsMatchValidator },
    ) as PasswordChangeForm;
  }

  ngOnInit(): void {
    const username = this.authSessionService.getCurrentUsername();

    if (!username) {
      this.loading = false;
      this.errorKey = 'perfil.error.noSession';
      return;
    }

    this.perfilService.getPerfilByUsername(username).subscribe({
      next: (perfilResponse) => {
        this.loading = false;
        this.perfil = perfilResponse;
        this.cargarFormulario(perfilResponse);
        this.authSessionService.setCurrentUsername(perfilResponse.username);
        this.authSessionService.setProfileCompleted(Boolean(perfilResponse.profileCompleted));
      },
      error: (error: { status?: number }) => {
        this.loading = false;

        // Si el perfil no existe para ese username, la sesión local quedó desfasada.
        if (error.status === 404) {
          this.authSessionService.clearSession();
          this.errorKey = 'perfil.error.noSession';
          return;
        }

        this.errorKey = 'perfil.error.loadFailed';
      },
    });
  }

  protected activarEdicion(): void {
    if (!this.perfil || this.editMode) {
      return;
    }

    this.editMode = true;
    this.errorKey = '';
    this.cargarFormulario(this.perfil);
  }

  protected esCampoSoloLectura(fieldName: string): boolean {
    return fieldName === 'carrera' || fieldName === 'universidad';
  }

  protected cerrarConfirmacionCambioIdentidad(): void {
    this.showIdentityConfirmModal = false;
    this.pendingUpdatePayload = null;
  }

  protected cerrarConfirmacionCambioIdentidadBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarConfirmacionCambioIdentidad();
    }
  }

  protected confirmarCambioIdentidadYGuardar(): void {
    if (!this.pendingUpdatePayload) {
      return;
    }

    const payload = this.pendingUpdatePayload;
    this.showIdentityConfirmModal = false;
    this.pendingUpdatePayload = null;
    this.ejecutarGuardado(payload);
  }

  protected cancelarEdicion(): void {
    this.editMode = false;
    this.errorKey = '';

    if (this.perfil) {
      this.cargarFormulario(this.perfil);
    }
  }

  protected guardarEdicion(): void {
    this.errorKey = '';

    if (!this.perfil || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const carrera = this.editForm.controls.carrera.value.trim();
    const universidad = this.editForm.controls.universidad.value.trim();
    const username = this.editForm.controls.username.value.trim();
    const email = this.editForm.controls.email.value.trim();

    const updatePayload: PerfilUpdateRequest = {
      username,
      nombre: this.editForm.controls.nombre.value.trim(),
      apellido: this.editForm.controls.apellido.value.trim(),
      email,
      carnetIdentidad: this.editForm.controls.carnetIdentidad.value.trim(),
      fechaNacimiento: this.formatDateForApi(this.editForm.controls.fechaNacimiento.value),
      carrera,
      universidad,
    };

    if (this.huboCambioIdentidadCritica(username, email)) {
      this.pendingUpdatePayload = updatePayload;
      this.showIdentityConfirmModal = true;
      return;
    }

    this.ejecutarGuardado(updatePayload);
  }

  protected abrirCambioContrasenia(): void {
    this.passwordForm.reset({
      currentPassword: '',
      newPassword: '',
      confirmNewPassword: '',
    });
    this.passwordForm.markAsPristine();
    this.passwordForm.markAsUntouched();
    this.showChangePasswordModal = true;
  }

  protected cerrarCambioContrasenia(): void {
    this.showChangePasswordModal = false;
    this.changingPassword = false;
  }

  protected cerrarCambioContraseniaBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarCambioContrasenia();
    }
  }

  protected guardarCambioContrasenia(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const payload: ChangePasswordRequest = {
      currentPassword: this.passwordForm.controls.currentPassword.value,
      newPassword: this.passwordForm.controls.newPassword.value,
      confirmNewPassword: this.passwordForm.controls.confirmNewPassword.value,
    };

    this.changingPassword = true;
    this.perfilService.changePassword(payload).subscribe({
      next: () => {
        this.changingPassword = false;
        this.cerrarCambioContrasenia();
        this.toastService.success('perfil.password.success.updated');
      },
      error: (error: { status?: number; error?: { message?: string } }) => {
        this.changingPassword = false;
        const backendMessage = this.normalizeBackendMessage(error.error?.message);

        if (error.status === 401) {
          this.toastService.error('perfil.password.error.unauthorized');
          return;
        }

        if (backendMessage.includes('contrasenia actual es incorrecta') || backendMessage.includes('contrasena actual es incorrecta')) {
          this.toastService.error('perfil.password.error.currentIncorrect');
          return;
        }

        if (backendMessage.includes('confirmacion no coinciden') || backendMessage.includes('no coinciden')) {
          this.toastService.error('perfil.password.error.mismatch');
          return;
        }

        if (backendMessage.includes('debe ser diferente')) {
          this.toastService.error('perfil.password.error.sameAsCurrent');
          return;
        }

        this.toastService.error('perfil.password.error.generic');
      },
    });
  }

  protected showPasswordMismatchError(): boolean {
    const confirmControl = this.passwordForm.controls.confirmNewPassword;
    const newPasswordControl = this.passwordForm.controls.newPassword;

    return (confirmControl.touched || newPasswordControl.touched)
      && Boolean(this.passwordForm.errors?.['passwordMismatch']);
  }

  protected canSubmitPasswordChange(): boolean {
    return !this.changingPassword && this.passwordForm.valid;
  }

  private ejecutarGuardado(updatePayload: PerfilUpdateRequest): void {
    if (!this.perfil) {
      return;
    }

    this.saving = true;
    this.perfilService.updatePerfil(this.perfil.username, updatePayload).subscribe({
      next: (updatedPerfil) => {
        this.saving = false;
        this.editMode = false;
        this.perfil = updatedPerfil;
        this.toastService.success('perfil.success.updated');
        this.authSessionService.setCurrentUsername(this.perfil.username);
        this.authSessionService.setProfileCompleted(Boolean(this.perfil.profileCompleted));
        this.cargarFormulario(this.perfil);
      },
      error: (error: { status?: number; error?: { message?: string } }) => {
        this.saving = false;

        if (error.status === 409) {
          const backendMessage = this.extractBackendMessage(error);

          if (backendMessage.includes('usuario')) {
            this.toastService.error('perfil.error.usernameTaken');
            return;
          }

          if (backendMessage.includes('correo')) {
            this.toastService.error('perfil.error.emailTaken');
            return;
          }

          if (backendMessage.includes('carnet')) {
            this.toastService.error('perfil.error.carnetTaken');
            return;
          }
        }

        this.errorKey = 'perfil.error.saveFailed';
        this.toastService.error('perfil.error.saveFailed');
      },
    });
  }

  private huboCambioIdentidadCritica(username: string, email: string): boolean {
    if (!this.perfil) {
      return false;
    }

    const currentUsername = this.perfil.username.trim().toLowerCase();
    const currentEmail = (this.perfil.email ?? '').trim().toLowerCase();

    return currentUsername !== username.toLowerCase() || currentEmail !== email.toLowerCase();
  }

  private extractBackendMessage(error: { error?: { message?: string } }): string {
    const message = error.error?.message;
    return this.normalizeBackendMessage(message);
  }

  private normalizeBackendMessage(message?: string): string {
    if (typeof message !== 'string') {
      return '';
    }

    return message
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }

  protected getNombreCompleto(): string {
    if (!this.perfil) {
      return '';
    }

    const nombre = (this.perfil.nombre ?? '').trim();
    const apellido = (this.perfil.apellido ?? '').trim();
    const nombreCompleto = `${nombre} ${apellido}`.trim();

    return nombreCompleto || this.perfil.username;
  }

  protected getFechaNacimientoFormateada(): string {
    if (!this.perfil?.fechaNacimiento) {
      return '—';
    }

    const fechaNacimiento = new Date(`${this.perfil.fechaNacimiento}T00:00:00`);
    const locale = this.languageService.getCurrentLanguage();

    return new Intl.DateTimeFormat(locale, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(fechaNacimiento);
  }

  protected getValorSeguro(value: string | null): string {
    const valueNormalized = (value ?? '').trim();
    
    if (valueNormalized) {
      return valueNormalized;
    }

    return '—';
  }

  protected getReadonlyMallaValue(value: string | null): string {
    const valueNormalized = (value ?? '').trim();
    return valueNormalized || this.translateService.instant('perfil.readOnly');
  }

  protected esCampoEditablePendiente(value: string | null, fieldName: string): boolean {
    return !this.esCampoSoloLectura(fieldName) && !this.tieneContenido(value);
  }

  protected esCampoAutoPendiente(value: string | null, fieldName: string): boolean {
    return this.esCampoSoloLectura(fieldName) && !this.tieneContenido(value);
  }

  private tieneContenido(value: string | null): boolean {
    return (value ?? '').trim().length > 0;
  }

  private cargarFormulario(perfil: PerfilResponse): void {
    this.editForm.patchValue({
      username: perfil.username ?? '',
      nombre: perfil.nombre ?? '',
      apellido: perfil.apellido ?? '',
      email: perfil.email ?? '',
      carnetIdentidad: perfil.carnetIdentidad ?? '',
      fechaNacimiento: this.toDateStruct(perfil.fechaNacimiento),
      carrera: perfil.carrera ?? '',
      universidad: perfil.universidad ?? '',
    });
  }

  private toDateStruct(rawDate: string | null): NgbDateStruct | null {
    if (!rawDate) {
      return null;
    }

    const dateParts = rawDate.split('-');
    if (dateParts.length !== 3) {
      return null;
    }

    const year = Number(dateParts[0]);
    const month = Number(dateParts[1]);
    const day = Number(dateParts[2]);

    if (!Number.isFinite(year) || !Number.isFinite(month) || !Number.isFinite(day)) {
      return null;
    }

    return { year, month, day };
  }

  private formatDateForApi(dateStruct: NgbDateStruct | null): string {
    if (!dateStruct) {
      return '';
    }

    const month = String(dateStruct.month).padStart(2, '0');
    const day = String(dateStruct.day).padStart(2, '0');
    return `${dateStruct.year}-${month}-${day}`;
  }

  private passwordsMatchValidator = (form: FormGroup): ValidationErrors | null => {
    const newPassword = form.get('newPassword')?.value;
    const confirmNewPassword = form.get('confirmNewPassword')?.value;

    if (!newPassword || !confirmNewPassword) {
      return null;
    }

    return newPassword === confirmNewPassword ? null : { passwordMismatch: true };
  };
}
