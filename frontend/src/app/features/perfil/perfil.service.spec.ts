import { of } from 'rxjs';

import { PerfilService } from './perfil.service';
import { ApiService } from '../../services/api.service';

describe('PerfilService', () => {
  let service: PerfilService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'put', 'post']);
    service = new PerfilService(apiServiceSpy);
  });

  it('calls get profile endpoint with encoded username', () => {
    apiServiceSpy.get.and.returnValue(of({} as never));

    service.getPerfilByUsername('juan perez').subscribe();

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/api/estudiantes/perfil/juan%20perez');
  });

  it('calls update profile endpoint with encoded username', () => {
    apiServiceSpy.put.and.returnValue(of({} as never));

    const payload = {
      username: 'juan',
      nombre: 'Juan',
      apellido: 'Perez',
      email: 'juan@mail.com',
      carnetIdentidad: '123',
      fechaNacimiento: '2000-01-01',
      carrera: '',
      universidad: '',
    };

    service.updatePerfil('juan perez', payload).subscribe();

    expect(apiServiceSpy.put).toHaveBeenCalledWith('/api/estudiantes/perfil/juan%20perez', payload);
  });

  it('calls change password endpoint', () => {
    apiServiceSpy.post.and.returnValue(of({ message: 'ok' } as never));

    const payload = {
      currentPassword: 'oldPass123',
      newPassword: 'newPass1234',
      confirmNewPassword: 'newPass1234',
    };

    service.changePassword(payload).subscribe();

    expect(apiServiceSpy.post).toHaveBeenCalledWith('/api/change-password', payload);
  });
});
