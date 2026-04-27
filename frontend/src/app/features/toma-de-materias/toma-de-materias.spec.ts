import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { TomaDeMaterias } from './toma-de-materias';
import { HorarioActualService } from '../../services/academico/horario-actual.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { PerfilService } from '../perfil/perfil.service';

describe('TomaDeMaterias component logic', () => {
  let component: TomaDeMaterias;
  let horarioActualServiceSpy: jasmine.SpyObj<HorarioActualService>;
  let authSessionServiceSpy: jasmine.SpyObj<AuthSessionService>;
  let perfilServiceSpy: jasmine.SpyObj<PerfilService>;

  beforeEach(() => {
    horarioActualServiceSpy = jasmine.createSpyObj<HorarioActualService>('HorarioActualService', [
      'getHorarioActual',
      'exportHorarioActualCsv',
      'exportHorarioActualPdf',
      'exportHorarioActualImage',
    ]);
    authSessionServiceSpy = jasmine.createSpyObj<AuthSessionService>('AuthSessionService', ['getCurrentUsername']);
    perfilServiceSpy = jasmine.createSpyObj<PerfilService>('PerfilService', ['getPerfilByUsername']);
    component = new TomaDeMaterias(horarioActualServiceSpy, authSessionServiceSpy, perfilServiceSpy);
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

    authSessionServiceSpy.getCurrentUsername.and.returnValue(null);

    component.ngOnInit();

    expect((component as any).loading).toBeFalse();
    expect((component as any).error).toBeFalse();
    expect((component as any).timeRows).toEqual(['07:00 - 08:30']);
    expect((component as any).getCellItems('07:00 - 08:30', 'Lunes').length).toBe(1);
  });

  it('shows info message when exporting without classes', () => {
    (component as any).horario = {
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: null,
      clases: [],
    };

    (component as any).exportHorario();

    expect((component as any).exportInfo).toBe('No hay horario disponible para exportar.');
    expect((component as any).exportLoading).toBeFalse();
    expect(horarioActualServiceSpy.exportHorarioActualCsv).not.toHaveBeenCalled();
  });

  it('blocks unsupported format exports', () => {
    (component as any).horario = {
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: 1,
      clases: [
        {
          materia: 'Materia',
          paralelo: 'A',
          dia: 'Lunes',
          horaInicio: '07:00',
          horaFin: '08:30',
          docente: null,
          aula: null,
        },
      ],
    };
    (component as any).exportFormat = 'xlsx';

    (component as any).exportHorario();

    expect((component as any).exportError).toBe('Formato no soportado por el momento.');
    expect(horarioActualServiceSpy.exportHorarioActualCsv).not.toHaveBeenCalled();
  });

  it('exports csv when student id is available', () => {
    const payload = new Blob(['csv']);
    const headers = new HttpHeaders({ 'Content-Disposition': 'attachment; filename="horario.csv"' });

    (component as any).estudianteId = 7;
    (component as any).horario = {
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: 1,
      clases: [
        {
          materia: 'Materia',
          paralelo: 'A',
          dia: 'Lunes',
          horaInicio: '07:00',
          horaFin: '08:30',
          docente: null,
          aula: null,
        },
      ],
    };

    horarioActualServiceSpy.exportHorarioActualCsv.and.returnValue(
      of(new HttpResponse({ body: payload, headers }))
    );
    const downloadSpy = spyOn(component as any, 'triggerDownload');

    (component as any).exportHorario();

    expect(horarioActualServiceSpy.exportHorarioActualCsv).toHaveBeenCalledWith(7);
    expect(downloadSpy).toHaveBeenCalledWith(payload, 'horario.csv');
    expect((component as any).exportLoading).toBeFalse();
  });

  it('exports pdf when selected', () => {
    const payload = new Blob(['pdf']);
    const headers = new HttpHeaders({ 'Content-Disposition': 'attachment; filename="horario.pdf"' });

    (component as any).estudianteId = 7;
    (component as any).exportFormat = 'pdf';
    (component as any).horario = {
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: 1,
      clases: [
        {
          materia: 'Materia',
          paralelo: 'A',
          dia: 'Lunes',
          horaInicio: '07:00',
          horaFin: '08:30',
          docente: null,
          aula: null,
        },
      ],
    };

    horarioActualServiceSpy.exportHorarioActualPdf.and.returnValue(
      of(new HttpResponse({ body: payload, headers }))
    );
    const downloadSpy = spyOn(component as any, 'triggerDownload');

    (component as any).exportHorario();

    expect(horarioActualServiceSpy.exportHorarioActualPdf).toHaveBeenCalledWith(7);
    expect(downloadSpy).toHaveBeenCalledWith(payload, 'horario.pdf');
    expect((component as any).exportLoading).toBeFalse();
  });

  it('exports image when selected', () => {
    const payload = new Blob(['png']);
    const headers = new HttpHeaders({ 'Content-Disposition': 'attachment; filename="horario.png"' });

    (component as any).estudianteId = 7;
    (component as any).exportFormat = 'imagen';
    (component as any).horario = {
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: 1,
      clases: [
        {
          materia: 'Materia',
          paralelo: 'A',
          dia: 'Lunes',
          horaInicio: '07:00',
          horaFin: '08:30',
          docente: null,
          aula: null,
        },
      ],
    };

    horarioActualServiceSpy.exportHorarioActualImage.and.returnValue(
      of(new HttpResponse({ body: payload, headers }))
    );
    const downloadSpy = spyOn(component as any, 'triggerDownload');

    (component as any).exportHorario();

    expect(horarioActualServiceSpy.exportHorarioActualImage).toHaveBeenCalledWith(7);
    expect(downloadSpy).toHaveBeenCalledWith(payload, 'horario.png');
    expect((component as any).exportLoading).toBeFalse();
  });

  it('resolves student id from profile when missing', () => {
    const payload = new Blob(['csv']);

    (component as any).horario = {
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: 1,
      clases: [
        {
          materia: 'Materia',
          paralelo: 'A',
          dia: 'Lunes',
          horaInicio: '07:00',
          horaFin: '08:30',
          docente: null,
          aula: null,
        },
      ],
    };

    authSessionServiceSpy.getCurrentUsername.and.returnValue('estudiante');
    perfilServiceSpy.getPerfilByUsername.and.returnValue(
      of({
        id: 9,
        username: 'estudiante',
        nombre: null,
        apellido: null,
        email: 'estudiante@demo.com',
        carnetIdentidad: null,
        fechaNacimiento: null,
        fechaRegistro: null,
        semestreActual: 1,
        carrera: null,
        mallaId: null,
        universidad: null,
      })
    );
    horarioActualServiceSpy.exportHorarioActualCsv.and.returnValue(
      of(new HttpResponse({ body: payload }))
    );
    const downloadSpy = spyOn(component as any, 'triggerDownload');

    (component as any).exportHorario();

    expect(perfilServiceSpy.getPerfilByUsername).toHaveBeenCalledWith('estudiante');
    expect(horarioActualServiceSpy.exportHorarioActualCsv).toHaveBeenCalledWith(9);
    expect(downloadSpy).toHaveBeenCalled();
  });

  it('shows info message when backend reports no schedule', () => {
    (component as any).estudianteId = 7;
    (component as any).horario = {
      universidad: null,
      carrera: null,
      malla: null,
      semestreOferta: null,
      semestreActual: 1,
      clases: [
        {
          materia: 'Materia',
          paralelo: 'A',
          dia: 'Lunes',
          horaInicio: '07:00',
          horaFin: '08:30',
          docente: null,
          aula: null,
        },
      ],
    };

    horarioActualServiceSpy.exportHorarioActualCsv.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 404 }))
    );

    (component as any).exportHorario();

    expect((component as any).exportInfo).toBe('No hay horario disponible para exportar.');
    expect((component as any).exportLoading).toBeFalse();
  });
});
