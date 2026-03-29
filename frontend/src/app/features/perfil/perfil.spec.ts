import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { Perfil } from './perfil';
import { PerfilService } from './perfil.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { LanguageService } from '../../core/services/language.service';
import { PerfilResponse } from './perfil.model';

describe('Perfil Component', () => {
  let fixture: ComponentFixture<Perfil>;
  let component: Perfil;
  let perfilServiceSpy: jasmine.SpyObj<PerfilService>;
  let authSessionSpy: jasmine.SpyObj<AuthSessionService>;
  let languageServiceSpy: jasmine.SpyObj<LanguageService>;

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
    perfilServiceSpy = jasmine.createSpyObj<PerfilService>('PerfilService', ['getPerfilByUsername', 'updatePerfil', 'getMallas']);
    authSessionSpy = jasmine.createSpyObj<AuthSessionService>('AuthSessionService', ['getCurrentUsername', 'setCurrentUsername', 'clearSession']);
    languageServiceSpy = jasmine.createSpyObj<LanguageService>('LanguageService', ['getCurrentLanguage']);

    perfilServiceSpy.getPerfilByUsername.and.returnValue(of(perfilMock));
    languageServiceSpy.getCurrentLanguage.and.returnValue('es');
    authSessionSpy.getCurrentUsername.and.returnValue('diego');

    await TestBed.configureTestingModule({
      imports: [Perfil, TranslateModule.forRoot()],
      providers: [
        { provide: PerfilService, useValue: perfilServiceSpy },
        { provide: AuthSessionService, useValue: authSessionSpy },
        { provide: LanguageService, useValue: languageServiceSpy },
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

  it('enters edit mode when clicking editable field and not for readonly field', () => {
    fixture.detectChanges();

    (component as any).activarEdicionDesdeCampo('carrera');
    expect((component as any).editMode).toBeFalse();

    (component as any).activarEdicionDesdeCampo('nombre');
    expect((component as any).editMode).toBeTrue();
  });

  it('saves profile update and opens success modal', () => {
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

    expect(perfilServiceSpy.updatePerfil).toHaveBeenCalled();
    expect((component as any).showSuccessModal).toBeTrue();
    expect((component as any).editMode).toBeFalse();
    expect(authSessionSpy.setCurrentUsername).toHaveBeenCalledWith('diego2');
  });
});
