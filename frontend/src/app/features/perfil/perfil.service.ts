import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { MallaResponse, PerfilResponse, PerfilUpdateRequest } from './perfil.model';

@Injectable({
	providedIn: 'root',
})
export class PerfilService {
	constructor(private readonly apiService: ApiService) {}

	getPerfilByUsername(username: string): Observable<PerfilResponse> {
		const encodedUsername = encodeURIComponent(username);
		return this.apiService.get<PerfilResponse>(`/api/estudiantes/perfil/${encodedUsername}`);
	}

	getMallas(): Observable<MallaResponse[]> {
		return this.apiService.get<MallaResponse[]>('/api/mallas');
	}

	updatePerfil(estudianteId: number, payload: PerfilUpdateRequest): Observable<PerfilResponse> {
		return this.apiService.put<PerfilResponse, PerfilUpdateRequest>(`/api/estudiantes/${estudianteId}`, payload);
	}
}
