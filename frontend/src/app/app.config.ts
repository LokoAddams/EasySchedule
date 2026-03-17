import {
  APP_INITIALIZER,
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';
import { FeatureToggleService } from './services/feature-toggle.service';
import { LanguageService } from './core/services/language.service';

export function initializeFeatureFlags(featureToggleService: FeatureToggleService): () => Promise<void> {
  return () => featureToggleService.loadFlags();
}

export function initializeLanguage(languageService: LanguageService): () => Promise<void> {
  return () => languageService.initializeDefaultLanguage();
}



export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),

    importProvidersFrom(
      HttpClientModule,
      ReactiveFormsModule,
    ),
    ...provideTranslateService({
      lang: 'es',
      fallbackLang: 'es',
    }),
    ...provideTranslateHttpLoader({
      prefix: './assets/i18n/',
      suffix: '.json',
    }),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeFeatureFlags,
      multi: true,
      deps: [FeatureToggleService],
    },
    {
      provide: APP_INITIALIZER,
      useFactory: initializeLanguage,
      multi: true,
      deps: [LanguageService],
    },
  ]
};