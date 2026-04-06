import { NgFor, NgIf } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom, Subscription } from 'rxjs';

import { CarreraCatalogoItem, CarreraService } from '../../services/academico/carrera.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';
import { MallaCatalogoItem, MallaCatalogoService } from '../../services/academico/malla-catalogo.service';
import {
  SeleccionAcademica,
  SeleccionAcademicaService,
} from '../../services/academico/seleccion-academica.service';
import { UniversidadCatalogoItem, UniversidadService } from '../../services/academico/universidad.service';

type SeleccionStep = 'universidad' | 'carrera' | 'malla' | 'resumen';

@Component({
  selector: 'app-malla',
  imports: [FormsModule, NgFor, NgIf, TranslatePipe],
  templateUrl: './malla.html',
  styleUrl: './malla.scss',
})
export class Malla implements OnInit, OnDestroy {
  protected mallaEnabled = false;
  protected step: SeleccionStep = 'universidad';

  protected universidades: UniversidadCatalogoItem[] = [];
  protected carreras: CarreraCatalogoItem[] = [];
  protected mallas: MallaCatalogoItem[] = [];

  protected selectedUniversidadId: number | null = null;
  protected selectedCarreraId: number | null = null;
  protected selectedMallaId: number | null = null;

  protected selectedResumen: SeleccionAcademica | null = null;

  protected loadingUniversidades = true;
  protected loadingCarreras = false;
  protected loadingMallas = false;
  protected savingSeleccion = false;

  protected loadUniversidadesError = false;
  protected loadCarrerasError = false;
  protected loadMallasError = false;
  protected saveSeleccionError = false;

  protected universidadRequiredError = false;
  protected carreraRequiredError = false;
  protected mallaRequiredError = false;

  private flagsSubscription?: Subscription;

  constructor(
    private readonly featureService: FeatureToggleService,
    private readonly universidadService: UniversidadService,
    private readonly carreraService: CarreraService,
    private readonly mallaCatalogoService: MallaCatalogoService,
    private readonly seleccionAcademicaService: SeleccionAcademicaService,
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

    // Al cambiar universidad, se reinicia la selección dependiente.
    this.selectedCarreraId = null;
    this.selectedMallaId = null;
    this.carreras = [];
    this.mallas = [];
    this.carreraRequiredError = false;
    this.mallaRequiredError = false;
    this.loadCarrerasError = false;
    this.loadMallasError = false;
  }

  protected onCarreraChange(selectedCarreraId: number | null): void {
    this.selectedCarreraId = selectedCarreraId;
    this.carreraRequiredError = false;

    this.selectedMallaId = null;
    this.mallas = [];
    this.mallaRequiredError = false;
    this.loadMallasError = false;
  }

  protected onMallaChange(selectedMallaId: number | null): void {
    this.selectedMallaId = selectedMallaId;
    this.mallaRequiredError = false;
    this.saveSeleccionError = false;
  }

  protected onGuardarUniversidadClick(): void {
    if (this.selectedUniversidadId === null) {
      this.universidadRequiredError = true;
      return;
    }

    void this.loadCarreras(this.selectedUniversidadId);
  }

  protected onGuardarCarreraClick(): void {
    if (this.selectedCarreraId === null) {
      this.carreraRequiredError = true;
      return;
    }

    void this.loadMallas(this.selectedCarreraId);
  }

  protected onGuardarMallaClick(): void {
    if (this.selectedUniversidadId === null || this.selectedCarreraId === null || this.selectedMallaId === null) {
      this.mallaRequiredError = this.selectedMallaId === null;
      return;
    }

    void this.guardarSeleccion();
  }

  protected getResumenUniversidad(): string {
    const nombre = this.selectedResumen?.universidad;
    return (nombre ?? '').trim();
  }

  protected getResumenCarrera(): string {
    const nombre = this.selectedResumen?.carrera;
    return (nombre ?? '').trim();
  }

  protected getResumenMalla(): string {
    const nombre = this.selectedResumen?.malla;
    return (nombre ?? '').trim();
  }

  private async loadUniversidades(): Promise<void> {
    this.loadingUniversidades = true;
    this.loadUniversidadesError = false;

    try {
      this.universidades = await firstValueFrom(this.universidadService.getUniversidadesActivas());
      await this.loadSeleccionActual();
    } catch {
      this.universidades = [];
      this.loadUniversidadesError = true;
    } finally {
      this.loadingUniversidades = false;
    }
  }

  private async loadSeleccionActual(): Promise<void> {
    try {
      const seleccion = await firstValueFrom(this.seleccionAcademicaService.getSeleccionActual());
      if (seleccion.universidadId === null || seleccion.carreraId === null || seleccion.mallaId === null) {
        return;
      }

      this.selectedUniversidadId = seleccion.universidadId;
      this.selectedCarreraId = seleccion.carreraId;
      this.selectedMallaId = seleccion.mallaId;
      this.selectedResumen = seleccion;

      await this.loadCarreras(seleccion.universidadId, false);
      await this.loadMallas(seleccion.carreraId, false);
      this.step = 'resumen';
    } catch {
      // Si no existe selección previa o falla la carga, se mantiene el flujo normal.
    }
  }

  private async loadCarreras(universidadId: number, avanzarStep = true): Promise<void> {
    this.loadingCarreras = true;
    this.loadCarrerasError = false;

    try {
      this.carreras = await firstValueFrom(this.carreraService.getCarrerasActivasPorUniversidad(universidadId));
      if (avanzarStep) {
        this.step = 'carrera';
      }
    } catch {
      this.carreras = [];
      this.loadCarrerasError = true;
    } finally {
      this.loadingCarreras = false;
    }
  }

  private async loadMallas(carreraId: number, avanzarStep = true): Promise<void> {
    this.loadingMallas = true;
    this.loadMallasError = false;

    try {
      this.mallas = await firstValueFrom(this.mallaCatalogoService.getMallasActivasPorCarrera(carreraId));
      if (avanzarStep) {
        this.step = 'malla';
      }
    } catch {
      this.mallas = [];
      this.loadMallasError = true;
    } finally {
      this.loadingMallas = false;
    }
  }

  private async guardarSeleccion(): Promise<void> {
    if (this.selectedUniversidadId === null || this.selectedCarreraId === null || this.selectedMallaId === null) {
      return;
    }

    this.savingSeleccion = true;
    this.saveSeleccionError = false;

    try {
      this.selectedResumen = await firstValueFrom(
        this.seleccionAcademicaService.guardarSeleccion({
          universidadId: this.selectedUniversidadId,
          carreraId: this.selectedCarreraId,
          mallaId: this.selectedMallaId,
        }),
      );
      this.step = 'resumen';
    } catch {
      this.saveSeleccionError = true;
    } finally {
      this.savingSeleccion = false;
    }
  }
}
