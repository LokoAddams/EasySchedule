import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../api.service';

export interface SeleccionTemporalResponse {
  id: number;
  materiaId: number;
  materiaNombre: string;
  ofertaMateriaId: number;
  paralelo: string;
  docente: string;
  horarioJson: string;
  aula: string;
}

export interface SeleccionTemporalRequest {
  ofertaMateriaId: number;
}

@Injectable({
  providedIn: 'root'
})
export class SeleccionTemporalService {
  private readonly path = '/api/academico/seleccion-temporal';

  constructor(private apiService: ApiService) {}

  listarSelecciones(): Observable<SeleccionTemporalResponse[]> {
    return this.apiService.get<SeleccionTemporalResponse[]>(this.path);
  }

  agregarSeleccion(request: SeleccionTemporalRequest): Observable<SeleccionTemporalResponse> {
    return this.apiService.post<SeleccionTemporalResponse, SeleccionTemporalRequest>(this.path, request);
  }

  removerSeleccion(ofertaMateriaId: number): Observable<void> {
    return this.apiService.delete<void>(`${this.path}/${ofertaMateriaId}`);
  }

  limpiarSelecciones(): Observable<void> {
    return this.apiService.delete<void>(`${this.path}/clear`);
  }
}
