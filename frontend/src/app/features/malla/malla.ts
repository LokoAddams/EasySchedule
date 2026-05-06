import { NgFor, NgIf, NgClass } from '@angular/common';
import { Component, OnDestroy, OnInit, HostListener, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { filter, firstValueFrom, Subscription } from 'rxjs';
import { NgbPopover, NgbPopoverModule } from '@ng-bootstrap/ng-bootstrap';

import { CarreraCatalogoItem, CarreraService } from '../../services/academico/carrera.service';
import { EstadoMateriaService, EstadoMateriaItem, EstadoMateriaRequest } from '../../services/academico/estado-materia.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';
import { MallaCatalogoItem, MallaCatalogoService, MallaMateria } from '../../services/academico/malla-catalogo.service';
import {
  SeleccionAcademica,
  SeleccionAcademicaService,
} from '../../services/academico/seleccion-academica.service';
import { UniversidadCatalogoItem, UniversidadService } from '../../services/academico/universidad.service';
import { TomaSeleccionService } from '../../services/academico/toma-seleccion.service';
import { OfertaDetalleResponse, OfertaMateriaSimple } from '../../services/academico/malla-catalogo.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { PerfilService } from '../perfil/perfil.service';
import { TourHintsService } from '../../services/tour-hints.service';

type SeleccionStep = 'universidad' | 'carrera' | 'malla' | 'resumen';
type EditMode = 'universidad' | 'malla' | null;

interface SeleccionSnapshot {
  universidadId: number | null;
  carreraId: number | null;
  mallaId: number | null;
}

@Component({
  selector: 'app-malla',
  imports: [FormsModule, NgFor, NgIf, NgClass, TranslatePipe, NgbPopoverModule],
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
  protected semestreActual: number = 1;
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
  private routerEventsSubscription?: Subscription;
  private tomaSeleccionSubscription?: Subscription;
  private previousSelectionSnapshot: SeleccionSnapshot | null = null;
  private materiasLoadedForMallaId: number | null = null;

  protected showModal = false;
  protected showAccionesModal = false;
  protected selectedMateriaParaAccion: MallaMateria | null = null;
  protected materiaDetalle: OfertaDetalleResponse | null = null;
  protected loadingDetalle = false;
  protected selectedOfertaId: number | null = null;
  protected materiasSeleccionadas: Set<number> = new Set();

  public showImportModal = false;
  public importFile: File | null = null;
  public importFileName = '';
  public importLoading = false;
  public importError: string | null = null;
  public importSuccess: string | null = null;
  public importMallaName = '';
  public showTutorial = false;
  public fullPrompt = `Necesito que generes un archivo en formato CSV con las materias de una malla curricular distribuidas en semestres.

El CSV debe tener exactamente estos encabezados, en este orden:

codigo,nombre,semestre,creditos,prerequisitos

Reglas obligatorias:

- codigo: código único de la materia. Si la malla original no incluye código, genera códigos coherentes usando el prefijo de la carrera y el semestre, por ejemplo: SIS101, SIS102, SIS201, SIS202.
- nombre: nombre completo de la materia, respetando el nombre original de la malla.
- semestre: número entero del semestre al que pertenece la materia. Debe estar entre 1 y N, donde N es la cantidad total de semestres de la carrera.
- creditos: número entero entre 3 y 6. Si la malla original no indica créditos, asigna un valor razonable según la importancia de la materia.
- prerequisitos: códigos de materias separados por punto y coma ;. Si no tiene prerequisitos, dejar el campo vacío.
- Los códigos usados en prerequisitos deben existir previamente en la columna codigo.
- Una materia solo puede tener como prerequisito materias de semestres anteriores.
- No colocar espacios antes ni después del punto y coma en prerequisitos.
- No agregar columnas adicionales.
- No agregar explicaciones, comentarios ni texto fuera del CSV.
- La salida debe ser unicamente el contenido CSV válido.`;

  public get displayPrompt(): string {
    const maxLength = 180;
    if (this.fullPrompt.length > maxLength) {
      return this.fullPrompt.substring(0, maxLength) + '...';
    }
    return this.fullPrompt;
  }
  @ViewChild('popoverStep1') popoverStep1?: NgbPopover;
  @ViewChild('popoverStep2') popoverStep2?: NgbPopover;
  @ViewChild('popoverStep4') popoverStep4?: NgbPopover;

  protected tourStep = 0;

  constructor(
    private readonly featureService: FeatureToggleService,
    private readonly universidadService: UniversidadService,
    private readonly carreraService: CarreraService,
    private readonly mallaCatalogoService: MallaCatalogoService,
    private readonly estadoMateriaService: EstadoMateriaService,
    private readonly seleccionAcademicaService: SeleccionAcademicaService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly tomaSeleccionService: TomaSeleccionService,
    private readonly translateService: TranslateService,
    private readonly toastService: ToastService,
    private readonly authSessionService: AuthSessionService,
    private readonly perfilService: PerfilService,
    private readonly tourHintsService: TourHintsService,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.flagsSubscription = this.featureService.flags$.subscribe((flags) => {
      this.mallaEnabled = flags.malla;
    });

    this.routerEventsSubscription = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        if (event.urlAfterRedirects.includes('/malla') && !event.urlAfterRedirects.includes('/actualizar')) {
          this.materiasLoadedForMallaId = null;
          if (this.selectedMallaId !== null && this.step === 'resumen') {
            void this.loadMaterias(this.selectedMallaId);
          }
        }
      });

    this.tomaSeleccionSubscription = this.tomaSeleccionService.seleccion$.subscribe((materias) => {
      this.materiasSeleccionadas = new Set(materias.map(m => m.id));
    });

    void this.featureService.loadFlags();
    void this.loadUniversidades();
  }

  ngOnDestroy(): void {
    this.flagsSubscription?.unsubscribe();
    this.routerEventsSubscription?.unsubscribe();
    this.tomaSeleccionSubscription?.unsubscribe();
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

  protected showActualizarModal = false;
  protected selectedMateriaIdActualizar: number | null = null;
  protected selectedEstadoActualizar: 'APROBADA' | 'CURSANDO' | 'PENDIENTE' = 'PENDIENTE';
  protected savingEstado = false;

  protected onActualizarMallaClick(): void {
    if (this.selectedMallaId === null) {
      return;
    }

    this.showActualizarModal = true;
    if (this.showAccionesModal) {
      const materiaId = this.selectedMateriaParaAccion?.id ?? null;
      this.closeAccionesModal();
      this.selectedMateriaIdActualizar = materiaId;
    } else {
      this.selectedMateriaIdActualizar = this.materias.length > 0 ? this.materias[0].id : null;
    }
    this.onMateriaActualizarChange();
  }

  protected closeActualizarModal(): void {
    this.showActualizarModal = false;
  }

  protected onMateriaActualizarChange(): void {
    const materia = this.materias.find(m => m.id === this.selectedMateriaIdActualizar);
    if (materia) {
      this.selectedEstadoActualizar = this.mapEstadoBDToUI(materia.estado);
    }
  }

  protected async actualizarMateriaSeleccionada(): Promise<void> {
    if (this.selectedMateriaIdActualizar === null) return;
    
    if (this.selectedEstadoActualizar === 'CURSANDO') {
      this.toastService.error('malla.UpdateCourse.cursandoAutoAssigned');
      return;
    }

    this.savingEstado = true;
    try {
      const request: EstadoMateriaRequest = {
        mallaMateriaId: this.selectedMateriaIdActualizar,
        estado: this.mapEstadoUIToBD(this.selectedEstadoActualizar),
      };

      await firstValueFrom(this.estadoMateriaService.guardarEstado(request));

      const materia = this.materias.find(m => m.id === this.selectedMateriaIdActualizar);
      if (materia) {
        materia.estado = request.estado;
      }

      this.toastService.success('malla.UpdateCourse.success');
      this.closeActualizarModal();
    } catch (error) {
      this.toastService.error('malla.UpdateCourse.errorUpdate');
    } finally {
      this.savingEstado = false;
    }
  }

  protected getEstadoLabelKey(estado: 'APROBADA' | 'CURSANDO' | 'PENDIENTE'): string {
    const labelMap = {
      'APROBADA': 'malla.UpdateCourse.aprobada',
      'CURSANDO': 'malla.UpdateCourse.cursando',
      'PENDIENTE': 'malla.UpdateCourse.pendiente',
    };
    return labelMap[estado];
  }

  private mapEstadoUIToBD(estado: 'APROBADA' | 'CURSANDO' | 'PENDIENTE'): 'aprobada' | 'pendiente' | 'cursando' {
    const map = {
      'APROBADA': 'aprobada' as const,
      'CURSANDO': 'cursando' as const,
      'PENDIENTE': 'pendiente' as const,
    };
    return map[estado];
  }

  protected mapEstadoBDToUI(estado: string | null | undefined): 'APROBADA' | 'CURSANDO' | 'PENDIENTE' {
    if (!estado) return 'PENDIENTE';
    if (estado === 'aprobada') return 'APROBADA';
    if (estado === 'cursando') return 'CURSANDO';
    return 'PENDIENTE';
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

    this.selectedCarreraId = null;
    this.selectedMallaId = null;
    this.carreras = [];
    this.mallas = [];
    this.carreraRequiredError = false;
    this.mallaRequiredError = false;
    this.loadCarrerasError = false;
    this.loadMallasError = false;
    this.materiasLoadedForMallaId = null;
  }

  protected onCarreraChange(selectedCarreraId: number | null): void {
    this.selectedCarreraId = selectedCarreraId;
    this.carreraRequiredError = false;

    this.selectedMallaId = null;
    this.mallas = [];
    this.mallaRequiredError = false;
    this.loadMallasError = false;
    this.materiasLoadedForMallaId = null;
  }

  protected onMallaChange(selectedMallaId: number | null): void {
    if (this.selectedMallaId !== selectedMallaId) {
      this.materiasLoadedForMallaId = null;
    }
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
        const confirmed = window.confirm(this.translateService.instant('malla.modal.confirmChangeMalla'));
        if (!confirmed) {
          return;
        }
      }
    }

    void this.guardarSeleccion();
  }

  protected onMateriaClick(materia: MallaMateria): void {
    this.selectedMateriaParaAccion = materia;
    this.showAccionesModal = true;
  }

  protected hoveredMateriaId: number | null = null;
  protected prereqLines: { x1: number, y1: number, x2: number, y2: number }[] = [];

  protected onMateriaHover(materiaId: number | null): void {
    if (window.innerWidth < 768) return;
    this.hoveredMateriaId = materiaId;
    // Delay to ensure rendering is complete if needed, but synchronous is fine since DOM exists
    this.updatePrereqLines();
  }

  @HostListener('window:resize')
  protected updatePrereqLines(): void {
    if (!this.hoveredMateriaId) {
      this.prereqLines = [];
      return;
    }
    const lines: { x1: number, y1: number, x2: number, y2: number }[] = [];
    const target = this.materias.find(m => m.id === this.hoveredMateriaId);
    
    if (!target || !target.prerequisitosIds || target.prerequisitosIds.length === 0) {
      this.prereqLines = [];
      return;
    }

    const boardWrapper = document.querySelector('.malla-board-wrapper') as HTMLElement;
    const wrapperRect = boardWrapper?.getBoundingClientRect();
    if (!wrapperRect) return;

    const targetEl = document.getElementById(`subject-${target.id}`);
    if (targetEl) {
      const tRect = targetEl.getBoundingClientRect();
      const scrollLeft = boardWrapper.scrollLeft || 0;
      const x2 = tRect.left - wrapperRect.left + scrollLeft;
      const y2 = tRect.top - wrapperRect.top + (tRect.height / 2);

      for (const pid of target.prerequisitosIds) {
        const pEl = document.getElementById(`subject-${pid}`);
        if (pEl) {
          const pRect = pEl.getBoundingClientRect();
          const x1 = pRect.right - wrapperRect.left + scrollLeft;
          const y1 = pRect.top - wrapperRect.top + (pRect.height / 2);
          lines.push({ x1, y1, x2, y2 });
        }
      }
    }
    this.prereqLines = lines;
  }

  protected getMateriaCodigo(id: number): string {
    return this.materias.find(m => m.id === id)?.codigoMateria ?? '???';
  }

  protected enfocarMateria(id: number, event: Event): void {
    event.stopPropagation();
    const el = document.getElementById(`subject-${id}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('malla-subject--highlighted');
      setTimeout(() => el.classList.remove('malla-subject--highlighted'), 2000);
    }
  }

  protected closeAccionesModal(): void {
    this.showAccionesModal = false;
    this.selectedMateriaParaAccion = null;
  }

  protected onTomarMateriaClick(): void {
    const materia = this.selectedMateriaParaAccion;
    if (!materia) return;
    
    if (materia.estado === 'aprobada' || materia.estado === 'cursando') {
      return;
    }

    this.showAccionesModal = false;
    this.showModal = true;
    this.loadingDetalle = true;
    this.selectedOfertaId = null;

    this.mallaCatalogoService.getDetallesMateria(materia.id).subscribe({
      next: (detalle) => {
        this.materiaDetalle = detalle;
        this.loadingDetalle = false;
      },
      error: () => {
        alert(this.translateService.instant('malla.modal.detailLoadError'));
        this.closeModal();
      }
    });
  }

  protected confirmarSeleccionModal(): void {
    if (!this.materiaDetalle || !this.selectedOfertaId) return;

    this.tomaSeleccionService.agregarMateria({
      id: this.materiaDetalle.mallaMateriaId,
      nombre: this.materiaDetalle.nombreMateria,
      creditos: this.materiaDetalle.creditos,
      ofertaId: this.selectedOfertaId
    });

    this.closeModal();
    this.router.navigate(['/toma-de-materias']);

  }

  protected closeModal(): void {
    this.showModal = false;
    this.materiaDetalle = null;
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

  protected setSemestreActual(semestre: number): void {
    this.semestreActual = semestre;
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
    if (this.materiasLoadedForMallaId === mallaId && this.materias.length > 0) {
      return;
    }

    this.loadingMaterias = true;
    this.loadMateriasError = false;
    this.materias = [];
    this.materiasPorSemestre.clear();
    this.semestres = [];

    try {
      this.materias = await firstValueFrom(this.mallaCatalogoService.getMateriasPorMalla(mallaId));

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
      if (!this.loadMateriasError) {
        this.materiasLoadedForMallaId = mallaId;
        this.iniciarTour();
      }
    }
  }

  private getTourStorageKey(): string {
    const username = this.authSessionService.getCurrentUsername();
    return username ? `malla.tourCompleted.${username}` : 'malla.tourCompleted';
  }

  protected iniciarTour(): void {
    const storageKey = this.getTourStorageKey();

    // Migrar clave global antigua a clave por usuario (si existe)
    const legacyKey = 'malla.tourCompleted';
    if (localStorage.getItem(legacyKey) === 'true') {
      // La clave vieja se elimina para no bloquear a otros usuarios
      localStorage.removeItem(legacyKey);
    }

    // Verificar primero en localStorage (clave por usuario) para evitar flash
    if (localStorage.getItem(storageKey) === 'true') {
      return;
    }

    // Verificar en el backend (fuente de verdad, persiste entre dispositivos)
    const username = this.authSessionService.getCurrentUsername();
    if (username) {
      this.perfilService.getPerfilByUsername(username).subscribe({
        next: (perfil) => {
          if (perfil.tourCompleted) {
            localStorage.setItem(storageKey, 'true');
            return;
          }
          this.lanzarTourConRetraso();
        },
        error: () => {
          // Si falla la consulta, mostramos el tour de todas formas
          this.lanzarTourConRetraso();
        }
      });
    } else {
      this.lanzarTourConRetraso();
    }
  }

  private lanzarTourConRetraso(): void {
    // Pequeno retraso para asegurar que los elementos del DOM esten listos
    setTimeout(() => {
      this.siguienteTour(1);
    }, 800);
  }

  protected siguienteTour(step: number): void {
    this.popoverStep1?.close();
    this.popoverStep2?.close();
    this.popoverStep4?.close();
    this.tourHintsService.closeTomaMateriasPopover();

    setTimeout(() => {
      if (step === 1) this.popoverStep1?.open();
      if (step === 2) this.popoverStep2?.open();
      if (step === 3) this.tourHintsService.openTomaMateriasPopover();
      if (step === 4) this.popoverStep4?.open();
    }, 100);
  }

  protected finalizarTour(): void {
    this.cerrarTodosLosPopovers();
    localStorage.setItem(this.getTourStorageKey(), 'true');
    this.persistirTourCompletado();
  }

  public openImportModal(): void {
    this.showImportModal = true;
    this.importFile = null;
    this.importFileName = '';
    this.importError = null;
    this.importSuccess = null;
    this.importMallaName = '';
  }

  public closeImportModal(): void {
    this.showImportModal = false;
    this.importFile = null;
    this.importFileName = '';
    this.importError = null;
    this.importSuccess = null;
  }

  public onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      const validExtensions = ['.csv', '.json'];
      const ext = file.name.toLowerCase().substring(file.name.lastIndexOf('.'));
      if (!validExtensions.includes(ext)) {
        this.importError = this.translateService.instant('malla.import.errorFormat');
        this.importFile = null;
        this.importFileName = '';
        return;
      }
      this.importFile = file;
      this.importFileName = file.name;
      this.importError = null;
    }
  }

  public async importMalla(): Promise<void> {
    if (!this.importFile) {
      this.importError = this.translateService.instant('malla.import.errorNoFile');
      return;
    }
    if (!this.importMallaName.trim()) {
      this.importError = this.translateService.instant('malla.import.errorNameRequired');
      return;
    }
    if (this.selectedCarreraId === null) {
      this.importError = this.translateService.instant('malla.import.errorCarreraRequired');
      return;
    }

    this.importLoading = true;
    this.importError = null;
    this.importSuccess = null;

    try {
      const formData = new FormData();
      formData.append('file', this.importFile!);
      formData.append('carreraId', this.selectedCarreraId!.toString());
      formData.append('nombre', this.importMallaName);

      const result: any = await firstValueFrom(
        this.http.post('/api/academico/mallas/importar', formData, {
          withCredentials: true
        })
      );

      this.importSuccess = this.translateService.instant('malla.import.success', {
        count: result.materiasImportadas,
        prerequisitos: result.prerequisitosImportados
      });

      setTimeout(() => {
        this.closeImportModal();
        void this.loadMallas(this.selectedCarreraId!);
      }, 2000);

    } catch (error: any) {
      console.error('Error importando malla:', error);
      let errorMsg = this.translateService.instant('malla.import.errorGeneric');
      if (error.error) {
        if (typeof error.error === 'string') {
          errorMsg = error.error;
        } else if (error.error.message) {
          errorMsg = error.error.message;
        }
      } else if (error.message) {
        errorMsg = error.message;
      }
      this.importError = errorMsg;
    } finally {
      this.importLoading = false;
    }
  }

  public copyPrompt(): void {
    navigator.clipboard.writeText(this.fullPrompt).then(() => {
      alert(this.translateService.instant('malla.import.promptCopied'));
    }).catch(() => {
      prompt(this.translateService.instant('malla.import.copyPromptManually'), this.fullPrompt);
    });
  }

  public toggleTutorial(): void {
    this.showTutorial = !this.showTutorial;
  }
  protected irATomaYCerrarTour(): void {
    this.cerrarTodosLosPopovers();
    localStorage.setItem(this.getTourStorageKey(), 'true');
    this.persistirTourCompletado();
    void this.router.navigate(['/toma-de-materias']);
  }

  protected noVolverAMostrarTour(): void {
    this.cerrarTodosLosPopovers();
    localStorage.setItem(this.getTourStorageKey(), 'true');
    this.persistirTourCompletado();
  }

  private cerrarTodosLosPopovers(): void {
    this.popoverStep1?.close();
    this.popoverStep2?.close();
    this.popoverStep4?.close();
    this.tourHintsService.closeTomaMateriasPopover();
  }

  private persistirTourCompletado(): void {
    const username = this.authSessionService.getCurrentUsername();
    if (!username) return;
    this.perfilService.completeTour(username).subscribe({
      next: () => {},
      error: () => {}
    });
  }

}
