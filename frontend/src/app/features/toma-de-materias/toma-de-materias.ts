import { NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import {
  HorarioActualResponse,
  HorarioActualService,
  HorarioClase,
} from '../../services/academico/horario-actual.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ApiService } from '../../services/api.service';
import { TomaSeleccionService } from '../../services/academico/toma-seleccion.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { PerfilService } from '../perfil/perfil.service';
import { MallaCatalogoService, MallaMateria } from '../../services/academico/malla-catalogo.service';

export interface MateriaSeleccionada {
  id: number;
  nombre: string;
  creditos: number;
  ofertaId: number;
}

@Component({
  selector: 'app-toma-de-materias',
  imports: [NgIf, NgFor, FormsModule, TranslatePipe],
  templateUrl: './toma-de-materias.html',
  styleUrl: './toma-de-materias.scss',
})
export class TomaDeMaterias implements OnInit {
  protected readonly dias = ['Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado'];
  protected readonly timeRowsDefault = [
    '07:00 - 08:30',
    '08:45 - 10:15',
    '10:30 - 12:00',
    '12:15 - 13:45',
    '14:00 - 15:30',
    '15:45 - 17:15',
    '17:30 - 19:00',
    '19:15 - 20:45',
  ];

  protected horario: HorarioActualResponse | null = null;
  protected loading = true;
  protected error = false;
  protected exportLoading = false;
  protected exportFormat = 'csv';
  protected exportError = '';
  protected exportInfo = '';
  protected timeRows: string[] = [...this.timeRowsDefault];
  protected cellMap = new Map<string, HorarioClase[]>();

  protected materiasSeleccionadas: MateriaSeleccionada[] = [];
  protected totalCreditosSeleccionados = 0;
  protected creditosConfirmados = 0;
  protected totalCreditosHorario = 0;
  protected submitLoading = false;
  protected submitError = '';
  protected submitSuccess = '';

  private estudianteId: number | null = null;

  constructor(
    private readonly horarioActualService: HorarioActualService,
    private readonly apiService: ApiService,
    private readonly tomaSeleccionService: TomaSeleccionService,
    private readonly authSessionService: AuthSessionService,
    private readonly perfilService: PerfilService,
    private readonly translateService: TranslateService,
    private readonly mallaCatalogoService: MallaCatalogoService,
  ) {}

  // Estadísticas de malla
  protected totalMateriasMalla = 0;
  protected materiasAprobadas = 0;
  protected materiasCursando = 0;
  protected materiasPendientes = 0;
  protected porcentajeCompletado = 0;
  protected porcentajeAnimated = 0;

  ngOnInit(): void {
    this.cargarHorarioYSelecciones();

    this.tomaSeleccionService.seleccion$.subscribe(materias => {
      this.materiasSeleccionadas = materias;
      this.calcularTotalCreditos();
    });
  }

  private calcularTotalCreditos(): void {
    const creditosEnCesta = this.materiasSeleccionadas.reduce((sum, m) => sum + (Number(m.creditos) || 0), 0);
    this.totalCreditosSeleccionados = this.creditosConfirmados + creditosEnCesta;
  }

  private calcularCreditosHorario(): void {
    // Calcular créditos totales del horario actual (sin duplicados de materias)
    const materiasSet = new Set<string>();
    let totalCreditos = 0;

    if (this.horario && this.horario.clases && this.horario.clases.length > 0) {
      for (const clase of this.horario.clases) {
        if (!materiasSet.has(clase.materia)) {
          materiasSet.add(clase.materia);
          totalCreditos += clase.creditos || 0;
        }
      }
    }

    this.totalCreditosHorario = totalCreditos;
  }

  private loadMallaProgress(mallaId: number | null): void {
    // reset
    this.totalMateriasMalla = 0;
    this.materiasAprobadas = 0;
    this.materiasCursando = 0;
    this.materiasPendientes = 0;

    if (!mallaId) {
      return;
    }

    this.mallaCatalogoService.getMateriasPorMalla(mallaId).subscribe({
      next: (materias: MallaMateria[]) => {
        this.totalMateriasMalla = materias.length;
        this.materiasAprobadas = materias.filter(m => m.estado === 'aprobada').length;
        this.materiasCursando = materias.filter(m => m.estado === 'cursando').length;
        this.materiasPendientes = materias.filter(m => m.estado === 'pendiente' || m.estado === null).length;
        this.porcentajeCompletado = this.totalMateriasMalla > 0 ? Math.round((this.materiasAprobadas / this.totalMateriasMalla) * 100) : 0;
        // animate from 0 to porcentajeCompletado
        this.porcentajeAnimated = 0;
        setTimeout(() => (this.porcentajeAnimated = this.porcentajeCompletado), 50);
      },
      error: () => {
        // leave zeros on error
      }
    });
  }

  protected removerMateria(id: number): void {
    const confirmacion = window.confirm(this.translateService.instant('tomaMaterias.confirm.removeSubject'));
    if (confirmacion) {
      this.tomaSeleccionService.removerMateria(id);
    }
  }

  protected confirmarInscripcion(): void {
    if (this.submitLoading) {
      return;
    }

    this.submitError = '';
    this.submitSuccess = '';

    const ofertaIds = this.buildOfertaIds();
    if (ofertaIds.length === 0) {
      this.submitError = this.translateService.instant('tomaMaterias.messages.noValidGroups');
      return;
    }

    const body = { ofertaIds };
    this.submitLoading = true;

    this.apiService.post('/api/academico/toma-materias', body).subscribe({
      next: () => {
        this.submitLoading = false;
        this.submitSuccess = this.translateService.instant('tomaMaterias.messages.registrationSuccess');
        const creditosNuevos = this.materiasSeleccionadas.reduce((sum, m) => sum + (Number(m.creditos) || 0), 0);
        this.creditosConfirmados += creditosNuevos;

        this.tomaSeleccionService.limpiar();
        this.cargarHorarioYSelecciones();
      },
      error: (err: HttpErrorResponse) => {
        this.submitLoading = false;
        this.submitError = this.extractApiErrorMessage(err);
      }
    });
  }

  private buildOfertaIds(): number[] {
    const ids = this.materiasSeleccionadas
      .map(m => Number(m.ofertaId))
      .filter(id => Number.isFinite(id) && id > 0);

    return Array.from(new Set(ids));
  }

  private extractApiErrorMessage(error: HttpErrorResponse): string {
    const backendMessage = this.extractBackendMessage(error);
    if (backendMessage) {
      return backendMessage;
    }

    return this.translateService.instant('tomaMaterias.messages.unexpectedRegistrationError');
  }

  private extractBackendMessage(error: HttpErrorResponse): string {
    if (!error?.error) {
      return '';
    }

    const rawError = error.error as { message?: unknown; error?: unknown } | string;

    let message = '';
    if (typeof rawError === 'string') {
      message = rawError;
    } else if (typeof rawError.message === 'string') {
      message = rawError.message;
    } else if (typeof rawError.error === 'string') {
      message = rawError.error;
    } else if (rawError.error && typeof rawError.error === 'object') {
      const nestedError = rawError.error as { message?: unknown };
      if (typeof nestedError.message === 'string') {
        message = nestedError.message;
      }
    }

    const normalized = message.trim();
    if (!normalized) {
      return '';
    }

    const lowerCaseMessage = normalized.toLowerCase();
    if (
      lowerCaseMessage === 'error interno del servidor' ||
      lowerCaseMessage === 'internal server error' ||
      lowerCaseMessage === 'error en la solicitud' ||
      lowerCaseMessage.includes('unexpected error')
    ) {
      return '';
    }

    return normalized;
  }

  private cargarHorarioYSelecciones(): void {
    this.loading = true;
    this.error = false;
    this.horarioActualService.getHorarioActual().subscribe({
      next: (horario) => {
        this.horario = horario;
        this.buildGrid(horario.clases ?? []);
        this.calcularCreditosHorario();
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      },
    });

    this.loadEstudianteId();
  }

  protected exportHorario(): void {
    this.exportError = '';
    this.exportInfo = '';

    if (this.exportLoading) {
      return;
    }

    if (!this.horario || !this.horario.clases || this.horario.clases.length === 0) {
      this.exportInfo = this.translateService.instant('tomaMaterias.messages.noScheduleToExport');
      return;
    }

    if (!['csv', 'pdf', 'imagen'].includes(this.exportFormat)) {
      this.exportError = this.translateService.instant('tomaMaterias.messages.unsupportedFormat');
      return;
    }

    if (this.estudianteId) {
      this.requestExport(this.estudianteId, this.exportFormat);
      return;
    }

    const username = this.authSessionService.getCurrentUsername();
    if (!username) {
      this.exportError = this.translateService.instant('tomaMaterias.messages.studentSessionNotFound');
      return;
    }

    this.exportLoading = true;
    this.perfilService.getPerfilByUsername(username).subscribe({
      next: (perfil) => {
        this.estudianteId = perfil.id;
        this.requestExport(perfil.id, this.exportFormat);
      },
      error: () => {
        this.exportLoading = false;
        this.exportError = this.translateService.instant('tomaMaterias.messages.studentNotFound');
      },
    });
  }

  protected getCellItems(timeRow: string, dia: string): HorarioClase[] {
    return this.cellMap.get(this.cellKey(timeRow, dia)) ?? [];
  }

  protected getClasesPorDia(dia: string): HorarioClase[] {
    if (!this.horario?.clases) return [];
    return this.horario.clases
      .filter(c => this.normalizeDay(c.dia) === dia)
      .sort((a, b) => (a.horaInicio ?? '').localeCompare(b.horaInicio ?? ''));
  }

  private requestExport(estudianteId: number, formato: string): void {
    this.exportLoading = true;
    const exportRequest = formato === 'pdf'
      ? this.horarioActualService.exportHorarioActualPdf(estudianteId)
      : (formato === 'imagen'
        ? this.horarioActualService.exportHorarioActualImage(estudianteId)
        : this.horarioActualService.exportHorarioActualCsv(estudianteId));

    exportRequest.subscribe({
      next: (response) => {
        this.exportLoading = false;
        this.triggerDownload(response.body, this.resolveFilename(response.headers.get('Content-Disposition')));
      },
      error: (error: HttpErrorResponse) => {
        this.exportLoading = false;
        if (error.status === 404) {
          this.exportInfo = this.translateService.instant('tomaMaterias.messages.noScheduleToExport');
          return;
        }
        this.exportError = this.translateService.instant('tomaMaterias.messages.exportError');
      },
    });
  }

  private resolveFilename(contentDisposition: string | null): string {
    if (!contentDisposition) {
      return this.defaultFilenameByFormat();
    }

    const filenameMatch = /filename="?([^";]+)"?/i.exec(contentDisposition);
    if (!filenameMatch || !filenameMatch[1]) {
      return this.defaultFilenameByFormat();
    }

    return filenameMatch[1].trim() || this.defaultFilenameByFormat();
  }

  private defaultFilenameByFormat(): string {
    if (this.exportFormat === 'pdf') {
      return 'horario.pdf';
    }
    if (this.exportFormat === 'imagen') {
      return 'horario.png';
    }
    return 'horario.csv';
  }

  private triggerDownload(blob: Blob | null, filename: string): void {
    if (!blob) {
      this.exportError = this.translateService.instant('tomaMaterias.messages.fileGenerationError');
      return;
    }

    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    window.URL.revokeObjectURL(url);
  }

  private buildGrid(clases: HorarioClase[]): void {
    this.cellMap.clear();
    const rowsFromData = new Set<string>();

    for (const clase of clases) {
      const diaNorm = this.normalizeDay(clase.dia);
      if (!diaNorm) {
        continue;
      }

      const timeRow = `${clase.horaInicio} - ${clase.horaFin}`;
      rowsFromData.add(timeRow);

      const key = this.cellKey(timeRow, diaNorm);
      const current = this.cellMap.get(key) ?? [];
      current.push(clase);
      this.cellMap.set(key, current);
    }

    const finalRows = rowsFromData.size > 0 ? Array.from(rowsFromData) : [...this.timeRowsDefault];
    this.timeRows = finalRows.sort((a, b) => {
      const aStart = a.split(' - ')[0] ?? '';
      const bStart = b.split(' - ')[0] ?? '';
      return aStart.localeCompare(bStart);
    });
  }

  private normalizeDay(day: string): string | null {
    const normalized = (day ?? '')
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    switch (normalized) {
      case 'lunes':
        return 'Lunes';
      case 'martes':
        return 'Martes';
      case 'miercoles':
        return 'Miercoles';
      case 'jueves':
        return 'Jueves';
      case 'viernes':
        return 'Viernes';
      case 'sabado':
        return 'Sabado';
      default:
        return null;
    }
  }

  private cellKey(timeRow: string, dia: string): string {
    return `${timeRow}|${dia}`;
  }

  private loadEstudianteId(): void {
    const username = this.authSessionService.getCurrentUsername();
    if (!username) {
      return;
    }

    this.perfilService.getPerfilByUsername(username).subscribe({
      next: (perfil) => {
        this.estudianteId = perfil.id;
        this.loadMallaProgress(perfil.mallaId ?? null);
      },
      error: () => {
        this.estudianteId = null;
      },
    });
  }
}
