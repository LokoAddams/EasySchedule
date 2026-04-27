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

export interface OfertaMateriaSimple {
  id: number;
  semestre: string;
  paralelo: string;
  docente: string;
  aula: string;
}

export interface OfertaDetalleResponse {
  mallaMateriaId: number;
  nombreMateria: string;
  creditos: number;
  prerequisitos: string[];
  gruposDisponibles: OfertaMateriaSimple[];
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

   getDetallesMateria(mallaMateriaId: number): Observable<OfertaDetalleResponse> {
    return this.apiService.get<OfertaDetalleResponse>(`/api/academico/ofertas/detalles/${mallaMateriaId}`);
  }

}
