import { AuthSessionService } from './auth-session.service';

describe('AuthSessionService', () => {
  let service: AuthSessionService;

  beforeEach(() => {
    localStorage.clear();
    service = new AuthSessionService();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('stores token with expiration and returns it while valid', () => {
    service.setAuthToken('abc-token', 3600);

    expect(service.getAuthToken()).toBe('abc-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('clears session when token is expired', () => {
    service.setAuthToken('abc-token', 1);
    localStorage.setItem('easySchedule.tokenExpiresAt', String(Date.now() - 1000));

    expect(service.getAuthToken()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('easySchedule.token')).toBeNull();
  });
});
