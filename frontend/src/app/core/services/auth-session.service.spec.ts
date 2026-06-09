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

  it('stores current email and admin flag', () => {
    service.setAuthToken('abc-token', 3600);
    service.setCurrentEmail('USER.ADMIN@EASYSCHEDULE.COM');
    service.setAdmin(true);

    expect(service.getCurrentEmail()).toBe('user.admin@easyschedule.com');
    expect(service.isAdmin()).toBeTrue();
  });

  it('clears session when token is expired', () => {
    service.setAuthToken('abc-token', 1);
    service.setCurrentEmail('user.admin@easyschedule.com');
    service.setAdmin(true);
    localStorage.setItem('easySchedule.tokenExpiresAt', String(Date.now() - 1000));

    expect(service.getAuthToken()).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getCurrentEmail()).toBeNull();
    expect(service.isAdmin()).toBeFalse();
    expect(localStorage.getItem('easySchedule.token')).toBeNull();
  });
});
