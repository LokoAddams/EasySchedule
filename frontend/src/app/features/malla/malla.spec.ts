import { BehaviorSubject, of, throwError } from 'rxjs';

import { Malla } from './malla';
import { CarreraService } from '../../services/academico/carrera.service';
import { MallaCatalogoService } from '../../services/academico/malla-catalogo.service';
import { SeleccionAcademicaService } from '../../services/academico/seleccion-academica.service';
import { UniversidadService } from '../../services/academico/universidad.service';
import { FeatureToggleService, FeatureFlags } from '../../services/feature-toggle.service';

describe('Malla component logic', () => {
  let component: Malla;
  let flagsSubject: BehaviorSubject<FeatureFlags>;
  let featureServiceMock: jasmine.SpyObj<FeatureToggleService> & { flags$: BehaviorSubject<FeatureFlags> };
  let universidadServiceSpy: jasmine.SpyObj<UniversidadService>;
  let carreraServiceSpy: jasmine.SpyObj<CarreraService>;
  let mallaCatalogoServiceSpy: jasmine.SpyObj<MallaCatalogoService>;
  let seleccionAcademicaServiceSpy: jasmine.SpyObj<SeleccionAcademicaService>;

  beforeEach(() => {
    flagsSubject = new BehaviorSubject<FeatureFlags>({ malla: true, tomaMaterias: false });

    featureServiceMock = jasmine.createSpyObj<FeatureToggleService>('FeatureToggleService', ['loadFlags']) as any;
    featureServiceMock.flags$ = flagsSubject;
    featureServiceMock.loadFlags.and.returnValue(Promise.resolve());

    universidadServiceSpy = jasmine.createSpyObj<UniversidadService>('UniversidadService', ['getUniversidadesActivas']);
    carreraServiceSpy = jasmine.createSpyObj<CarreraService>('CarreraService', ['getCarrerasActivasPorUniversidad']);
    mallaCatalogoServiceSpy = jasmine.createSpyObj<MallaCatalogoService>('MallaCatalogoService', ['getMallasActivasPorCarrera']);
    seleccionAcademicaServiceSpy = jasmine.createSpyObj<SeleccionAcademicaService>('SeleccionAcademicaService', ['getSeleccionActual', 'guardarSeleccion']);

    component = new Malla(
      featureServiceMock,
      universidadServiceSpy,
      carreraServiceSpy,
      mallaCatalogoServiceSpy,
      seleccionAcademicaServiceSpy,
    );
  });

  it('sets required error when trying to save universidad without selection', () => {
    (component as any).onGuardarUniversidadClick();

    expect((component as any).universidadRequiredError).toBeTrue();
  });

  it('resets dependent selections when universidad changes', () => {
    (component as any).selectedCarreraId = 22;
    (component as any).selectedMallaId = 44;
    (component as any).carreras = [{ id: 1, universidadId: 1, nombre: 'Sistemas', codigo: 'SIS' }];
    (component as any).mallas = [{ id: 10, carreraId: 1, nombre: 'Malla 2017', version: '2017', active: true }];

    (component as any).onUniversidadChange(3);

    expect((component as any).selectedUniversidadId).toBe(3);
    expect((component as any).selectedCarreraId).toBeNull();
    expect((component as any).selectedMallaId).toBeNull();
    expect((component as any).carreras.length).toBe(0);
    expect((component as any).mallas.length).toBe(0);
  });

  it('loads existing selection and moves to resumen step', async () => {
    universidadServiceSpy.getUniversidadesActivas.and.returnValue(of([{ id: 1, nombre: 'UCB', codigo: 'UCB' }]));
    carreraServiceSpy.getCarrerasActivasPorUniversidad.and.returnValue(of([{ id: 11, universidadId: 1, nombre: 'Sistemas', codigo: 'SIS' }]));
    mallaCatalogoServiceSpy.getMallasActivasPorCarrera.and.returnValue(of([{ id: 101, carreraId: 11, nombre: 'Malla 2017', version: '2017', active: true }]));
    seleccionAcademicaServiceSpy.getSeleccionActual.and.returnValue(of({
      universidadId: 1,
      universidad: 'UCB',
      carreraId: 11,
      carrera: 'Sistemas',
      mallaId: 101,
      malla: 'Malla 2017',
    }));

    await (component as any).loadUniversidades();

    expect((component as any).step).toBe('resumen');
    expect((component as any).selectedUniversidadId).toBe(1);
    expect((component as any).selectedCarreraId).toBe(11);
    expect((component as any).selectedMallaId).toBe(101);
  });

  it('marks save error when guardarSeleccion fails', async () => {
    (component as any).selectedUniversidadId = 1;
    (component as any).selectedCarreraId = 2;
    (component as any).selectedMallaId = 3;
    seleccionAcademicaServiceSpy.guardarSeleccion.and.returnValue(throwError(() => new Error('fail')));

    (component as any).onGuardarMallaClick();
    await Promise.resolve();

    expect((component as any).saveSeleccionError).toBeTrue();
    expect((component as any).savingSeleccion).toBeFalse();
  });
});
