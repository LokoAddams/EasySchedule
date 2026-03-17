import { Component, OnInit } from '@angular/core';
import { NgIf } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { LanguageService } from '../../core/services/language.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, NgIf, TranslatePipe],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent implements OnInit {
  protected mallaEnabled = false;
  protected tomaMateriasEnabled = false;

  constructor(
    private readonly languageService: LanguageService,
    private readonly featureToggleService: FeatureToggleService,
  ) {}

  ngOnInit(): void {
    this.mallaEnabled = this.featureToggleService.isEnabled('malla');
    this.tomaMateriasEnabled = this.featureToggleService.isEnabled('tomaMaterias');
  }

  protected setLanguage(lang: string): void {
    this.languageService.setLanguage(lang);
  }
}
