import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/services/auth-session.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';

@Component({
  selector: 'app-home',
  imports: [TranslatePipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  constructor(
    private readonly authSessionService: AuthSessionService,
    private readonly router: Router,
    private readonly featureToggleService: FeatureToggleService,
  ) {}

  protected goToStart(): void {
    if (!this.authSessionService.isLoggedIn()) {
      void this.router.navigate(['/login']);
      return;
    }

    if (!this.authSessionService.isProfileCompleted()) {
      void this.router.navigate(['/perfil']);
      return;
    }

    if (this.featureToggleService.isEnabled('malla')) {
      void this.router.navigate(['/malla']);
      return;
    }

    if (this.featureToggleService.isEnabled('tomaMaterias')) {
      void this.router.navigate(['/toma-de-materias']);
    }
  }
}
