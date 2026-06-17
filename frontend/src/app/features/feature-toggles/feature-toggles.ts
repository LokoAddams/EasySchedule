import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgClass, NgFor, NgIf } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';

import { ToastService } from '../../core/services/toast.service';
import {
  FEATURE_TOGGLE_DEFINITIONS,
  FeatureFlags,
  FeatureName,
  FeatureToggleDefinition,
  FeatureToggleService,
} from '../../services/feature-toggle.service';

@Component({
  selector: 'app-feature-toggles',
  imports: [NgClass, NgFor, NgIf, TranslatePipe],
  templateUrl: './feature-toggles.html',
  styleUrl: './feature-toggles.scss',
})
export class FeatureToggles implements OnInit, OnDestroy {
  protected readonly toggleDefinitions = FEATURE_TOGGLE_DEFINITIONS;
  protected flags: FeatureFlags;
  protected loading = true;
  protected error = false;

  private readonly savingToggles = new Set<FeatureName>();
  private flagsSubscription?: Subscription;

  constructor(
    private readonly featureToggleService: FeatureToggleService,
    private readonly toastService: ToastService,
  ) {
    this.flags = this.featureToggleService.getSnapshot();
  }

  ngOnInit(): void {
    this.flagsSubscription = this.featureToggleService.flags$.subscribe((flags) => {
      this.flags = { ...flags };
    });

    this.featureToggleService.refreshFlags().subscribe({
      next: () => {
        this.loading = false;
        this.error = false;
      },
      error: () => {
        this.loading = false;
        this.error = true;
        this.toastService.error('featureToggles.toast.loadError');
      },
    });
  }

  ngOnDestroy(): void {
    this.flagsSubscription?.unsubscribe();
  }

  protected isEnabled(featureName: FeatureName): boolean {
    return this.flags[featureName];
  }

  protected isSaving(featureName: FeatureName): boolean {
    return this.savingToggles.has(featureName);
  }

  protected onToggleChange(featureName: FeatureName, event: Event): void {
    const input = event.target as HTMLInputElement;
    const enabled = input.checked;
    const previousValue = this.flags[featureName];

    this.flags = {
      ...this.flags,
      [featureName]: enabled,
    };
    this.savingToggles.add(featureName);
    this.error = false;

    this.featureToggleService.updateFlag(featureName, enabled).subscribe({
      next: () => {
        this.savingToggles.delete(featureName);
        this.toastService.success('featureToggles.toast.updateSuccess');
      },
      error: () => {
        this.flags = {
          ...this.flags,
          [featureName]: previousValue,
        };
        input.checked = previousValue;
        this.savingToggles.delete(featureName);
        this.toastService.error('featureToggles.toast.updateError');
      },
    });
  }

  protected trackToggle(_: number, toggle: FeatureToggleDefinition): FeatureName {
    return toggle.name;
  }
}
