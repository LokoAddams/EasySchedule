import { NgFor, NgIf } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

import { OfertaImportService, OfertaListResponse } from '../../../services/academico/oferta-import.service';

@Component({
  selector: 'app-gestionar-ofertas',
  imports: [NgIf, NgFor, FormsModule, TranslatePipe],
  templateUrl: './gestionar-ofertas.html',
  styleUrl: './gestionar-ofertas.scss',
})
export class GestionarOfertas implements OnInit {
  @Input({ required: true }) mallaId: number | null = null;
  @Output() closeModal = new EventEmitter<void>();

  protected ofertas: OfertaListResponse[] = [];
  protected semestres: string[] = [];
  protected paralelos: string[] = [];
  protected loading = false;
  protected loaded = false;

  protected searchTerm = '';
  protected selectedSemestre = '';
  protected selectedParalelo = '';

  constructor(private readonly ofertaImportService: OfertaImportService) {}

  async ngOnInit(): Promise<void> {
    if (this.mallaId === null) return;
    await this.loadFilterOptions();
    await this.loadOfertas();
  }

  protected async onSearchChange(): Promise<void> {
    await this.loadOfertas();
  }

  protected async onFilterChange(): Promise<void> {
    await this.loadOfertas();
  }

  protected async clearFilters(): Promise<void> {
    this.searchTerm = '';
    this.selectedSemestre = '';
    this.selectedParalelo = '';
    await this.loadOfertas();
  }

  protected cancel(): void {
    this.closeModal.emit();
  }

  private async loadFilterOptions(): Promise<void> {
    if (this.mallaId === null) return;
    try {
      const [semestres, paralelos] = await Promise.all([
        firstValueFrom(this.ofertaImportService.listarSemestres(this.mallaId)),
        firstValueFrom(this.ofertaImportService.listarParalelos(this.mallaId)),
      ]);
      this.semestres = semestres;
      this.paralelos = paralelos;
    } catch {
      this.semestres = [];
      this.paralelos = [];
    }
  }

  private async loadOfertas(): Promise<void> {
    if (this.mallaId === null) return;
    this.loading = true;
    try {
      this.ofertas = await firstValueFrom(
        this.ofertaImportService.listarOfertas(
          this.mallaId,
          this.searchTerm || undefined,
          this.selectedSemestre || undefined,
          this.selectedParalelo || undefined,
        ),
      );
      this.loaded = true;
    } catch {
      this.ofertas = [];
      this.loaded = true;
    } finally {
      this.loading = false;
    }
  }
}
