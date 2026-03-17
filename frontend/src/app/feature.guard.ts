import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { FeatureToggleService } from './services/feature-toggle.service';

export const featureGuard = (featureName: string): CanActivateFn => {
  return () => {
    const featureToggleService = inject(FeatureToggleService);
    const router = inject(Router);

    if (featureToggleService.isEnabled(featureName)) {
      return true;
    }

    return router.createUrlTree(['/home']);
  };
};
