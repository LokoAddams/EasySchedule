import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface OfertaImportErrorResponse {
  rowNumber: number;
  field: string;
  reason: string;
  critical: boolean;
}

export interface OfertaImportWarningResponse {
  rowNumber: number;
  field: string;
  reason: string;
}

export interface OfertaImportHorarioResponse {
  rowNumber: number;
  dia: string;
  horaInicio: string;
  horaFin: string;
}

export interface OfertaImportPreviewResponse {
  codigoMateria: string;
  nombreMateria: string;
  mallaMateriaId: number;
  paralelo: string;
  semestreAcademico: string;
  docente: string | null;
  aula: string | null;
  horarios: OfertaImportHorarioResponse[];
}

export interface OfertaImportSummaryResponse {
  totalRows: number;
  offersCreated: number;
  offersUpdated: number;
  scheduleBlocks: number;
  skippedRows: number;
  errorsCount: number;
  warningsCount: number;
}

export interface OfertaImportResultResponse {
  summary: OfertaImportSummaryResponse;
  offers: OfertaImportPreviewResponse[];
  errors: OfertaImportErrorResponse[];
  warnings: OfertaImportWarningResponse[];
}

@Injectable({
  providedIn: 'root',
})
export class OfertaImportService {
  constructor(private readonly apiService: ApiService) {}

  importarOfertas(mallaId: number, file: File): Observable<OfertaImportResultResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.apiService.post<OfertaImportResultResponse, FormData>(
      `/api/academico/ofertas/importar?mallaId=${mallaId}`,
      formData,
    );
  }
}