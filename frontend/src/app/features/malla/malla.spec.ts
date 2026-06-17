import { HttpClient } from '@angular/common/http';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { Subject } from 'rxjs';

import { Malla } from './malla';
import { CarreraService } from '../../services/academico/carrera.service';
import { MallaCatalogoService } from '../../services/academico/malla-catalogo.service';
import { SeleccionAcademicaService } from '../../services/academico/seleccion-academica.service';
import { UniversidadService } from '../../services/academico/universidad.service';
import { FeatureToggleService, FeatureFlags } from '../../services/feature-toggle.service';
import { TomaSeleccionService } from '../../services/academico/toma-seleccion.service';
import { TranslateService } from '@ngx-translate/core';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { PerfilService } from '../perfil/perfil.service';
import { ToastService } from '../../core/services/toast.service';
import { TourHintsService } from '../../services/tour-hints.service';
import { SeleccionTemporalService } from '../../services/academico/seleccion-temporal.service';
import { EstadoMateriaService } from '../../services/academico/estado-materia.service';

describe('Malla component logic', () => {
  let component: Malla;
  let flagsSubject: BehaviorSubject<FeatureFlags>;
  let featureServiceMock: jasmine.SpyObj<FeatureToggleService> & { flags$: BehaviorSubject<FeatureFlags> };
  let universidadServiceSpy: jasmine.SpyObj<UniversidadService>;
  let carreraServiceSpy: jasmine.SpyObj<CarreraService>;
  let mallaCatalogoServiceSpy: jasmine.SpyObj<MallaCatalogoService>;
  let seleccionAcademicaServiceSpy: jasmine.SpyObj<SeleccionAcademicaService>;
  let estadoMateriaServiceSpy: jasmine.SpyObj<any>;
  let tomaSeleccionServiceSpy: jasmine.SpyObj<TomaSeleccionService>;
  let translateServiceSpy: jasmine.SpyObj<TranslateService>;
  let httpClientSpy: jasmine.SpyObj<HttpClient>;
  let authSessionServiceSpy: jasmine.SpyObj<AuthSessionService>;
  let perfilServiceSpy: jasmine.SpyObj<PerfilService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let tourHintsServiceSpy: jasmine.SpyObj<TourHintsService>;
  let seleccionTemporalServiceSpy: jasmine.SpyObj<SeleccionTemporalService>;
  let routerSpy: jasmine.SpyObj<any>;
  let activatedRouteStub: any;

  beforeEach(() => {
    flagsSubject = new BehaviorSubject<FeatureFlags>({ malla: true, tomaMaterias: false, ofertasImport: true });

    featureServiceMock = jasmine.createSpyObj<FeatureToggleService>('FeatureToggleService', ['loadFlags']) as any;
    featureServiceMock.flags$ = flagsSubject;
    featureServiceMock.loadFlags.and.returnValue(Promise.resolve());

    universidadServiceSpy = jasmine.createSpyObj<UniversidadService>('UniversidadService', ['getUniversidadesActivas']);
    carreraServiceSpy = jasmine.createSpyObj<CarreraService>('CarreraService', ['getCarrerasActivasPorUniversidad']);
    mallaCatalogoServiceSpy = jasmine.createSpyObj<MallaCatalogoService>('MallaCatalogoService', [
      'getMallasActivasPorCarrera',
      'getMateriasPorMalla',
      'exportarAvanceGraduacion',
      'getDetallesMateria',
    ]);
    seleccionAcademicaServiceSpy = jasmine.createSpyObj<SeleccionAcademicaService>('SeleccionAcademicaService', ['getSeleccionActual', 'guardarSeleccion']);
    estadoMateriaServiceSpy = jasmine.createSpyObj('EstadoMateriaService', ['getEstadosMateria', 'guardarEstado']);
    tomaSeleccionServiceSpy = jasmine.createSpyObj<TomaSeleccionService>('TomaSeleccionService', ['agregarMateria']);
    Object.defineProperty(tomaSeleccionServiceSpy, 'seleccion$', { value: of([]) });
    translateServiceSpy = jasmine.createSpyObj<TranslateService>('TranslateService', ['instant']);
    translateServiceSpy.instant.and.callFake((key: string) => key);
    httpClientSpy = jasmine.createSpyObj<HttpClient>('HttpClient', ['get', 'post']);
    authSessionServiceSpy = jasmine.createSpyObj<AuthSessionService>('AuthSessionService', ['getCurrentUsername', 'isLoggedIn']);
    authSessionServiceSpy.getCurrentUsername.and.returnValue('testuser');
    authSessionServiceSpy.isLoggedIn.and.returnValue(true);
    perfilServiceSpy = jasmine.createSpyObj<PerfilService>('PerfilService', ['getPerfilByUsername', 'completeTour']);
    perfilServiceSpy.getPerfilByUsername.and.returnValue(of({ tourCompleted: true } as any));
    perfilServiceSpy.completeTour.and.returnValue(of({}) as any);
    toastServiceSpy = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error']);
    tourHintsServiceSpy = jasmine.createSpyObj<TourHintsService>('TourHintsService', [
      'openTomaMateriasPopover',
      'closeTomaMateriasPopover',
    ]);
    seleccionTemporalServiceSpy = jasmine.createSpyObj<SeleccionTemporalService>('SeleccionTemporalService', [
      'listarSelecciones',
      'agregarSeleccion',
      'limpiarSelecciones',
    ]);
    seleccionTemporalServiceSpy.listarSelecciones.and.returnValue(of([]));
    routerSpy = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);
    Object.defineProperty(routerSpy, 'events', { value: of([]) });
    activatedRouteStub = { snapshot: { queryParams: {} } };

    component = new (Malla as any)(
      featureServiceMock,
      universidadServiceSpy,
      carreraServiceSpy,
      mallaCatalogoServiceSpy,
      estadoMateriaServiceSpy,
      seleccionAcademicaServiceSpy,
      routerSpy,
      activatedRouteStub,
      tomaSeleccionServiceSpy,
      translateServiceSpy,
      httpClientSpy,
      toastServiceSpy,
      authSessionServiceSpy,
      perfilServiceSpy,
      tourHintsServiceSpy,
      seleccionTemporalServiceSpy,
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
    mallaCatalogoServiceSpy.getMateriasPorMalla.and.returnValue(of([]));
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
    (component as any).editMode = 'malla';
    (component as any).step = 'malla';
    (component as any).selectedResumen = {
      universidadId: 1,
      universidad: 'UCB',
      carreraId: 2,
      carrera: 'Sistemas',
      mallaId: 3,
      malla: 'Malla 2017',
    };
    (component as any).previousSelectionSnapshot = { universidadId: 1, carreraId: 2, mallaId: 3 };
    (component as any).selectedUniversidadId = 1;
    (component as any).selectedCarreraId = 2;
    (component as any).selectedMallaId = 3;
    seleccionAcademicaServiceSpy.guardarSeleccion.and.returnValue(throwError(() => new Error('fail')));
    spyOn(window, 'confirm').and.returnValue(true);

    (component as any).onGuardarMallaClick();
    await Promise.resolve();

    expect((component as any).saveSeleccionError).toBeTrue();
    expect((component as any).savingSeleccion).toBeFalse();
    expect((component as any).step).toBe('resumen');
    expect((component as any).selectedMallaId).toBe(3);
  });

  it('shows confirmation modal before university change', () => {
    (component as any).selectedUniversidadId = 1;
    (component as any).selectedCarreraId = 2;
    (component as any).selectedMallaId = 3;

    (component as any).onCambiarUniversidadClick();

    expect((component as any).showConfirmModal).toBeTrue();
    expect((component as any).pendingConfirmAction).toBe('changeUniversidad');
  });

  it('executes university change flow after confirmation', () => {
    (component as any).selectedUniversidadId = 1;
    (component as any).selectedCarreraId = 2;
    (component as any).selectedMallaId = 3;

    (component as any).onCambiarUniversidadClick();
    (component as any).onConfirmAction();

    expect((component as any).editMode).toBe('universidad');
    expect((component as any).step).toBe('universidad');
    expect((component as any).selectedCarreraId).toBeNull();
    expect((component as any).selectedMallaId).toBeNull();
  });

  it('loads mallas for current university when changing malla', async () => {
    (component as any).selectedUniversidadId = 1;
    (component as any).selectedCarreraId = 11;
    (component as any).selectedMallaId = 101;
    carreraServiceSpy.getCarrerasActivasPorUniversidad.and.returnValue(of([{ id: 11, universidadId: 1, nombre: 'Sistemas', codigo: 'SIS' }]));
    mallaCatalogoServiceSpy.getMallasActivasPorCarrera.and.returnValue(
      of([
        { id: 101, carreraId: 11, nombre: 'Malla 2017', version: '2017', active: true },
        { id: 102, carreraId: 11, nombre: 'Malla 2024', version: '2024', active: true },
      ]),
    );

    await (component as any).prepareMallaEditMode();

    expect((component as any).editMode).toBe('malla');
    expect((component as any).step).toBe('malla');
    expect((component as any).mallaChangeWarningVisible).toBeTrue();
    expect((component as any).mallas.length).toBe(2);
  });

  it('keeps selection unchanged when user cancels warning confirmation', () => {
    (component as any).editMode = 'malla';
    (component as any).selectedUniversidadId = 1;
    (component as any).selectedCarreraId = 11;
    (component as any).selectedMallaId = 102;
    (component as any).previousSelectionSnapshot = { universidadId: 1, carreraId: 11, mallaId: 101 };
    spyOn(window, 'confirm').and.returnValue(false);

    (component as any).onGuardarMallaClick();

    expect(seleccionAcademicaServiceSpy.guardarSeleccion).not.toHaveBeenCalled();
  });

  it('loadMaterias preserves estado values returned by backend', async () => {
    mallaCatalogoServiceSpy.getMateriasPorMalla.and.returnValue(of([
      {
        id: 1,
        materiaId: 10,
        codigoMateria: 'INF-101',
        nombreMateria: 'Programacion I',
        creditos: 4,
        semestreSugerido: 1,
        estado: 'aprobada',
        prerequisitosIds: [],
      },
      {
        id: 2,
        materiaId: 11,
        codigoMateria: 'INF-102',
        nombreMateria: 'Programacion II',
        creditos: 4,
        semestreSugerido: 2,
        estado: null,
        prerequisitosIds: [],
      },
    ]));

    await (component as any).loadMaterias(100);

    expect((component as any).materias.length).toBe(2);
    expect((component as any).materias[0].estado).toBe('aprobada');
    expect((component as any).materias[1].estado).toBeNull();
    expect((component as any).materiasPorSemestre.get(1).length).toBe(1);
    expect((component as any).materiasPorSemestre.get(2).length).toBe(1);
  });

  it('sets loadMateriasError when materias request fails', async () => {
    mallaCatalogoServiceSpy.getMateriasPorMalla.and.returnValue(throwError(() => new Error('boom')));

    await (component as any).loadMaterias(100);

    expect((component as any).loadMateriasError).toBeTrue();
    expect((component as any).loadingMaterias).toBeFalse();
  });

  it('calls graduation progress export endpoint when export button action runs', async () => {
    const response = buildBlobResponse('avance_graduacion.pdf');
    mallaCatalogoServiceSpy.exportarAvanceGraduacion.and.returnValue(of(response));
    spyOn(component as any, 'downloadBlob');

    await (component as any).onExportarAvanceClick();

    expect(mallaCatalogoServiceSpy.exportarAvanceGraduacion).toHaveBeenCalled();
  });

  it('shows loading state during graduation progress export', () => {
    const exportSubject = new Subject<HttpResponse<Blob>>();
    mallaCatalogoServiceSpy.exportarAvanceGraduacion.and.returnValue(exportSubject.asObservable());
    spyOn(component as any, 'downloadBlob');

    void (component as any).onExportarAvanceClick();

    expect((component as any).exportingAvance).toBeTrue();

    exportSubject.next(buildBlobResponse('avance_graduacion.pdf'));
    exportSubject.complete();
  });

  it('downloads the generated report when export succeeds', async () => {
    const response = buildBlobResponse('avance_graduacion.pdf');
    mallaCatalogoServiceSpy.exportarAvanceGraduacion.and.returnValue(of(response));
    const downloadSpy = spyOn(component as any, 'downloadBlob');

    await (component as any).onExportarAvanceClick();

    expect(downloadSpy).toHaveBeenCalledWith(response.body, 'avance_graduacion.pdf');
    expect(toastServiceSpy.success).toHaveBeenCalledWith('malla.export.success');
  });

  it('uses filename received from backend when export succeeds', async () => {
    const response = buildBlobResponse('mi_avance.pdf');
    mallaCatalogoServiceSpy.exportarAvanceGraduacion.and.returnValue(of(response));
    const downloadSpy = spyOn(component as any, 'downloadBlob');

    await (component as any).onExportarAvanceClick();

    expect(downloadSpy).toHaveBeenCalledWith(response.body, 'mi_avance.pdf');
  });

  it('shows insufficient data error when backend rejects export data', async () => {
    mallaCatalogoServiceSpy.exportarAvanceGraduacion.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 422 })),
    );

    await (component as any).onExportarAvanceClick();

    expect(toastServiceSpy.error).toHaveBeenCalledWith('malla.export.insufficientData');
  });

  it('shows connection error when export request fails by network', async () => {
    mallaCatalogoServiceSpy.exportarAvanceGraduacion.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );

    await (component as any).onExportarAvanceClick();

    expect(toastServiceSpy.error).toHaveBeenCalledWith('malla.export.connectionError');
  });

  it('ignores duplicate export clicks while request is in progress', () => {
    const exportSubject = new Subject<HttpResponse<Blob>>();
    mallaCatalogoServiceSpy.exportarAvanceGraduacion.and.returnValue(exportSubject.asObservable());
    spyOn(component as any, 'downloadBlob');

    void (component as any).onExportarAvanceClick();
    void (component as any).onExportarAvanceClick();

    expect(mallaCatalogoServiceSpy.exportarAvanceGraduacion).toHaveBeenCalledTimes(1);
    exportSubject.next(buildBlobResponse('avance_graduacion.pdf'));
    exportSubject.complete();
  });

  it('does not export when user is not authenticated', async () => {
    authSessionServiceSpy.isLoggedIn.and.returnValue(false);

    await (component as any).onExportarAvanceClick();

    expect(mallaCatalogoServiceSpy.exportarAvanceGraduacion).not.toHaveBeenCalled();
    expect(toastServiceSpy.error).toHaveBeenCalledWith('malla.export.notAuthenticated');
  });

  it('renders graduation progress export button', async () => {
    universidadServiceSpy.getUniversidadesActivas.and.returnValue(of([]));
    seleccionAcademicaServiceSpy.getSeleccionActual.and.returnValue(throwError(() => new Error('empty')));
    mallaCatalogoServiceSpy.getMateriasPorMalla.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Malla, TranslateModule.forRoot()],
      providers: [
        { provide: FeatureToggleService, useValue: featureServiceMock },
        { provide: UniversidadService, useValue: universidadServiceSpy },
        { provide: CarreraService, useValue: carreraServiceSpy },
        { provide: MallaCatalogoService, useValue: mallaCatalogoServiceSpy },
        { provide: SeleccionAcademicaService, useValue: seleccionAcademicaServiceSpy },
        { provide: EstadoMateriaService, useValue: estadoMateriaServiceSpy },
        { provide: TomaSeleccionService, useValue: tomaSeleccionServiceSpy },
        { provide: HttpClient, useValue: httpClientSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: AuthSessionService, useValue: authSessionServiceSpy },
        { provide: PerfilService, useValue: perfilServiceSpy },
        { provide: TourHintsService, useValue: tourHintsServiceSpy },
        { provide: SeleccionTemporalService, useValue: seleccionTemporalServiceSpy },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: Router, useValue: routerSpy },
      ],
    }).compileComponents();

    const fixture: ComponentFixture<Malla> = TestBed.createComponent(Malla);
    (fixture.componentInstance as any).mallaEnabled = true;
    (fixture.componentInstance as any).step = 'resumen';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const buttons = fixture.debugElement.queryAll(By.css('.malla-page__actions button'));
    const hasExportButton = buttons.some((button) => button.nativeElement.textContent.includes('malla.export.button'));

    expect(hasExportButton).toBeTrue();
  });

  describe('HU-148 confirm modal edge cases', () => {
    it('onImportMallaClick does not open modal when no file is selected', () => {
      (component as any).importFile = null;
      (component as any).importMallaName = 'Test';
      (component as any).importMallaVersion = '2024';

      (component as any).onImportMallaClick();

      expect((component as any).showConfirmModal).toBeFalse();
    });

    it('onImportMallaClick does not open modal when name is empty', () => {
      (component as any).importFile = new File([''], 'test.json');
      (component as any).importMallaName = '';
      (component as any).importMallaVersion = '2024';

      (component as any).onImportMallaClick();

      expect((component as any).showConfirmModal).toBeFalse();
    });

    it('onImportMallaClick does not open modal when version is invalid', () => {
      (component as any).importFile = new File([''], 'test.json');
      (component as any).importMallaName = 'Test';
      (component as any).importMallaVersion = 'abc';

      (component as any).onImportMallaClick();

      expect((component as any).showConfirmModal).toBeFalse();
    });

    it('onImportMallaClick does not open modal while already loading', () => {
      (component as any).importFile = new File([''], 'test.json');
      (component as any).importMallaName = 'Test';
      (component as any).importMallaVersion = '2024';
      (component as any).importLoading = true;

      (component as any).onImportMallaClick();

      expect((component as any).showConfirmModal).toBeFalse();
    });

    it('opens modal when canImportMalla returns true', () => {
      (component as any).importFile = new File([''], 'test.json');
      (component as any).importMallaName = 'Test';
      (component as any).importMallaVersion = '2024';

      (component as any).onImportMallaClick();

      expect((component as any).showConfirmModal).toBeTrue();
      expect((component as any).pendingConfirmAction).toBe('importMalla');
    });

    it('clears pendingConfirmAction on cancel and does not execute any action', () => {
      (component as any).pendingConfirmAction = 'changeUniversidad';
      (component as any).showConfirmModal = true;

      (component as any).onCancelConfirm();

      expect((component as any).showConfirmModal).toBeFalse();
      expect((component as any).pendingConfirmAction).toBeNull();
    });

    it('onConfirmAction calls ejecutarLimpiarSeleccion only once for clearSelection', () => {
      spyOn(component as any, 'ejecutarLimpiarSeleccion');
      (component as any).pendingConfirmAction = 'clearSelection';

      (component as any).onConfirmAction();
      (component as any).onConfirmAction();

      expect((component as any).ejecutarLimpiarSeleccion).toHaveBeenCalledTimes(1);
    });
  });

  function buildBlobResponse(filename: string): HttpResponse<Blob> {
    return new HttpResponse({
      body: new Blob(['pdf'], { type: 'application/pdf' }),
      headers: new HttpHeaders({ 'content-disposition': `attachment; filename="${filename}"` }),
    });
  }
});
