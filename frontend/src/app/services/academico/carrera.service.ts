import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface CarreraCatalogoItem {
  id: number;
  universidadId: number;
  nombre: string;
  codigo: string;
}

@Injectable({
  providedIn: 'root',
})
export class CarreraService {
  constructor(private readonly apiService: ApiService) {}

  getCarrerasActivasPorUniversidad(universidadId: number): Observable<CarreraCatalogoItem[]> {
    return this.apiService.get<CarreraCatalogoItem[]>(`/api/academico/carreras?universidadId=${universidadId}`);
  }
}
