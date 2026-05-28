import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface CarreraCatalogoItem {
  id: number;
  universidadId: number;
  nombre: string;
  codigo: string;
}

export interface CarreraRequest {
  universidadId: number;
  nombre: string;
  codigo: string;
  active?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class CarreraService {
  constructor(private readonly apiService: ApiService) {}

  getCarrerasActivasPorUniversidad(universidadId: number): Observable<CarreraCatalogoItem[]> {
    return this.apiService.get<CarreraCatalogoItem[]>(`/api/academico/carreras?universidadId=${universidadId}`);
  }

  createCarrera(request: CarreraRequest): Observable<CarreraCatalogoItem> {
    return this.apiService.post<CarreraCatalogoItem, CarreraRequest>('/api/academico/carreras', request);
  }
}
