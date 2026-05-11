import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../environments/environment';

export interface FeatureFlags {
  malla: boolean;
  tomaMaterias: boolean;
  ofertasImport: boolean;
}

export type FeatureName = keyof FeatureFlags;

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
}
