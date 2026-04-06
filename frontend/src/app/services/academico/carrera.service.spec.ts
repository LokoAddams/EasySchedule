import { of } from 'rxjs';

import { ApiService } from '../api.service';
import { CarreraService } from './carrera.service';

describe('CarreraService', () => {
  let service: CarreraService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get']);
    service = new CarreraService(apiServiceSpy);
  });

  it('calls carreras endpoint with universidadId query parameter', () => {
    apiServiceSpy.get.and.returnValue(of([]));

    service.getCarrerasActivasPorUniversidad(7).subscribe();

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/api/academico/carreras?universidadId=7');
  });
});
