import { of } from 'rxjs';

import { ApiService } from '../api.service';
import { MallaCatalogoService } from './malla-catalogo.service';

describe('MallaCatalogoService', () => {
  let service: MallaCatalogoService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get']);
    service = new MallaCatalogoService(apiServiceSpy);
  });

  it('calls mallas endpoint with carreraId query parameter', () => {
    apiServiceSpy.get.and.returnValue(of([]));

    service.getMallasActivasPorCarrera(11).subscribe();

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/api/academico/mallas?carreraId=11');
  });
});
