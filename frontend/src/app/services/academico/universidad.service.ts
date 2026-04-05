import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface UniversidadCatalogoItem {
  id: number;
  nombre: string;
  codigo: string;
}

@Injectable({
  providedIn: 'root',
})
export class UniversidadService {
  constructor(private readonly apiService: ApiService) {}

  getUniversidadesActivas(): Observable<UniversidadCatalogoItem[]> {
    return this.apiService.get<UniversidadCatalogoItem[]>('/api/academico/universidades');
  }
}
