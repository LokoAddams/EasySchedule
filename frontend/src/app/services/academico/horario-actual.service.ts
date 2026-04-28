import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpResponse } from '@angular/common/http';

import { ApiService } from '../api.service';

export interface HorarioClase {
  materia: string;
  paralelo: string | null;
  dia: string;
  horaInicio: string;
  horaFin: string;
  docente: string | null;
  aula: string | null;
  creditos: number | null;
}

export interface HorarioActualResponse {
  universidad: string | null;
  carrera: string | null;
  malla: string | null;
  semestreOferta: string | null;
  semestreActual: number | null;
  clases: HorarioClase[];
}

@Injectable({
  providedIn: 'root',
})
export class HorarioActualService {
  constructor(private readonly apiService: ApiService) {}

  getHorarioActual(): Observable<HorarioActualResponse> {
    return this.apiService.get<HorarioActualResponse>('/api/academico/horario/actual');
  }

  exportHorarioActualCsv(estudianteId: number): Observable<HttpResponse<Blob>> {
    const path = `/api/academico/horario/actual/${estudianteId}/export?formato=csv`;
    return this.apiService.getBlob(path);
  }

  exportHorarioActualPdf(estudianteId: number): Observable<HttpResponse<Blob>> {
    const path = `/api/academico/horario/actual/${estudianteId}/export?formato=pdf`;
    return this.apiService.getBlob(path);
  }

  exportHorarioActualImage(estudianteId: number): Observable<HttpResponse<Blob>> {
    const path = `/api/academico/horario/actual/${estudianteId}/export?formato=imagen`;
    return this.apiService.getBlob(path);
  }
}
