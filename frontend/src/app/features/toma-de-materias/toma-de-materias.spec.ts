import { of } from 'rxjs';

import { TomaDeMaterias } from './toma-de-materias';
import { HorarioActualService } from '../../services/academico/horario-actual.service';

describe('TomaDeMaterias component logic', () => {
  let component: TomaDeMaterias;
  let horarioActualServiceSpy: jasmine.SpyObj<HorarioActualService>;

  beforeEach(() => {
    horarioActualServiceSpy = jasmine.createSpyObj<HorarioActualService>('HorarioActualService', ['getHorarioActual']);
    component = new TomaDeMaterias(horarioActualServiceSpy);
  });

  it('loads schedule and builds rows from backend data', () => {
    horarioActualServiceSpy.getHorarioActual.and.returnValue(of({
      universidad: 'Universidad Catolica Boliviana',
      carrera: 'Ingenieria de Sistemas',
      malla: 'Malla 2024',
      semestreOferta: '2026-1',
      semestreActual: 1,
      clases: [
        {
          materia: 'Materia SIS S1 M1',
          paralelo: 'A',
          dia: 'Lunes',
          horaInicio: '07:00',
          horaFin: '08:30',
          docente: 'Docente',
          aula: 'A-01-1',
        },
      ],
    }));

    component.ngOnInit();

    expect((component as any).loading).toBeFalse();
    expect((component as any).error).toBeFalse();
    expect((component as any).timeRows).toEqual(['07:00 - 08:30']);
    expect((component as any).getCellItems('07:00 - 08:30', 'Lunes').length).toBe(1);
  });
});
