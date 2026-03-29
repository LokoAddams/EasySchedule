import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { Perfil } from './perfil';
import { PerfilService } from './perfil.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { LanguageService } from '../../core/services/language.service';
import { ToastService } from '../../core/services/toast.service';
import { PerfilResponse } from './perfil.model';

describe('Perfil Component', () => {
  let fixture: ComponentFixture<Perfil>;
  let component: Perfil;
  let perfilServiceSpy: jasmine.SpyObj<PerfilService>;
  let authSessionSpy: jasmine.SpyObj<AuthSessionService>;
  let languageServiceSpy: jasmine.SpyObj<LanguageService>;
  let toastServiceSpy: jasmine.SpyObj<ToastService>;

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

    perfilServiceSpy.getPerfilByUsername.and.returnValue(of(perfilMock));
    perfilServiceSpy.changePassword.and.returnValue(of({ message: 'ok' }));
    languageServiceSpy.getCurrentLanguage.and.returnValue('es');
    authSessionSpy.getCurrentUsername.and.returnValue('diego');

    await TestBed.configureTestingModule({
      imports: [Perfil, TranslateModule.forRoot()],
      providers: [
        { provide: PerfilService, useValue: perfilServiceSpy },
        { provide: AuthSessionService, useValue: authSessionSpy },
        { provide: LanguageService, useValue: languageServiceSpy },
        { provide: ToastService, useValue: toastServiceSpy },
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

  it('loads profile on init when session has username', () => {
    fixture.detectChanges();

    expect(perfilServiceSpy.getPerfilByUsername).toHaveBeenCalledWith('diego');
    expect((component as any).perfil?.username).toBe('diego');
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
});
