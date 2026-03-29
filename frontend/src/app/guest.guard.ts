import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthSessionService } from './core/services/auth-session.service';

export const guestGuard: CanActivateFn = () => {
  const authSessionService = inject(AuthSessionService);
  const router = inject(Router);

  if (!authSessionService.isLoggedIn()) {
    return true;
  }

  return authSessionService.isProfileCompleted()
    ? router.createUrlTree(['/home'])
    : router.createUrlTree(['/perfil']);
};
