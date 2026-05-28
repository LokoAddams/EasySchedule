import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../api.service';

export interface MateriaDisponible {
  id: number;
  materiaId: number;
  codigoMateria: string;
  nombreMateria: string;
  semestreSugerido: number;
  estado: string | null;
  prerequisitosIds: number[];
}

@Injectable({
  providedIn: 'root',
})
export class MateriasDisponiblesService {
  constructor(private readonly apiService: ApiService) {}

  getMateriasDisponibles(mallaId: number, userId: number): Observable<MateriaDisponible[]> {
    return this.apiService.get<MateriaDisponible[]>(
      `/api/materias/disponibles?mallaId=${mallaId}&userId=${userId}`
    );
  }
}
