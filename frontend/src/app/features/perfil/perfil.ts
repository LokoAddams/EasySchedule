import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbDateStruct, NgbDatepickerModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/services/auth-session.service';
import { LanguageService } from '../../core/services/language.service';
import {MallaResponse, PerfilResponse, PerfilUpdateRequest } from './perfil.model';
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
  protected errorKey = '';
  protected showSuccessModal = false;
  protected readonly fechaNacimientoMinDate: NgbDateStruct = { year: 1950, month: 1, day: 1 };
  protected readonly fechaNacimientoMaxDate: NgbDateStruct;
  protected readonly editForm: PerfilEditForm;
  private mallasDisponibles: MallaResponse[] = [];

  constructor(
    private readonly fb: FormBuilder,
    private readonly perfilService: PerfilService,
    private readonly authSessionService: AuthSessionService,
    private readonly languageService: LanguageService,
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
  }

  ngOnInit(): void {
    const username = this.authSessionService.getCurrentUsername();

    if (!username) {
      this.loading = false;
      this.errorKey = 'perfil.error.noSession';
      return;
    }

    // El endpoint de perfil espera username; evitar usar emails guardados como identificador.
    if (username.includes('@')) {
      this.authSessionService.clearSession();
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

  protected activarEdicionDesdeCampo(fieldName: string): void {
    if (this.esCampoSoloLectura(fieldName)) {
      return;
    }

    this.activarEdicion();
  }

  protected esCampoSoloLectura(fieldName: string): boolean {
    return fieldName === 'carrera' || fieldName === 'universidad';
  }

  protected cerrarModalExito(): void {
    this.showSuccessModal = false;
  }

  protected cerrarModalExitoBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.cerrarModalExito();
    }
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

    this.saving = true;
    this.perfilService.updatePerfil(this.perfil.username, updatePayload).subscribe({
      next: (updatedPerfil) => {
        this.saving = false;
        this.editMode = false;
        this.perfil = updatedPerfil;
        this.showSuccessModal = true;
        this.authSessionService.setCurrentUsername(this.perfil.username);
        this.cargarFormulario(this.perfil);
      },
      error: () => {
        this.saving = false;
        this.errorKey = 'perfil.error.saveFailed';
      },
    });
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
      return '-';
    }

    const fechaNacimiento = new Date(`${this.perfil.fechaNacimiento}T00:00:00`);
    const locale = this.languageService.getCurrentLanguage();

    return new Intl.DateTimeFormat(locale, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(fechaNacimiento);
  }

  protected getValorSeguro(value: string | null, fieldName?: string): string {
    const valueNormalized = (value ?? '').trim();
    
    if (valueNormalized) {
      return valueNormalized;
    }

    // Si no hay valor y se especifica el nombre del campo, retornar placeholder informativo
    if (fieldName) {
      const placeholderKey = `perfil.placeholders.${fieldName}`;
      const placeholder = this.translateService.instant(placeholderKey);
      // Verificar si la traducción existe (si no existe, instant retorna la clave)
      return placeholder !== placeholderKey ? placeholder : '-';
    }

    return '-';
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

  private buscarMalla(carrera: string, universidad: string): MallaResponse | undefined {
    const carreraNormalizada = carrera.toLowerCase();
    const universidadNormalizada = universidad.toLowerCase();

    return this.mallasDisponibles.find((malla) => {
      return (
        malla.carrera.trim().toLowerCase() === carreraNormalizada &&
        malla.universidad.trim().toLowerCase() === universidadNormalizada
      );
    });
  }
}
