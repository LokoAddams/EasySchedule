import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, firstValueFrom, Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';

export interface FeatureFlags {
  malla: boolean;
  tomaMaterias: boolean;
  ofertasImport: boolean;
}

export type FeatureName = keyof FeatureFlags;

export interface FeatureToggleDefinition {
  name: FeatureName;
  labelKey: string;
  descriptionKey: string;
}

export interface FeatureToggleUpdateRequest {
  enabled: boolean;
}

export const FEATURE_TOGGLE_DEFINITIONS: FeatureToggleDefinition[] = [
  {
    name: 'malla',
    labelKey: 'featureToggles.items.malla.name',
    descriptionKey: 'featureToggles.items.malla.description',
  },
  {
    name: 'tomaMaterias',
    labelKey: 'featureToggles.items.tomaMaterias.name',
    descriptionKey: 'featureToggles.items.tomaMaterias.description',
  },
  {
    name: 'ofertasImport',
    labelKey: 'featureToggles.items.ofertasImport.name',
    descriptionKey: 'featureToggles.items.ofertasImport.description',
  },
];

@Injectable({
  providedIn: 'root',
})
export class FeatureToggleService {
  private flags: FeatureFlags = {
    malla: false,
    tomaMaterias: false,
    ofertasImport: true,
  };
  private readonly flagsSubject = new BehaviorSubject<FeatureFlags>(this.flags);
  readonly flags$ = this.flagsSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  loadFlags(): Promise<void> {
    return firstValueFrom(this.fetchFlags())
      .then((flags) => {
        this.setFlags(flags);
      })
      .catch((error) => {
        console.error('Failed to load feature flags from backend:', error);
      });
  }

  refreshFlags(): Observable<FeatureFlags> {
    return this.fetchFlags();
  }

  updateFlag(featureName: FeatureName, enabled: boolean): Observable<FeatureFlags> {
    return this.http.put<FeatureFlags>(
      this.buildUrl(`/api/features/${featureName}`),
      { enabled } satisfies FeatureToggleUpdateRequest,
    ).pipe(
      tap((flags) => this.setFlags(flags)),
    );
  }

  getSnapshot(): FeatureFlags {
    return { ...this.flags };
  }

  isEnabled(featureName: FeatureName): boolean {
    if (!(featureName in this.flags)) {
      return false;
    }

    return this.flags[featureName as keyof FeatureFlags];
  }

  private fetchFlags(): Observable<FeatureFlags> {
    return this.http.get<FeatureFlags>(this.buildUrl('/api/features')).pipe(
      tap((flags) => this.setFlags(flags)),
    );
  }

  private setFlags(flags: FeatureFlags): void {
    this.flags = {
      ...this.flags,
      ...flags,
    };
    this.flagsSubject.next(this.flags);
  }

  private buildUrl(path: string): string {
    const baseUrl = environment.backendUrl.replace(/\/$/, '');

    if (!path.startsWith('/')) {
      return `${baseUrl}/${path}`;
    }

    return `${baseUrl}${path}`;
  }
}
