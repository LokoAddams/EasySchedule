import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
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
  creditos: number;
  semestreSugerido: number;
  estado: 'aprobada' | 'cursando' | 'pendiente' | null;
  prerequisitosIds: number[];
}

export interface MallaEditableMateria {
  codigo: string;
  nombre: string;
  semestre: number;
  creditos: number;
  prerequisitos: string[];
}

export interface MallaEditableResponse {
  mallaId: number;
  nombre: string;
  version: string;
  carreraId: number;
  materias: MallaEditableMateria[];
}

export interface MallaEditRequest {
  materias: MallaEditableMateria[];
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

  getMallaEditable(mallaId: number): Observable<MallaEditableResponse> {
    return this.apiService.get<MallaEditableResponse>(`/api/academico/mallas/${mallaId}/edicion`);
  }

  actualizarMallaEditable(mallaId: number, request: MallaEditRequest): Observable<MallaEditableResponse> {
    return this.apiService.put<MallaEditableResponse, MallaEditRequest>(`/api/academico/mallas/${mallaId}/edicion`, request);
  }

  actualizarMallaDesdeArchivo(mallaId: number, file: File): Observable<MallaEditableResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiService.put<MallaEditableResponse, FormData>(`/api/academico/mallas/${mallaId}/edicion/importar`, formData);
  }

  exportarAvanceGraduacion(): Observable<HttpResponse<Blob>> {
    return this.apiService.getBlob('/api/estudiantes/me/avance-graduacion/export');
  }

  getDetallesMateria(mallaMateriaId: number): Observable<OfertaDetalleResponse> {
    return this.apiService.get<OfertaDetalleResponse>(`/api/academico/ofertas/detalles/${mallaMateriaId}`);
  }

  crearPrerequisito(mallaMateriaId: number, prerequisitoMallaMateriaId: number): Observable<any> {
    return this.apiService.post<any, any>(`/api/academico/prerequisitos`, {
      mallaMateriaId,
      prerequisitoMallaMateriaId,
    });
  }

  eliminarPrerequisito(mallaMateriaId: number, prerequisitoMallaMateriaId: number): Observable<any> {
    return this.apiService.delete<any>(
      `/api/academico/prerequisitos?mallaMateriaId=${mallaMateriaId}&prerequisitoMallaMateriaId=${prerequisitoMallaMateriaId}`
    );
  }
}
