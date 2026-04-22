import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface MallaCatalogoItem {
  id: number;
  carreraId: number;
  nombre: string;
  version: string;
  active: boolean;
}

export interface MallaMateria {
  id: number;
  materiaId: number;
  codigoMateria: string;
  nombreMateria: string;
  semestreSugerido: number;
  estado: 'aprobada' | 'cursando' | 'pendiente' | null;
}

@Injectable({
  providedIn: 'root',
})
export class MallaCatalogoService {
  constructor(private readonly apiService: ApiService) {}

  getMallasActivasPorCarrera(carreraId: number): Observable<MallaCatalogoItem[]> {
    return this.apiService.get<MallaCatalogoItem[]>(`/api/academico/mallas?carreraId=${carreraId}`);
  }

  getMateriasPorMalla(mallaId: number): Observable<MallaMateria[]> {
    return this.apiService.get<MallaMateria[]>(`/api/academico/mallas/${mallaId}/materias`);
  }
}
