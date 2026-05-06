import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { ChangePasswordRequest, MallaResponse, PerfilResponse, PerfilUpdateRequest } from './perfil.model';

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

  updatePerfil(currentUsername: string, payload: PerfilUpdateRequest): Observable<PerfilResponse> {
    const encodedUsername = encodeURIComponent(currentUsername);
    return this.apiService.put<PerfilResponse, PerfilUpdateRequest>(`/api/estudiantes/perfil/${encodedUsername}`, payload);
  }

  changePassword(payload: ChangePasswordRequest): Observable<{ message: string }> {
    return this.apiService.post<{ message: string }, ChangePasswordRequest>('/api/change-password', payload);
  }

  completeTour(username: string): Observable<void> {
    const encodedUsername = encodeURIComponent(username);
    return this.apiService.post<void, {}>(`/api/estudiantes/perfil/${encodedUsername}/complete-tour`, {});
  }
}
