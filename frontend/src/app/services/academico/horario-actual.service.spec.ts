import { of } from 'rxjs';

import { ApiService } from '../api.service';
import { HorarioActualService } from './horario-actual.service';

describe('HorarioActualService', () => {
  let service: HorarioActualService;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    apiServiceSpy = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'getBlob']);
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

  it('calls export csv endpoint', () => {
    apiServiceSpy.getBlob.and.returnValue(of(null as any));

    service.exportHorarioActualCsv(7).subscribe();

    expect(apiServiceSpy.getBlob).toHaveBeenCalledWith('/api/academico/horario/actual/7/export?formato=csv');
  });

  it('calls export pdf endpoint', () => {
    apiServiceSpy.getBlob.and.returnValue(of(null as any));

    service.exportHorarioActualPdf(7).subscribe();

    expect(apiServiceSpy.getBlob).toHaveBeenCalledWith('/api/academico/horario/actual/7/export?formato=pdf');
  });
});
