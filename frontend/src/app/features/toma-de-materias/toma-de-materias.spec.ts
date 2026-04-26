import { of } from 'rxjs';
import { TomaDeMaterias } from './toma-de-materias';
import { HorarioActualService } from '../../services/academico/horario-actual.service';
import { ApiService } from '../../services/api.service';
import { TomaSeleccionService } from '../../services/academico/toma-seleccion.service';

describe('TomaDeMaterias', () => {
  let component: TomaDeMaterias;
  let horarioActualServiceSpy: jasmine.SpyObj<HorarioActualService>;
  let apiServiceSpy: jasmine.SpyObj<ApiService>;
  let tomaSeleccionServiceSpy: jasmine.SpyObj<TomaSeleccionService>;

  beforeEach(() => {
    horarioActualServiceSpy = jasmine.createSpyObj('HorarioActualService', ['getHorarioActual']);
    horarioActualServiceSpy.getHorarioActual.and.returnValue(of({ clases: [] }));

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
