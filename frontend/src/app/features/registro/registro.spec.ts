import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Registro } from './registro';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('Registro Component', () => {

  let component: Registro;
  let fixture: ComponentFixture<Registro>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {

    await TestBed.configureTestingModule({
      imports: [
        Registro,
        ReactiveFormsModule,
        HttpClientTestingModule
      ],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(Registro);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debería crear el componente', () => {
    expect(component).toBeTruthy();
  });

  it('debería crear el formulario con campos requeridos', () => {

    expect(component.form.contains('nombre')).toBeTrue();
    expect(component.form.contains('correo')).toBeTrue();
    expect(component.form.contains('password')).toBeTrue();
    expect(component.form.contains('confirmPassword')).toBeTrue();

  });

  it('el formulario debe ser inválido si está vacío', () => {
    expect(component.form.valid).toBeFalse();
  });

  it('debería enviar registro cuando el formulario es válido', () => {

    component.form.setValue({
      nombre: 'Eduardo',
      correo: 'test@test.com',
      password: '12345678',
      confirmPassword: '12345678'
    });

    component.registrar();

    const req = httpMock.expectOne('/api/estudiantes/registro');

    expect(req.request.method).toBe('POST');

    req.flush({});

    expect(component.successMessageKey).toContain('Registro exitoso');

  });

  it('debería manejar error cuando el servidor responde 409', () => {

    component.form.setValue({
      nombre: 'Eduardo',
      correo: 'test@test.com',
      password: '12345678',
      confirmPassword: '12345678'
    });

    component.registrar();

    const req = httpMock.expectOne('/api/estudiantes/registro');

    req.flush(
      { message: 'correo existente' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(component.errorMessageKey).toContain('correo');

  });

});
