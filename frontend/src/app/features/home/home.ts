import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/services/auth-session.service';

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

    void this.router.navigate(['/malla']);
  }
}
