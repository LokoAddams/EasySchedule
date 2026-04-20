import { HttpErrorResponse, HttpHandler, HttpRequest } from '@angular/common/http';
import { of, throwError } from 'rxjs';

import { AuthSessionService } from '../services/auth-session.service';
import { AuthTokenInterceptor } from './auth-token.interceptor';

describe('AuthTokenInterceptor', () => {
  let interceptor: AuthTokenInterceptor;
  let authSessionServiceSpy: jasmine.SpyObj<AuthSessionService>;

  beforeEach(() => {
    authSessionServiceSpy = jasmine.createSpyObj<AuthSessionService>('AuthSessionService', ['getAuthToken', 'clearSession']);
    interceptor = new AuthTokenInterceptor(authSessionServiceSpy);
  });

  it('adds bearer token header for api requests', () => {
    authSessionServiceSpy.getAuthToken.and.returnValue('my-token');

    const request = new HttpRequest('GET', '/api/academico/universidades');
    const next: HttpHandler = {
      handle: jasmine.createSpy('handle').and.returnValue(of({} as never)),
    };

    interceptor.intercept(request, next).subscribe();

    const handledRequest = (next.handle as jasmine.Spy).calls.mostRecent().args[0] as HttpRequest<unknown>;
    expect(handledRequest.headers.get('Authorization')).toBe('Bearer my-token');
  });

  it('clears session on 401 responses', () => {
    authSessionServiceSpy.getAuthToken.and.returnValue('my-token');

    const request = new HttpRequest('GET', '/api/academico/universidades');
    const next: HttpHandler = {
      handle: () => throwError(() => new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' })),
    };

    interceptor.intercept(request, next).subscribe({
      error: () => {
        expect(authSessionServiceSpy.clearSession).toHaveBeenCalled();
      },
    });
  });

  it('clears session on 403 responses', () => {
    authSessionServiceSpy.getAuthToken.and.returnValue('my-token');

    const request = new HttpRequest('GET', '/api/academico/universidades');
    const next: HttpHandler = {
      handle: () => throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })),
    };

    interceptor.intercept(request, next).subscribe({
      error: () => {
        expect(authSessionServiceSpy.clearSession).toHaveBeenCalled();
      },
    });
  });
});
