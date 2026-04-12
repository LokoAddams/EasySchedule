import { NgFor, NgIf, NgClass } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom, Subscription } from 'rxjs';

import { CarreraCatalogoItem, CarreraService } from '../../services/academico/carrera.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';
import { MallaCatalogoItem, MallaCatalogoService, MallaMateria } from '../../services/academico/malla-catalogo.service';
import {
  SeleccionAcademica,
  SeleccionAcademicaService,
} from '../../services/academico/seleccion-academica.service';
import { UniversidadCatalogoItem, UniversidadService } from '../../services/academico/universidad.service';

type SeleccionStep = 'universidad' | 'carrera' | 'malla' | 'resumen';
type EditMode = 'universidad' | 'malla' | null;

interface SeleccionSnapshot {
  universidadId: number | null;
  carreraId: number | null;
  mallaId: number | null;
}

@Component({
  selector: 'app-malla',
  imports: [FormsModule, NgFor, NgIf, NgClass, TranslatePipe],
  templateUrl: './malla.html',
  styleUrl: './malla.scss',
})
export class Malla implements OnInit, OnDestroy {
  protected mallaEnabled = false;
  protected step: SeleccionStep = 'universidad';
  protected editMode: EditMode = null;

  protected universidades: UniversidadCatalogoItem[] = [];
  protected carreras: CarreraCatalogoItem[] = [];
  protected mallas: MallaCatalogoItem[] = [];

  protected selectedUniversidadId: number | null = null;
  protected selectedCarreraId: number | null = null;
  protected selectedMallaId: number | null = null;

  protected selectedResumen: SeleccionAcademica | null = null;
  protected materias: MallaMateria[] = [];
  protected materiasPorSemestre: Map<number, MallaMateria[]> = new Map();
  protected semestres: number[] = [];
  protected semestreActual: number = 3; // Mocked active semester
  protected loadingMaterias = false;
  protected loadMateriasError = false;

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
  protected mallaChangeWarningVisible = false;

  private flagsSubscription?: Subscription;
  private previousSelectionSnapshot: SeleccionSnapshot | null = null;

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

  protected onCambiarUniversidadClick(): void {
    this.editMode = 'universidad';
    this.step = 'universidad';
    this.mallaChangeWarningVisible = false;
    this.clearErrors();
    this.createSelectionSnapshot();
    this.onUniversidadChange(null);
  }

  protected onCambiarMallaClick(): void {
    if (this.selectedUniversidadId === null) {
      return;
    }

    void this.prepareMallaEditMode();
  }

  protected onCancelChangeClick(): void {
    this.restoreSelectionSnapshot();
    this.clearErrors();
    this.editMode = null;
    this.step = 'resumen';
    this.mallaChangeWarningVisible = false;
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

    if (this.editMode === 'malla' && selectedMallaId !== null) {
      const selectedMalla = this.mallas.find((malla) => malla.id === selectedMallaId);
      this.selectedCarreraId = selectedMalla?.carreraId ?? null;
    }
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

    if (this.editMode === 'malla' && this.previousSelectionSnapshot !== null) {
      const mallaAnteriorId = this.previousSelectionSnapshot.mallaId;
      const isChangingMalla = mallaAnteriorId !== null && this.selectedMallaId !== mallaAnteriorId;
      if (isChangingMalla) {
        const confirmed = window.confirm('Cambiar de malla reiniciara tu progreso en la malla anterior. ¿Deseas continuar?');
        if (!confirmed) {
          return;
        }
      }
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
      this.editMode = null;
      this.mallaChangeWarningVisible = false;
      this.previousSelectionSnapshot = null;
      void this.loadMaterias(this.selectedMallaId);
    } catch {
      // Si no existe selección previa o falla la carga, se mantiene el flujo normal.
    }
  }

  private async prepareMallaEditMode(): Promise<void> {
    if (this.selectedUniversidadId === null || this.selectedMallaId === null) {
      return;
    }

    this.editMode = 'malla';
    this.step = 'malla';
    this.mallaChangeWarningVisible = true;
    this.clearErrors();
    this.createSelectionSnapshot();

    this.loadingMallas = true;
    this.loadMallasError = false;

    try {
      const carreras = await firstValueFrom(this.carreraService.getCarrerasActivasPorUniversidad(this.selectedUniversidadId));
      this.carreras = carreras;
      const mallasPorCarrera = await Promise.all(
        carreras.map((carrera) => firstValueFrom(this.mallaCatalogoService.getMallasActivasPorCarrera(carrera.id))),
      );

      const mallasPlanas = mallasPorCarrera.flat();
      this.mallas = mallasPlanas.filter((malla, index, source) => source.findIndex((candidate) => candidate.id === malla.id) === index);

      if (!this.mallas.some((malla) => malla.id === this.selectedMallaId)) {
        this.selectedMallaId = null;
      }
    } catch {
      this.mallas = [];
      this.loadMallasError = true;
    } finally {
      this.loadingMallas = false;
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
      await this.loadSeleccionActual();
      this.step = 'resumen';
      void this.loadMaterias(this.selectedMallaId!);
    } catch {
      this.saveSeleccionError = true;
      if (this.editMode === 'malla') {
        this.restoreSelectionSnapshot();
        this.editMode = null;
        this.mallaChangeWarningVisible = false;
        this.step = 'resumen';
      }
    } finally {
      this.savingSeleccion = false;
    }
  }

  private clearErrors(): void {
    this.universidadRequiredError = false;
    this.carreraRequiredError = false;
    this.mallaRequiredError = false;
    this.loadCarrerasError = false;
    this.loadMallasError = false;
    this.saveSeleccionError = false;
  }

  private createSelectionSnapshot(): void {
    this.previousSelectionSnapshot = {
      universidadId: this.selectedUniversidadId,
      carreraId: this.selectedCarreraId,
      mallaId: this.selectedMallaId,
    };
  }

  private restoreSelectionSnapshot(): void {
    if (this.previousSelectionSnapshot === null) {
      return;
    }

    this.selectedUniversidadId = this.previousSelectionSnapshot.universidadId;
    this.selectedCarreraId = this.previousSelectionSnapshot.carreraId;
    this.selectedMallaId = this.previousSelectionSnapshot.mallaId;
  }

  private async loadMaterias(mallaId: number): Promise<void> {
    this.loadingMaterias = true;
    this.loadMateriasError = false;
    this.materias = [];
    this.materiasPorSemestre.clear();
    this.semestres = [];

    try {
      this.materias = await firstValueFrom(this.mallaCatalogoService.getMateriasPorMalla(mallaId));
      
      // Mocking status for features demonstration
      // Using deterministic logic based on ID and semestreSugerido to remain consistent
      this.materias.forEach(m => {
        if (m.semestreSugerido < this.semestreActual) {
          m.estado = 'APROBADA';
        } else if (m.semestreSugerido === this.semestreActual) {
          // Half of current semester is approved, rest cursando
          m.estado = m.id % 2 === 0 ? 'APROBADA' : 'CURSANDO';
        } else {
          m.estado = 'PENDIENTE';
        }
      });

      this.materias.forEach(materia => {
        const sem = materia.semestreSugerido;
        if (!this.materiasPorSemestre.has(sem)) {
          this.materiasPorSemestre.set(sem, []);
          this.semestres.push(sem);
        }
        this.materiasPorSemestre.get(sem)!.push(materia);
      });
      this.semestres.sort((a, b) => a - b);
      
    } catch {
      this.loadMateriasError = true;
    } finally {
      this.loadingMaterias = false;
    }
  }
}
