import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthSessionService } from './core/services/auth-session.service';

export const profileCompletedGuard: CanActivateFn = () => {
  const authSessionService = inject(AuthSessionService);
  const router = inject(Router);

  if (!authSessionService.isLoggedIn()) {
    return router.createUrlTree(['/home']);
  }

  if (authSessionService.isProfileCompleted()) {
    return true;
  }

  return router.createUrlTree(['/perfil']);
};
