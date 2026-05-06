import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Registro } from './registro';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ToastService } from '../../core/services/toast.service';
import { environment } from '../../../environments/environment';

describe('Registro Component', () => {

  let component: Registro;
  let fixture: ComponentFixture<Registro>;
  let httpMock: HttpTestingController;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  const registerPath = '/api/estudiantes/registro';
  const registerUrl = `${environment.backendUrl.replace(/\/$/, '')}${registerPath}`;

  beforeEach(async () => {

    toastServiceSpy = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [
        Registro,
        ReactiveFormsModule,
        HttpClientTestingModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        { provide: ToastService, useValue: toastServiceSpy },
      ],
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

    const req = httpMock.expectOne(registerUrl);

    expect(req.request.method).toBe('POST');

    req.flush({});

    expect(component.successMessageKey).toBe('registro.success');

  });

  it('debería manejar error cuando el servidor responde 409', () => {

    component.form.setValue({
      nombre: 'Eduardo',
      correo: 'test@test.com',
      password: '12345678',
      confirmPassword: '12345678'
    });

    component.registrar();

    const req = httpMock.expectOne(registerUrl);

    req.flush(
      { message: 'correo existente' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(component.errorMessageKey).toBe('registro.error.emailExists');

  });

  it('debería cambiar la visibilidad del password', () => {
    expect(component.showPassword).toBeFalse();
    component.togglePasswordVisibility();
    expect(component.showPassword).toBeTrue();
  });

  it('debería cambiar la visibilidad de la confirmación de password', () => {
    expect(component.showConfirmPassword).toBeFalse();
    component.toggleConfirmPasswordVisibility();
    expect(component.showConfirmPassword).toBeTrue();
  });

});
