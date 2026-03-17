import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../environments/environment';

export interface FeatureFlags {
  malla: boolean;
  tomaMaterias: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class FeatureToggleService {
  //here we change the flags
  private flags: FeatureFlags = {
    malla: true,
    tomaMaterias: true,
  };

  constructor(private readonly http: HttpClient) {}

  loadFlags(): Promise<void> {
    return firstValueFrom(
      this.http.get<FeatureFlags>(`${environment.backendUrl}/api/features`),
    )
      .then((flags) => {
        this.flags = flags;
      })
      .catch((error) => {
        console.error('Failed to load feature flags from backend:', error);
      });
  }

  isEnabled(featureName: string): boolean {
    if (!(featureName in this.flags)) {
      return false;
    }

    return this.flags[featureName as keyof FeatureFlags];
  }
}
