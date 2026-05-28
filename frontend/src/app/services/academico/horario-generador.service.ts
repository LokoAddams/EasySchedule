import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../api.service';
import { HorarioClase } from './horario-actual.service';

export interface MateriaSeleccionadaRequest {
  materiaId: number;
  paralelos: string[];
}

export interface HorarioGeneradorRequest {
  userId: number;
  mallaId: number;
  materiasSeleccionadas: MateriaSeleccionadaRequest[];
  prioridades: string[];
}

export interface HorarioGeneradoResponse {
  puntajeTotal: number;
  clases: HorarioClase[];
}

@Injectable({
  providedIn: 'root',
})
export class HorarioGeneradorService {
  constructor(private readonly apiService: ApiService) {}

  generarHorarios(request: HorarioGeneradorRequest): Observable<HorarioGeneradoResponse[]> {
    return this.apiService.post<HorarioGeneradoResponse[], HorarioGeneradorRequest>('/api/horarios/generar', request);
  }
}
