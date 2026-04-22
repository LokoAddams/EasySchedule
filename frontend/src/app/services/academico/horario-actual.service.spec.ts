import { of } from 'rxjs';

import { ApiService } from '../api.service';
import { HorarioActualService } from './horario-actual.service';

describe('HorarioActualService', () => {
  let service: HorarioActualService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get']);
    service = new HorarioActualService(apiServiceSpy);
  });

  it('calls horario actual endpoint', () => {
    apiServiceSpy.get.and.returnValue(of({
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: null,
      clases: [],
    }));

    service.getHorarioActual().subscribe();

    expect(apiServiceSpy.get).toHaveBeenCalledWith('/api/academico/horario/actual');
  });
});
