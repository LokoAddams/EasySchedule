import { Component, OnDestroy, OnInit, ViewChild, ElementRef, HostListener } from '@angular/core';
import { NgIf } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription, filter } from 'rxjs';

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
  protected currentLanguage: string = 'es';
  private flagsSubscription?: Subscription;
  private profileCompletedSubscription?: Subscription;
  private tomaMateriasPopoverSubscription?: Subscription;
  private languageSubscription?: Subscription;
  private routerSubscription?: Subscription;
  private languageMenuOpen = false;

  @ViewChild('mallaPopover') mallaPopover?: NgbPopover;
  @ViewChild('tomaMateriasPopover') tomaMateriasPopover?: NgbPopover;
  @ViewChild('languageMenu') languageMenuRef?: ElementRef<HTMLDetailsElement>;

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
        }, 5000);
      }
    });

    this.tomaMateriasPopoverSubscription = this.tourHintsService.tomaMateriasPopoverOpen.subscribe((shouldOpen) => {
      if (shouldOpen && this.tomaMateriasPopover) {
        this.tomaMateriasPopover.open();
      } else if (!shouldOpen && this.tomaMateriasPopover) {
        this.tomaMateriasPopover.close();
      }
    });

    this.currentLanguage = this.languageService.getCurrentLanguage();
    this.languageSubscription = this.languageService.getCurrentLanguage$().subscribe((lang) => {
      this.currentLanguage = lang;
    });

    this.routerSubscription = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.closeLanguageMenu();
      this.closeMobileMenu();
    });
  }

  ngOnDestroy(): void {
    this.flagsSubscription?.unsubscribe();
    this.profileCompletedSubscription?.unsubscribe();
    this.tomaMateriasPopoverSubscription?.unsubscribe();
    this.languageSubscription?.unsubscribe();
    this.routerSubscription?.unsubscribe();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const clickedInside = this.languageMenuRef?.nativeElement?.contains(event.target as Node);
    if (!clickedInside && this.languageMenuRef?.nativeElement?.open) {
      this.closeLanguageMenu();
    }
  }

  protected isMobileMenuOpen = false;

  protected toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  protected closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  protected setLanguage(lang: string): void {
    this.languageService.setLanguage(lang);
    this.closeLanguageMenu();
  }

  private closeLanguageMenu(): void {
    if (this.languageMenuRef?.nativeElement?.open) {
      this.languageMenuRef.nativeElement.open = false;
    }
  }

  protected isLoggedIn(): boolean {
    return this.authSessionService.isLoggedIn();
  }

  protected isProfileCompleted(): boolean {
    return this.authSessionService.isProfileCompleted();
  }

  protected isAdmin(): boolean {
    return this.authSessionService.isAdmin();
  }

  protected isCurrentLanguage(lang: string): boolean {
    return this.currentLanguage === lang;
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
