import { Injectable } from '@angular/core';
import { firstValueFrom, BehaviorSubject, Observable } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root',
})
export class LanguageService {
  private readonly supportedLanguages = ['es', 'en', 'pt'];
  private currentLanguage = 'es';
  private readonly currentLanguageSubject: BehaviorSubject<string>;

  constructor(private readonly translate: TranslateService) {
    this.currentLanguageSubject = new BehaviorSubject<string>(this.currentLanguage);
  }

  initializeDefaultLanguage(): Promise<void> {
    const browserLanguage = this.translate.getBrowserLang();
    const initialLanguage = browserLanguage && this.supportedLanguages.includes(browserLanguage)
      ? browserLanguage
      : 'es';

    this.translate.addLangs(this.supportedLanguages);
    this.translate.setDefaultLang('es');
    this.currentLanguage = initialLanguage;
    this.currentLanguageSubject.next(initialLanguage);
    return firstValueFrom(this.translate.use(initialLanguage)).then(() => undefined);
  }

  setLanguage(lang: string): void {
    if (!this.supportedLanguages.includes(lang)) {
      return;
    }

    this.currentLanguage = lang;
    this.currentLanguageSubject.next(lang);
    this.translate.use(lang);
  }

  getCurrentLanguage(): string {
    return this.currentLanguage;
  }

  getCurrentLanguage$(): Observable<string> {
    return this.currentLanguageSubject.asObservable();
  }
}
