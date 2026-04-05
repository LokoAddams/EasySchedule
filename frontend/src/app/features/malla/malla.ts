import { NgFor, NgIf } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom, Subscription } from 'rxjs';

import { FeatureToggleService } from '../../services/feature-toggle.service';
import { UniversidadCatalogoItem, UniversidadService } from '../../services/academico/universidad.service';

@Component({
  selector: 'app-malla',
  imports: [FormsModule, NgFor, NgIf, TranslatePipe],
  templateUrl: './malla.html',
  styleUrl: './malla.scss',
})
export class Malla implements OnInit, OnDestroy {
  protected mallaEnabled = false;
  protected universidades: UniversidadCatalogoItem[] = [];
  protected selectedUniversidadId: number | null = null;
  protected loadingUniversidades = true;
  protected loadUniversidadesError = false;
  protected universidadRequiredError = false;

  private flagsSubscription?: Subscription;

  constructor(
    private readonly featureService: FeatureToggleService,
    private readonly universidadService: UniversidadService,
  ) {}

  ngOnInit(): void {
    this.flagsSubscription = this.featureService.flags$.subscribe((flags) => {
      this.mallaEnabled = flags.malla;
    });

    void this.featureService.loadFlags();
    void this.loadUniversidades();
  }

  ngOnDestroy(): void {
    this.flagsSubscription?.unsubscribe();
  }

  protected retryLoadUniversidades(): void {
    void this.loadUniversidades();
  }

  protected onUniversidadChange(selectedUniversidadId: number | null): void {
    this.selectedUniversidadId = selectedUniversidadId;
    this.universidadRequiredError = false;
  }

  protected onGuardarUniversidadClick(): void {
    if (this.selectedUniversidadId === null) {
      this.universidadRequiredError = true;
      return;
    }
  }

  private async loadUniversidades(): Promise<void> {
    this.loadingUniversidades = true;
    this.loadUniversidadesError = false;

    try {
      this.universidades = await firstValueFrom(this.universidadService.getUniversidadesActivas());
    } catch {
      this.universidades = [];
      this.loadUniversidadesError = true;
    } finally {
      this.loadingUniversidades = false;
    }
  }
}
