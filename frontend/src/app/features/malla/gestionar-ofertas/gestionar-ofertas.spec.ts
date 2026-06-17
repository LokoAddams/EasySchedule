import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GestionarOfertas } from './gestionar-ofertas';
import { OfertaImportService } from '../../../services/academico/oferta-import.service';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

describe('GestionarOfertas', () => {
  let component: GestionarOfertas;
  let fixture: ComponentFixture<GestionarOfertas>;
  let mockOfertaImportService: jasmine.SpyObj<OfertaImportService>;

  beforeEach(async () => {
    mockOfertaImportService = jasmine.createSpyObj('OfertaImportService', [
      'listarSemestres',
      'listarParalelos',
      'listarOfertas',
      'obtenerOfertaParaEdicion',
      'validarActualizacion',
      'actualizarOferta'
    ]);

    // Setup default mock returns
    mockOfertaImportService.listarSemestres.and.returnValue(of(['1', '2']));
    mockOfertaImportService.listarParalelos.and.returnValue(of(['A', 'B']));
    mockOfertaImportService.listarOfertas.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [GestionarOfertas, FormsModule, TranslateModule.forRoot()],
      providers: [
        { provide: OfertaImportService, useValue: mockOfertaImportService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(GestionarOfertas);
    component = fixture.componentInstance;
    component.mallaId = 1;
    fixture.detectChanges();
  });

  it('debe crearse correctamente', () => {
    expect(component).toBeTruthy();
  });

  describe('abrirEdicion', () => {
    it('debe mapear el dia a mayusculas', async () => {
      // Arrange
      const mockOferta = {
        id: 1,
        codigoMateria: 'MAT-101',
        nombreMateria: 'Matematicas',
        paralelo: 'A',
        semestre: '1',
        docente: 'Docente X',
        aula: 'Aula 1',
        horarios: [{ dia: 'Lunes', horaInicio: '08:00', horaFin: '09:30' }]
      };
      mockOfertaImportService.obtenerOfertaParaEdicion.and.returnValue(of(mockOferta));

      // Act
      await component['abrirEdicion'](1);

      // Assert
      expect(component['editingOferta']).toBeTruthy();
      expect(component['editingOferta']?.horarios[0].dia).toBe('LUNES');
    });
  });

  describe('gestionar bloques de horario', () => {
    beforeEach(() => {
      component['editingOferta'] = {
        id: 1,
        codigoMateria: 'MAT-101',
        nombreMateria: 'Matematicas',
        paralelo: 'A',
        semestre: '1',
        docente: 'Docente X',
        aula: 'Aula 1',
        horarios: []
      };
    });

    it('addHorarioBlock debe agregar un bloque por defecto', () => {
      component['addHorarioBlock']();
      expect(component['editingOferta']?.horarios.length).toBe(1);
      expect(component['editingOferta']?.horarios[0].dia).toBe('LUNES');
    });

    it('removeHorarioBlock debe eliminar el bloque en el indice especificado', () => {
      component['addHorarioBlock']();
      component['addHorarioBlock'](); // 2 blocks
      component['removeHorarioBlock'](0);
      expect(component['editingOferta']?.horarios.length).toBe(1);
    });
  });

  describe('validarEdicion', () => {
    beforeEach(() => {
      component['editingOferta'] = {
        id: 1,
        codigoMateria: 'MAT-101',
        nombreMateria: 'Matematicas',
        paralelo: 'A',
        semestre: '1',
        docente: 'Docente X',
        aula: 'Aula 1',
        horarios: []
      };
    });

    it('debe mostrar mensaje de exito si la validacion pasa', async () => {
      mockOfertaImportService.validarActualizacion.and.returnValue(of(undefined)); // Success
      await component['validarEdicion']();
      expect(component['editSuccessMsg']).toContain('exitosa');
      expect(component['editErrorMsg']).toBe('');
    });

    it('debe mostrar mensaje de error si hay cruce', async () => {
      const errorResponse = { error: { message: 'Cruce de horario en Aula 1' } };
      mockOfertaImportService.validarActualizacion.and.returnValue(throwError(() => errorResponse));
      await component['validarEdicion']();
      expect(component['editErrorMsg']).toBe('Cruce de horario en Aula 1');
      expect(component['editSuccessMsg']).toBe('');
    });
  });

  describe('guardarEdicion', () => {
    beforeEach(() => {
      component['editingOferta'] = {
        id: 1,
        codigoMateria: 'MAT-101',
        nombreMateria: 'Matematicas',
        paralelo: 'A',
        semestre: '1',
        docente: 'Docente X',
        aula: 'Aula 1',
        horarios: []
      };
    });

    it('debe cerrar la edicion y recargar si guarda con exito', async () => {
      mockOfertaImportService.actualizarOferta.and.returnValue(of(undefined));
      spyOn<any>(component, 'loadOfertas');
      
      await component['guardarEdicion']();
      
      expect(component['editingOferta']).toBeNull(); // Cerrar edicion
      expect(component['loadOfertas']).toHaveBeenCalled(); // Recargar
    });

    it('debe mostrar error si falla el guardado', async () => {
      const errorResponse = { error: { message: 'Error de servidor' } };
      mockOfertaImportService.actualizarOferta.and.returnValue(throwError(() => errorResponse));
      
      await component['guardarEdicion']();
      
      expect(component['editErrorMsg']).toBe('Error de servidor');
      expect(component['editingOferta']).not.toBeNull(); // No se cierra
    });
  });
});
