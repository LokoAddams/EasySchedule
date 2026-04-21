import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface EstadoMateriaItem {
  id: number;
  mallaMateriaId: number;
  estado: 'aprobada' | 'pendiente' | 'cursando';
  fechaActualizacion: string;
}

export interface EstadoMateriaRequest {
  mallaMateriaId: number;
  estado: 'aprobada' | 'pendiente' | 'cursando';
}

@Injectable({
  providedIn: 'root',
})
export class EstadoMateriaService {
  constructor(private readonly apiService: ApiService) {}

  getEstadosPorMalla(mallaId: number): Observable<EstadoMateriaItem[]> {
    return this.apiService.get<EstadoMateriaItem[]>(`/api/academico/estados-materia/malla/${mallaId}`);
  }

  guardarEstado(request: EstadoMateriaRequest): Observable<EstadoMateriaItem> {
    return this.apiService.post<EstadoMateriaItem, EstadoMateriaRequest>('/api/academico/estados-materia', request);
  }

  guardarEstadosBatch(requests: EstadoMateriaRequest[]): Observable<EstadoMateriaItem[]> {
    return this.apiService.post<EstadoMateriaItem[], EstadoMateriaRequest[]>('/api/academico/estados-materia/batch', requests);
  }
}
