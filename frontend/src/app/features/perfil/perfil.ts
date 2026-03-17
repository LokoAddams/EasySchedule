import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/services/auth-session.service';
import { LanguageService } from '../../core/services/language.service';
import { MallaResponse, PerfilResponse, PerfilUpdateRequest } from './perfil.model';
import { PerfilService } from './perfil.service';

type PerfilEditForm = FormGroup<{
  username: FormControl<string>;
  nombre: FormControl<string>;
  apellido: FormControl<string>;
  email: FormControl<string>;
  carnetIdentidad: FormControl<string>;
  fechaNacimiento: FormControl<string>;
  carrera: FormControl<string>;
  universidad: FormControl<string>;
}>;

@Component({
  selector: 'app-perfil',
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil implements OnInit {
  protected perfil: PerfilResponse | null = null;
  protected editMode = false;
  protected loading = true;
  protected saving = false;
  protected errorKey = '';
  protected successKey = '';
  protected readonly editForm: PerfilEditForm;

  private mallasDisponibles: MallaResponse[] = [];

  constructor(
    private readonly fb: FormBuilder,
    private readonly perfilService: PerfilService,
    private readonly authSessionService: AuthSessionService,
    private readonly languageService: LanguageService,
  ) {
    this.editForm = this.fb.nonNullable.group({
      username: ['', [Validators.required]],
      nombre: ['', [Validators.required]],
      apellido: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      carnetIdentidad: ['', [Validators.required]],
      fechaNacimiento: ['', [Validators.required]],
      carrera: ['', [Validators.required]],
      universidad: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.perfilService.getMallas().subscribe({
      next: (mallas) => {
        this.mallasDisponibles = mallas;
      },
      error: () => {
        this.mallasDisponibles = [];
      },
    });

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
      },
      error: () => {
        this.loading = false;
        this.errorKey = 'perfil.error.loadFailed';
      },
    });
  }

  protected activarEdicion(): void {
    if (!this.perfil) {
      return;
    }

    this.editMode = true;
    this.errorKey = '';
    this.successKey = '';
    this.cargarFormulario(this.perfil);
  }

  protected cancelarEdicion(): void {
    this.editMode = false;
    this.errorKey = '';
    this.successKey = '';

    if (this.perfil) {
      this.cargarFormulario(this.perfil);
    }
  }

  protected guardarEdicion(): void {
    this.successKey = '';
    this.errorKey = '';

    if (!this.perfil || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const carrera = this.editForm.controls.carrera.value.trim();
    const universidad = this.editForm.controls.universidad.value.trim();
    const mallaSeleccionada = this.buscarMalla(carrera, universidad);

    if (!mallaSeleccionada) {
      this.errorKey = 'perfil.error.mallaNotFound';
      return;
    }

    const updatePayload: PerfilUpdateRequest = {
      nombre: this.editForm.controls.nombre.value.trim(),
      apellido: this.editForm.controls.apellido.value.trim(),
      carnetIdentidad: this.editForm.controls.carnetIdentidad.value.trim(),
      fechaNacimiento: this.editForm.controls.fechaNacimiento.value,
      semestreActual: this.perfil.semestreActual ?? 1,
      carrera,
      mallaId: mallaSeleccionada.id,
    };

    this.saving = true;
    this.perfilService.updatePerfil(this.perfil.id, updatePayload).subscribe({
      next: (updatedPerfil) => {
        this.saving = false;
        this.editMode = false;
        this.perfil = {
          ...updatedPerfil,
          username: this.editForm.controls.username.value.trim(),
          email: this.editForm.controls.email.value.trim(),
          universidad,
        };
        this.successKey = 'perfil.success.updated';
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

  protected getValorSeguro(value: string | null): string {
    const valueNormalized = (value ?? '').trim();
    return valueNormalized || '-';
  }

  private cargarFormulario(perfil: PerfilResponse): void {
    this.editForm.patchValue({
      username: perfil.username ?? '',
      nombre: perfil.nombre ?? '',
      apellido: perfil.apellido ?? '',
      email: perfil.email ?? '',
      carnetIdentidad: perfil.carnetIdentidad ?? '',
      fechaNacimiento: perfil.fechaNacimiento ?? '',
      carrera: perfil.carrera ?? '',
      universidad: perfil.universidad ?? '',
    });
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
