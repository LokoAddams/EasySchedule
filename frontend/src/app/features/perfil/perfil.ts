import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AuthSessionService } from '../../core/services/auth-session.service';
import { LanguageService } from '../../core/services/language.service';
import { PerfilResponse } from './perfil.model';
import { PerfilService } from './perfil.service';

@Component({
  selector: 'app-perfil',
  imports: [CommonModule, RouterLink, TranslatePipe],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss',
})
export class Perfil implements OnInit {
  protected perfil: PerfilResponse | null = null;
  protected loading = true;
  protected errorKey = '';

  constructor(
    private readonly perfilService: PerfilService,
    private readonly authSessionService: AuthSessionService,
    private readonly languageService: LanguageService,
  ) {}

  ngOnInit(): void {
    const username = this.authSessionService.getCurrentUsername();

    if (!username) {
      this.loading = false;
      this.errorKey = 'perfil.error.noSession';
      return;
    }

    this.perfilService.getPerfilByUsername(username).subscribe({
      next: (perfilResponse) => {
        this.loading = false;
        this.perfil = perfilResponse;
        this.authSessionService.setCurrentUsername(perfilResponse.username);
      },
      error: () => {
        this.loading = false;
        this.errorKey = 'perfil.error.loadFailed';
      },
    });
  }

  protected getNombreCompleto(): string {
    if (!this.perfil) {
      return '';
    }

    const nombre = (this.perfil.nombre ?? '').trim();
    const apellido = (this.perfil.apellido ?? '').trim();
    const nombreCompleto = `${nombre} ${apellido}`.trim();

    return nombreCompleto || this.perfil.username;
  }

  protected getFechaNacimientoFormateada(): string {
    if (!this.perfil?.fechaNacimiento) {
      return '-';
    }

    const fechaNacimiento = new Date(`${this.perfil.fechaNacimiento}T00:00:00`);
    const locale = this.languageService.getCurrentLanguage();

    return new Intl.DateTimeFormat(locale, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    }).format(fechaNacimiento);
  }

  protected getValorSeguro(value: string | null): string {
    const valueNormalized = (value ?? '').trim();
    return valueNormalized || '-';
  }
}
