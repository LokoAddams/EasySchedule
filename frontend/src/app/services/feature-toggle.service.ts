import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, firstValueFrom, tap } from 'rxjs';

import { environment } from '../../environments/environment';

export interface FeatureFlags {
  [key: string]: boolean;
}

export type FeatureName = keyof FeatureFlags;

export interface FeatureToggle {
  key: FeatureName;
  name: string;
  description: string;
  active: boolean;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class FeatureToggleService {
  private flags: FeatureFlags = {};
  private readonly flagsSubject = new BehaviorSubject<FeatureFlags>(this.flags);
  readonly flags$ = this.flagsSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  loadFlags(): Promise<void> {
    return firstValueFrom(
      this.http.get<FeatureFlags>(`${environment.backendUrl}/api/features`),
    )
      .then((flags) => {
        this.flags = {
          ...this.flags,
          ...flags,
        };
        this.flagsSubject.next(this.flags);
      })
      .catch((error) => {
        console.error('Failed to load feature flags from backend:', error);
      });
  }

  isEnabled(featureName: FeatureName): boolean {
    if (!(featureName in this.flags)) {
      return false;
    }

    return this.flags[featureName as keyof FeatureFlags];
  }

  getToggles(): Observable<FeatureToggle[]> {
    return this.http.get<FeatureToggle[]>(`${environment.backendUrl}/api/features/toggles`);
  }

  updateToggle(featureName: FeatureName, active: boolean): Observable<FeatureToggle> {
    return this.http
      .patch<FeatureToggle>(`${environment.backendUrl}/api/features/toggles/${featureName}`, { active })
      .pipe(
        tap((toggle) => {
          this.flags = {
            ...this.flags,
            [toggle.key]: toggle.active,
          };
          this.flagsSubject.next(this.flags);
        }),
      );
  }
}
