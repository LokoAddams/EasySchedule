import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { Perfil } from './perfil';
import { PerfilService } from './perfil.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { LanguageService } from '../../core/services/language.service';
import { ToastService } from '../../core/services/toast.service';
import { SeleccionAcademicaService } from '../../services/academico/seleccion-academica.service';
import { PerfilResponse } from './perfil.model';

describe('Perfil Component', () => {
  let fixture: ComponentFixture<Perfil>;
  let component: Perfil;
  let perfilServiceSpy: jasmine.SpyObj<PerfilService>;
  let authSessionSpy: jasmine.SpyObj<AuthSessionService>;
  let languageServiceSpy: jasmine.SpyObj<LanguageService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;
  let seleccionAcademicaServiceSpy: jasmine.SpyObj<SeleccionAcademicaService>;

  const perfilMock: PerfilResponse = {
    id: 1,
    username: 'diego',
    nombre: null,
    apellido: null,
    email: null,
    carnetIdentidad: null,
    fechaNacimiento: null,
    fechaRegistro: null,
    semestreActual: null,
    carrera: null,
    mallaId: null,
    universidad: null,
  };

  beforeEach(async () => {
    perfilServiceSpy = jasmine.createSpyObj<PerfilService>('PerfilService', ['getPerfilByUsername', 'updatePerfil', 'changePassword']);
    authSessionSpy = jasmine.createSpyObj<AuthSessionService>(
      'AuthSessionService',
      ['getCurrentUsername', 'setCurrentUsername', 'setProfileCompleted', 'clearSession'],
    );
    languageServiceSpy = jasmine.createSpyObj<LanguageService>('LanguageService', ['getCurrentLanguage']);
    toastServiceSpy = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error']);
    seleccionAcademicaServiceSpy = jasmine.createSpyObj<SeleccionAcademicaService>('SeleccionAcademicaService', ['getSeleccionActual']);

    perfilServiceSpy.getPerfilByUsername.and.returnValue(of(perfilMock));
    perfilServiceSpy.changePassword.and.returnValue(of({ message: 'ok' }));
    seleccionAcademicaServiceSpy.getSeleccionActual.and.returnValue(of({
      universidadId: 1,
      universidad: 'Universidad Privada Boliviana',
      carreraId: 2,
      carrera: 'Ingenieria Comercial',
      mallaId: 5,
      malla: 'Malla 2024',
    }));
    languageServiceSpy.getCurrentLanguage.and.returnValue('es');
    authSessionSpy.getCurrentUsername.and.returnValue('diego');

    await TestBed.configureTestingModule({
      imports: [Perfil, TranslateModule.forRoot()],
      providers: [
        { provide: PerfilService, useValue: perfilServiceSpy },
        { provide: AuthSessionService, useValue: authSessionSpy },
        { provide: LanguageService, useValue: languageServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
        { provide: SeleccionAcademicaService, useValue: seleccionAcademicaServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation('es', {
      perfil: {
        placeholders: {
          nombre: 'Haz clic en este campo para completar tu nombre',
          apellido: 'Haz clic en este campo para completar tus apellidos',
          email: 'Haz clic en este campo para completar tu correo',
          carnetIdentidad: 'Haz clic en este campo para completar tu carnet',
          fechaNacimiento: 'Haz clic en este campo para completar tu fecha de nacimiento',
          carrera: 'Se asigna automaticamente segun tu malla',
          universidad: 'Se asigna automaticamente segun tu malla',
        },
      },
    }, true);
    translateService.use('es');
  });

  it('loads profile on init when session has username', async () => {
    fixture.detectChanges();
    await fixture.whenStable();

    expect(perfilServiceSpy.getPerfilByUsername).toHaveBeenCalledWith('diego');
    expect(seleccionAcademicaServiceSpy.getSeleccionActual).toHaveBeenCalled();
    expect((component as any).perfil?.username).toBe('diego');
    expect((component as any).perfil?.universidad).toBe('Universidad Privada Boliviana');
    expect((component as any).perfil?.carrera).toBe('Ingenieria Comercial');
    expect((component as any).loading).toBeFalse();
  });

  it('shows no-session error when username does not exist', () => {
    authSessionSpy.getCurrentUsername.and.returnValue('');

    fixture.detectChanges();

    expect((component as any).errorKey).toBe('perfil.error.noSession');
    expect(perfilServiceSpy.getPerfilByUsername).not.toHaveBeenCalled();
  });

  it('enters edit mode when clicking edit button action', () => {
    fixture.detectChanges();

    (component as any).activarEdicion();
    expect((component as any).editMode).toBeTrue();
  });

  it('saves profile update and emits success toast', () => {
    const updatedPerfil: PerfilResponse = {
      ...perfilMock,
      username: 'diego2',
      nombre: 'Diego',
      apellido: 'Suarez',
      email: 'diego@mail.com',
      carnetIdentidad: '991122',
      fechaNacimiento: '2001-03-10',
    };

    perfilServiceSpy.updatePerfil.and.returnValue(of(updatedPerfil));
    fixture.detectChanges();

    (component as any).activarEdicion();
    (component as any).editForm.patchValue({
      username: 'diego2',
      nombre: 'Diego',
      apellido: 'Suarez',
      email: 'diego@mail.com',
      carnetIdentidad: '991122',
      fechaNacimiento: { year: 2001, month: 3, day: 10 },
      carrera: '',
      universidad: '',
    });

    (component as any).guardarEdicion();
    expect((component as any).showIdentityConfirmModal).toBeTrue();

    (component as any).confirmarCambioIdentidadYGuardar();

    expect(perfilServiceSpy.updatePerfil).toHaveBeenCalled();
    expect(toastServiceSpy.success).toHaveBeenCalledWith('perfil.success.updated');
    expect((component as any).editMode).toBeFalse();
    expect(authSessionSpy.setCurrentUsername).toHaveBeenCalledWith('diego2');
  });

  it('changes password and emits success toast', () => {
    fixture.detectChanges();

    (component as any).abrirCambioContrasenia();
    (component as any).passwordForm.patchValue({
      currentPassword: 'actual1234',
      newPassword: 'nueva1234',
      confirmNewPassword: 'nueva1234',
    });

    (component as any).guardarCambioContrasenia();

    expect(perfilServiceSpy.changePassword).toHaveBeenCalledWith({
      currentPassword: 'actual1234',
      newPassword: 'nueva1234',
      confirmNewPassword: 'nueva1234',
    });
    expect(toastServiceSpy.success).toHaveBeenCalledWith('perfil.password.success.updated');
    expect((component as any).showChangePasswordModal).toBeFalse();
  });

  it('shows toast when current password is incorrect', () => {
    perfilServiceSpy.changePassword.and.returnValue(
      throwError(() => ({ status: 400, error: { message: 'La contrasenia actual es incorrecta' } })),
    );
    fixture.detectChanges();

    (component as any).abrirCambioContrasenia();
    (component as any).passwordForm.patchValue({
      currentPassword: 'incorrecta',
      newPassword: 'nueva1234',
      confirmNewPassword: 'nueva1234',
    });

    (component as any).guardarCambioContrasenia();

    expect(toastServiceSpy.error).toHaveBeenCalledWith('perfil.password.error.currentIncorrect');
    expect((component as any).showChangePasswordModal).toBeTrue();
  });

  it('accepts bolivian identity card format with extension', () => {
    fixture.detectChanges();

    (component as any).activarEdicion();
    (component as any).editForm.controls.carnetIdentidad.setValue('1234567-1A lp');
    (component as any).editForm.controls.carnetIdentidad.markAsTouched();

    expect((component as any).editForm.controls.carnetIdentidad.valid).toBeTrue();
  });

  it('rejects identity card with invalid mixed format', () => {
    fixture.detectChanges();

    (component as any).activarEdicion();
    (component as any).editForm.controls.carnetIdentidad.setValue('abc123');
    (component as any).editForm.controls.carnetIdentidad.markAsTouched();

    expect((component as any).editForm.controls.carnetIdentidad.invalid).toBeTrue();
    expect((component as any).getErrorMessageCarnet()).toBe('perfil.validation.carnet.invalidChars');
  });

  it('rejects identity card with 5 digits', () => {
    fixture.detectChanges();

    (component as any).activarEdicion();
    (component as any).editForm.controls.carnetIdentidad.setValue('93267');
    (component as any).editForm.controls.carnetIdentidad.markAsTouched();

    expect((component as any).editForm.controls.carnetIdentidad.invalid).toBeTrue();
    expect((component as any).getErrorMessageCarnet()).toBe('perfil.validation.carnet.invalidChars');
  });

  it('shows specific toast when backend rejects carnet format', () => {
    const perfilCompleto: PerfilResponse = {
      ...perfilMock,
      nombre: 'Diego',
      apellido: 'Suarez',
      email: 'diego@mail.com',
      carnetIdentidad: '1234567',
      fechaNacimiento: '2001-03-10',
    };

    perfilServiceSpy.getPerfilByUsername.and.returnValue(of(perfilCompleto));
    perfilServiceSpy.updatePerfil.and.returnValue(
      throwError(() => ({ status: 400, error: { message: 'Formato de carnet de identidad invalido para Bolivia' } })),
    );

    fixture.detectChanges();
    (component as any).activarEdicion();
    (component as any).editForm.patchValue({
      carnetIdentidad: '1234567',
    });

    (component as any).guardarEdicion();

    expect(toastServiceSpy.error).toHaveBeenCalledWith('perfil.error.carnetInvalidFormat');
  });
});
