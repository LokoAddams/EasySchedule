import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthSessionService } from './core/services/auth-session.service';

export const adminGuard: CanActivateFn = () => {
  const authSessionService = inject(AuthSessionService);
  const router = inject(Router);

  if (authSessionService.isAdmin()) {
    return true;
  }

  return router.createUrlTree(['/home']);
};
