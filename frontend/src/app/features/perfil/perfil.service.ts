import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../services/api.service';
import { PerfilResponse } from './perfil.model';

@Injectable({
  providedIn: 'root',
})
export class PerfilService {
  constructor(private readonly apiService: ApiService) {}

  getPerfilByUsername(username: string): Observable<PerfilResponse> {
    const encodedUsername = encodeURIComponent(username);
    return this.apiService.get<PerfilResponse>(`/api/estudiantes/perfil/${encodedUsername}`);
  }
}
