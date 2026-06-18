import { NgClass, NgFor, NgIf } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import {
  FeatureToggle,
  FeatureToggleService,
} from '../../services/feature-toggle.service';
import { LanguageService } from '../../core/services/language.service';
import { ToastService } from '../../core/services/toast.service';

interface PendingToggleUpdate {
  toggle: FeatureToggle;
  active: boolean;
}

@Component({
  selector: 'app-admin-feature-toggles',
  imports: [NgClass, NgFor, NgIf, TranslateModule],
  templateUrl: './admin-feature-toggles.html',
  styleUrl: './admin-feature-toggles.scss',
})
export class AdminFeatureToggles implements OnInit {
  protected toggles: FeatureToggle[] = [];
  protected loading = true;
  protected error: string | null = null;
  protected savingKey: string | null = null;
  protected pendingUpdate: PendingToggleUpdate | null = null;
  protected currentLanguage = 'es';

  private readonly featureToggleService = inject(FeatureToggleService);
  private readonly languageService = inject(LanguageService);
  private readonly translateService = inject(TranslateService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.currentLanguage = this.languageService.getCurrentLanguage();
    this.languageService
      .getCurrentLanguage$()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((language) => {
        this.currentLanguage = language;
      });

    this.loadToggles();
  }

  protected loadToggles(): void {
    this.loading = true;
    this.error = null;

    this.featureToggleService
      .getToggles()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (toggles) => {
          this.toggles = toggles.filter(t => !['malla', 'tomaMaterias', 'ofertasImport'].includes(t.key as string));
          this.loading = false;
        },
        error: (error: HttpErrorResponse) => {
          this.error = this.resolveError(error);
          this.loading = false;
        },
      });
  }

  protected updateToggle(toggle: FeatureToggle): void {
    this.pendingUpdate = {
      toggle,
      active: !toggle.active,
    };
  }

  protected cancelToggleUpdate(): void {
    if (this.savingKey) {
      return;
    }

    this.pendingUpdate = null;
  }

  protected confirmToggleUpdate(): void {
    if (!this.pendingUpdate) {
      return;
    }

    const { toggle, active } = this.pendingUpdate;
    this.savingKey = toggle.key as string;
    this.featureToggleService
      .updateToggle(toggle.key, active)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedToggle) => {
          this.toggles = this.toggles.map((item) =>
            item.key === updatedToggle.key ? updatedToggle : item,
          );
          this.savingKey = null;
          this.pendingUpdate = null;
          this.toastService.success('adminFeatureToggles.success.updated');
        },
        error: (error: HttpErrorResponse) => {
          this.savingKey = null;
          this.toastService.error('adminFeatureToggles.error.update', 3600, {
            message: this.resolveError(error),
          });
        },
      });
  }

  protected isSaving(toggle: FeatureToggle): boolean {
    return this.savingKey === toggle.key;
  }

  protected toggleNameKey(toggle: FeatureToggle): string {
    return `adminFeatureToggles.toggles.${toggle.key}.name`;
  }

  protected toggleDescriptionKey(toggle: FeatureToggle): string {
    return `adminFeatureToggles.toggles.${toggle.key}.description`;
  }

  protected toggleActionKey(toggle: FeatureToggle): string {
    return toggle.active
      ? 'adminFeatureToggles.actions.deactivate'
      : 'adminFeatureToggles.actions.activate';
  }

  protected pendingActionKey(): string {
    return this.pendingUpdate?.active
      ? 'adminFeatureToggles.actions.activate'
      : 'adminFeatureToggles.actions.deactivate';
  }

  protected pendingTitleKey(): string {
    return this.pendingUpdate?.active
      ? 'adminFeatureToggles.confirm.activateTitle'
      : 'adminFeatureToggles.confirm.deactivateTitle';
  }

  protected pendingMessageKey(): string {
    return this.pendingUpdate?.active
      ? 'adminFeatureToggles.confirm.activateMessage'
      : 'adminFeatureToggles.confirm.deactivateMessage';
  }

  protected pendingToggleName(): string {
    if (!this.pendingUpdate) {
      return '';
    }

    return this.translateService.instant(this.toggleNameKey(this.pendingUpdate.toggle));
  }

  protected formatUpdatedAt(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return this.translateService.instant('adminFeatureToggles.updatedAtUnavailable');
    }

    return new Intl.DateTimeFormat(this.currentLanguage, {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(date);
  }

  private resolveError(error: HttpErrorResponse): string {
    if (error.status === 401) {
      return this.translateService.instant('adminFeatureToggles.error.unauthorized');
    }

    if (error.status === 403) {
      return this.translateService.instant('adminFeatureToggles.error.forbidden');
    }

    const message = error.error?.message;
    if (typeof message === 'string' && message.trim()) {
      return message;
    }

    return this.translateService.instant('adminFeatureToggles.error.generic');
  }
}
