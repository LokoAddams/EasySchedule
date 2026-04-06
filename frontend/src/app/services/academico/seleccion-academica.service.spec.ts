import { of } from 'rxjs';

import { ApiService } from '../api.service';
import { SeleccionAcademicaService } from './seleccion-academica.service';

describe('SeleccionAcademicaService', () => {
  let service: SeleccionAcademicaService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'put']);
    service = new SeleccionAcademicaService(apiServiceSpy);
  });

  it('calls get current selection endpoint', () => {
    apiServiceSpy.get.and.returnValue(of({} as never));

    service.getSeleccionActual().subscribe();

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/api/academico/seleccion');
  });

  it('calls save selection endpoint with payload', () => {
    apiServiceSpy.put.and.returnValue(of({} as never));

    const payload = { universidadId: 1, carreraId: 2, mallaId: 3 };
    service.guardarSeleccion(payload).subscribe();

    expect(apiServiceSpy.put).toHaveBeenCalledWith('/api/academico/seleccion', payload);
  });
});
