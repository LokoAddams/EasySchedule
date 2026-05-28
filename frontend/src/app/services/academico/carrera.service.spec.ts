import { of } from 'rxjs';

import { ApiService } from '../api.service';
import { CarreraRequest, CarreraService } from './carrera.service';

describe('CarreraService', () => {
  let service: CarreraService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post']);
    service = new CarreraService(apiServiceSpy);
  });

  it('calls carreras endpoint with universidadId query parameter', () => {
    apiServiceSpy.get.and.returnValue(of([]));

    service.getCarrerasActivasPorUniversidad(7).subscribe();

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/api/academico/carreras?universidadId=7');
  });

  it('calls createCarrera endpoint with POST', () => {
    const request: CarreraRequest = { universidadId: 1, nombre: 'Nueva Carrera', codigo: 'NC' };
    apiServiceSpy.post.and.returnValue(of({ id: 10, universidadId: 1, nombre: 'Nueva Carrera', codigo: 'NC' }));

    service.createCarrera(request).subscribe();

    expect(apiServiceSpy.post).toHaveBeenCalledWith('/api/academico/carreras', request);
  });
});
