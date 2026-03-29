import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgIf } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

import { LanguageService } from '../../core/services/language.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, NgIf, TranslatePipe],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit, OnDestroy {
  protected mallaEnabled = false;
  protected tomaMateriasEnabled = false;
  private flagsSubscription?: Subscription;

  constructor(
    private readonly router: Router,
    private readonly languageService: LanguageService,
    private readonly authSessionService: AuthSessionService,
    private readonly featureToggleService: FeatureToggleService,
    private readonly apiService: ApiService,
  ) {}

  ngOnInit(): void {
    this.flagsSubscription = this.featureToggleService.flags$.subscribe((flags) => {
      this.mallaEnabled = flags.malla;
      this.tomaMateriasEnabled = flags.tomaMaterias;
    });

    void this.featureToggleService.loadFlags();
  }

  ngOnDestroy(): void {
    this.flagsSubscription?.unsubscribe();
  }

  protected setLanguage(lang: string): void {
    this.languageService.setLanguage(lang);
  }
  protected isLoggedIn(): boolean {
    return this.authSessionService.isLoggedIn();
  }

  protected isProfileCompleted(): boolean {
    return this.authSessionService.isProfileCompleted();
  }
  
  protected logout(): void {
    this.apiService.post<{ message: string }, Record<string, never>>('/api/logout', {}).subscribe({
      next: () => {},
      error: () => {},
    });
    this.authSessionService.clearSession();
    void this.router.navigate(['/home']);
  }
}
