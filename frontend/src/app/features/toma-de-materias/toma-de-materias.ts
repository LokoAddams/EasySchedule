import { NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import {
  HorarioActualResponse,
  HorarioActualService,
  HorarioClase,
} from '../../services/academico/horario-actual.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { PerfilService } from '../perfil/perfil.service';

@Component({
  selector: 'app-toma-de-materias',
  imports: [NgIf, NgFor, FormsModule],
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

  private estudianteId: number | null = null;

  constructor(
    private readonly horarioActualService: HorarioActualService,
    private readonly authSessionService: AuthSessionService,
    private readonly perfilService: PerfilService,
  ) {}

  ngOnInit(): void {
    this.horarioActualService.getHorarioActual().subscribe({
      next: (horario) => {
        this.horario = horario;
        this.buildGrid(horario.clases ?? []);
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
      this.exportInfo = 'No hay horario disponible para exportar.';
      return;
    }

    if (!['csv', 'pdf', 'imagen'].includes(this.exportFormat)) {
      this.exportError = 'Formato no soportado por el momento.';
      return;
    }

    if (this.estudianteId) {
      this.requestExport(this.estudianteId, this.exportFormat);
      return;
    }

    const username = this.authSessionService.getCurrentUsername();
    if (!username) {
      this.exportError = 'No se pudo identificar la sesion del estudiante.';
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
        this.exportError = 'No se pudo identificar al estudiante.';
      },
    });
  }

  protected getCellItems(timeRow: string, dia: string): HorarioClase[] {
    return this.cellMap.get(this.cellKey(timeRow, dia)) ?? [];
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
          this.exportInfo = 'No hay horario disponible para exportar.';
          return;
        }
        this.exportError = 'Ocurrio un error al exportar el horario. Intenta nuevamente.';
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
      this.exportError = 'No se pudo generar el archivo.';
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
      },
      error: () => {
        this.estudianteId = null;
      },
    });
  }
}
