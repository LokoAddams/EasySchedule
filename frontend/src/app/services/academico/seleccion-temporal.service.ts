import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

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
  private readonly apiUrl = `${environment.apiUrl}/api/academico/seleccion-temporal`;

  constructor(private http: HttpClient) {}

  listarSelecciones(): Observable<SeleccionTemporalResponse[]> {
    return this.http.get<SeleccionTemporalResponse[]>(this.apiUrl);
  }

  agregarSeleccion(request: SeleccionTemporalRequest): Observable<SeleccionTemporalResponse> {
    return this.http.post<SeleccionTemporalResponse>(this.apiUrl, request);
  }

  removerSeleccion(ofertaMateriaId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${ofertaMateriaId}`);
  }

  limpiarSelecciones(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clear`);
  }
}
