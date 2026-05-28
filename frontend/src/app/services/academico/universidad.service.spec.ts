import { of } from 'rxjs';

import { ApiService } from '../api.service';
import { UniversidadRequest, UniversidadService } from './universidad.service';

describe('UniversidadService', () => {
  let service: UniversidadService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post']);
    service = new UniversidadService(apiServiceSpy);
  });

  it('calls universidades endpoint with GET', () => {
    apiServiceSpy.get.and.returnValue(of([]));

    service.getUniversidadesActivas().subscribe();

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/api/academico/universidades');
  });

  it('calls createUniversidad endpoint with POST', () => {
    const request: UniversidadRequest = { nombre: 'Nueva Universidad', codigo: 'NU' };
    apiServiceSpy.post.and.returnValue(of({ id: 1, nombre: 'Nueva Universidad', codigo: 'NU' }));

    service.createUniversidad(request).subscribe();

    expect(apiServiceSpy.post).toHaveBeenCalledWith('/api/academico/universidades', request);
  });
});
