import { of } from 'rxjs';
import { TomaDeMaterias } from './toma-de-materias';
import { HorarioActualService, HorarioActualResponse } from '../../services/academico/horario-actual.service';
import { ApiService } from '../../services/api.service';
import { TomaSeleccionService } from '../../services/academico/toma-seleccion.service';

describe('TomaDeMaterias', () => {
  let component: TomaDeMaterias;
  let horarioActualServiceSpy: jasmine.SpyObj<HorarioActualService>;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;
  let tomaSeleccionServiceSpy: jasmine.SpyObj<TomaSeleccionService>;

  beforeEach(() => {
    horarioActualServiceSpy = jasmine.createSpyObj('HorarioActualService', ['getHorarioActual']);

    const mockResponse: HorarioActualResponse = {
      universidad: 'Test',
      carrera: 'Test',
      malla: 'Test',
      semestreOferta: '2026-1',
      semestreActual: 1,
      clases: []
    };
    horarioActualServiceSpy.getHorarioActual.and.returnValue(of(mockResponse));

    apiServiceSpy = jasmine.createSpyObj('ApiService', ['post', 'get', 'put', 'delete']);

    tomaSeleccionServiceSpy = jasmine.createSpyObj('TomaSeleccionService', ['removerMateria', 'limpiar', 'agregarMateria']);
    Object.defineProperty(tomaSeleccionServiceSpy, 'seleccion$', { value: of([]) });

    component = new TomaDeMaterias(
      horarioActualServiceSpy,
      apiServiceSpy,
      tomaSeleccionServiceSpy
    );
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
