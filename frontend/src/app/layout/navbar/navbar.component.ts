import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { NgIf } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

import { LanguageService } from '../../core/services/language.service';
import { NgbPopover, NgbPopoverModule } from '@ng-bootstrap/ng-bootstrap';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';
import { ApiService } from '../../services/api.service';
import { TourHintsService } from '../../services/tour-hints.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, NgIf, TranslatePipe, NgbPopoverModule],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit, OnDestroy {
  protected mallaEnabled = false;
  protected tomaMateriasEnabled = false;
  private flagsSubscription?: Subscription;
  private profileCompletedSubscription?: Subscription;
  private tomaMateriasPopoverSubscription?: Subscription;

  @ViewChild('mallaPopover') mallaPopover?: NgbPopover;
  @ViewChild('tomaMateriasPopover') tomaMateriasPopover?: NgbPopover;

  constructor(
    private readonly router: Router,
    private readonly languageService: LanguageService,
    private readonly authSessionService: AuthSessionService,
    private readonly featureToggleService: FeatureToggleService,
    private readonly apiService: ApiService,
    private readonly tourHintsService: TourHintsService,
  ) {}

  ngOnInit(): void {
    this.flagsSubscription = this.featureToggleService.flags$.subscribe((flags) => {
      this.mallaEnabled = flags.malla;
      this.tomaMateriasEnabled = flags.tomaMaterias;
    });

    void this.featureToggleService.loadFlags();

    this.profileCompletedSubscription = this.authSessionService.profileCompleted$.subscribe((completed) => {
      if (completed && this.mallaPopover) {
        this.mallaPopover.open();
        setTimeout(() => {
          if (this.mallaPopover?.isOpen()) {
            this.mallaPopover.close();
          }
        }, 5000); // auto close after 5 seconds
      }
    });

    // Suscribirse a los cambios de tour hints
    this.tomaMateriasPopoverSubscription = this.tourHintsService.tomaMateriasPopoverOpen.subscribe((shouldOpen) => {
      if (shouldOpen && this.tomaMateriasPopover) {
        this.tomaMateriasPopover.open();
      } else if (!shouldOpen && this.tomaMateriasPopover) {
        this.tomaMateriasPopover.close();
      }
    });
  }

  ngOnDestroy(): void {
    this.flagsSubscription?.unsubscribe();
    this.profileCompletedSubscription?.unsubscribe();
    this.tomaMateriasPopoverSubscription?.unsubscribe();
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

  protected closeTourPopover(): void {
    this.tourHintsService.closeTomaMateriasPopover();
  }
}
