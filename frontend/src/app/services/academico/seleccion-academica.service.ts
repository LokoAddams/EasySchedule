import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface SeleccionAcademica {
  universidadId: number | null;
  universidad: string | null;
  carreraId: number | null;
  carrera: string | null;
  mallaId: number | null;
  malla: string | null;
}

export interface SeleccionAcademicaRequest {
  universidadId: number;
  carreraId: number;
  mallaId: number;
}

@Injectable({
  providedIn: 'root',
})
export class SeleccionAcademicaService {
  constructor(private readonly apiService: ApiService) {}

  getSeleccionActual(): Observable<SeleccionAcademica> {
    return this.apiService.get<SeleccionAcademica>('/api/academico/seleccion');
  }

  guardarSeleccion(request: SeleccionAcademicaRequest): Observable<SeleccionAcademica> {
    return this.apiService.put<SeleccionAcademica, SeleccionAcademicaRequest>('/api/academico/seleccion', request);
  }
}
