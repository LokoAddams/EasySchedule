import { NgFor, NgIf, NgClass } from '@angular/common';
import { Component, OnDestroy, OnInit, HostListener, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { filter, firstValueFrom, Subscription } from 'rxjs';
import { NgbPopover, NgbPopoverModule } from '@ng-bootstrap/ng-bootstrap';

import { environment } from '../../../environments/environment';
import { CarreraCatalogoItem, CarreraService } from '../../services/academico/carrera.service';
import { EstadoMateriaService, EstadoMateriaRequest } from '../../services/academico/estado-materia.service';
import { FeatureToggleService } from '../../services/feature-toggle.service';
import { MallaCatalogoItem, MallaCatalogoService, MallaEditableMateria, MallaMateria } from '../../services/academico/malla-catalogo.service';
import {
  SeleccionAcademica,
  SeleccionAcademicaService,
} from '../../services/academico/seleccion-academica.service';
import { UniversidadCatalogoItem, UniversidadService } from '../../services/academico/universidad.service';
import { TomaSeleccionService } from '../../services/academico/toma-seleccion.service';
import { OfertaDetalleResponse } from '../../services/academico/malla-catalogo.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthSessionService } from '../../core/services/auth-session.service';
import { PerfilService } from '../perfil/perfil.service';
import { TourHintsService } from '../../services/tour-hints.service';
import { SeleccionTemporalService, SeleccionTemporalResponse } from '../../services/academico/seleccion-temporal.service';

import { ImportarOfertasModal } from './importar-ofertas-modal/importar-ofertas-modal';
import { GestionarOfertas } from './gestionar-ofertas/gestionar-ofertas';
import { ConfirmModal } from '../../shared/ui/confirm-modal/confirm-modal';

type SeleccionStep = 'universidad' | 'carrera' | 'malla' | 'resumen';
type EditMode = 'universidad' | 'malla' | null;

interface SeleccionSnapshot {
  universidadId: number | null;
  carreraId: number | null;
  mallaId: number | null;
}

interface MallaEditRow {
  rowId: number;
  codigo: string;
  nombre: string;
  semestre: number;
  creditos: number;
  prerequisitos: string;
}

@Component({
  selector: 'app-malla',
  imports: [
    FormsModule,
    NgFor,
    NgIf,
    NgClass,
    TranslatePipe,
    NgbPopoverModule,
    ImportarOfertasModal,
    GestionarOfertas,
    ConfirmModal,
  ],
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
  protected semestreActual = 1;
  protected loadingMaterias = false;
  protected loadMateriasError = false;
  protected exportingAvance = false;

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

  protected ofertasImportEnabled = true;
  protected showImportarOfertasModal = false;
  protected showGestionarOfertasModal = false;

  protected showActualizarModal = false;
  protected selectedMateriaIdActualizar: number | null = null;
  protected selectedEstadoActualizar: 'APROBADA' | 'CURSANDO' | 'PENDIENTE' = 'PENDIENTE';
  protected savingEstado = false;

  protected showEditarMallaModal = false;
  protected editMallaRows: MallaEditRow[] = [];
  protected editMallaNombre = '';
  protected editMallaVersion = '';
  protected editMallaLoading = false;
  protected editMallaSaving = false;
  protected editMallaError: string | null = null;
  protected editMallaCsvFileName = '';
  private nextEditRowId = 1;

  protected hoveredMateriaId: number | null = null;
  protected prereqLines: { x1: number; y1: number; x2: number; y2: number }[] = [];

  protected showPrerequisitoModal = false;
  protected prereqDisponibles: MallaMateria[] = [];
  protected prereqDisponiblesPorSemestre: Map<number, MallaMateria[]> = new Map();
  protected prereqSemestres: number[] = [];
  protected prereqSeleccionadosTemp: Set<number> = new Set();
  protected prereqQuitarTemp: Set<number> = new Set();
  protected prereqGuardando = false;
  protected prereqError: string | null = null;
  protected prereqSuccess: string | null = null;

  protected showConfirmModal = false;
  protected pendingConfirmAction: 'changeMalla' | 'changeUniversidad' | 'clearSelection' | 'importMalla' | null = null;
  protected confirmModalMessage = 'confirmModal.defaultMessage';
  protected confirmModalTitle = 'confirmModal.defaultTitle';

  protected tourStep = 0;

  protected showCrearUniversidad = false;
  protected nuevaUniversidadNombre = '';
  protected nuevaUniversidadCodigo = '';
  protected crearUniversidadError: string | null = null;
  protected creandoUniversidad = false;

  protected showCrearCarrera = false;
  protected nuevaCarreraNombre = '';
  protected nuevaCarreraCodigo = '';
  protected crearCarreraError: string | null = null;
  protected creandoCarrera = false;

  public showImportModal = false;
  public importFile: File | null = null;
  public importFileName = '';
  public importLoading = false;
  public importError: string | null = null;
  public importSuccess: string | null = null;
  public importMallaName = '';
  public importMallaVersion = '';
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


  protected seleccionesTemporales: SeleccionTemporalResponse[] = [];
  protected showFloatingPanel = false;
  protected panelExpanded = true;
  protected loadingSelecciones = false;

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
    private readonly http: HttpClient,
    private readonly toastService: ToastService,
    private readonly authSessionService: AuthSessionService,
    private readonly perfilService: PerfilService,
    private readonly tourHintsService: TourHintsService,
    private readonly seleccionTemporalService: SeleccionTemporalService,
  ) {}

  ngOnInit(): void {
    this.flagsSubscription = this.featureService.flags$.subscribe((flags) => {
      this.mallaEnabled = flags.malla;
      this.ofertasImportEnabled = flags.ofertasImport ?? true;
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
      this.materiasSeleccionadas = new Set(materias.map((materia) => materia.id));
    });

    void this.featureService.loadFlags();
    void this.loadUniversidades();
    void this.cargarSeleccionesTemporales();
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
    this.pendingConfirmAction = 'changeUniversidad';
    this.confirmModalTitle = 'confirmModal.defaultTitle';
    this.confirmModalMessage = 'confirmModal.changeUniversidad';
    this.showConfirmModal = true;
  }

  protected onCambiarMallaClick(): void {
    if (this.selectedUniversidadId === null) {
      return;
    }

    void this.prepareMallaEditMode();
  }

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

  protected async onEditarMallaClick(): Promise<void> {
    if (this.selectedMallaId === null) {
      return;
    }

    this.showEditarMallaModal = true;
    this.editMallaLoading = true;
    this.editMallaSaving = false;
    this.editMallaError = null;
    this.editMallaCsvFileName = '';
    this.editMallaRows = [];

    try {
      const response = await firstValueFrom(this.mallaCatalogoService.getMallaEditable(this.selectedMallaId));
      this.editMallaNombre = response.nombre;
      this.editMallaVersion = response.version;
      this.nextEditRowId = 1;
      this.editMallaRows = response.materias.map((materia) => this.toEditRow(materia));
    } catch (error: any) {
      this.editMallaError = this.resolveMallaEditError(error, 'malla.edit.errorLoad');
    } finally {
      this.editMallaLoading = false;
    }
  }

  protected closeEditarMallaModal(): void {
    if (this.editMallaSaving) {
      return;
    }

    this.showEditarMallaModal = false;
    this.editMallaRows = [];
    this.editMallaError = null;
    this.editMallaCsvFileName = '';
  }

  protected addEditMallaRow(): void {
    this.editMallaRows = [
      ...this.editMallaRows,
      {
        rowId: this.nextEditRowId++,
        codigo: '',
        nombre: '',
        semestre: 1,
        creditos: 4,
        prerequisitos: '',
      },
    ];
  }

  protected removeEditMallaRow(row: MallaEditRow): void {
    if (this.editMallaRows.length <= 1) {
      this.editMallaError = this.translateService.instant('malla.edit.errorAtLeastOne');
      return;
    }

    this.editMallaRows = this.editMallaRows.filter((item) => item.rowId !== row.rowId);
  }

  protected async onEditMallaCsvSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.editMallaError = this.translateService.instant('malla.edit.errorCsvOnly');
      input.value = '';
      return;
    }

    try {
      const materias = await this.parseMallaCsv(file);
      this.nextEditRowId = 1;
      this.editMallaRows = materias.map((materia) => this.toEditRow(materia));
      this.editMallaCsvFileName = file.name;
      this.editMallaError = null;
    } catch (error: any) {
      this.editMallaError = error?.message || this.translateService.instant('malla.edit.errorCsvRead');
    } finally {
      input.value = '';
    }
  }

  protected async saveEditarMalla(): Promise<void> {
    if (this.selectedMallaId === null) {
      return;
    }

    const validationError = this.validateEditMallaRows();
    if (validationError) {
      this.editMallaError = validationError;
      return;
    }

    this.editMallaSaving = true;
    this.editMallaError = null;

    try {
      await firstValueFrom(this.mallaCatalogoService.actualizarMallaEditable(this.selectedMallaId, {
        materias: this.editMallaRows.map((row) => this.toEditableMateria(row)),
      }));

      this.materiasLoadedForMallaId = null;
      await this.loadMaterias(this.selectedMallaId, true);
      this.toastService.success('malla.edit.success');
      this.editMallaSaving = false;
      this.closeEditarMallaModal();
    } catch (error: any) {
      this.editMallaError = this.resolveMallaEditError(error, 'malla.edit.errorSave');
    } finally {
      this.editMallaSaving = false;
    }
  }

  protected closeActualizarModal(): void {
    this.showActualizarModal = false;
  }

  protected onMateriaActualizarChange(): void {
    const materia = this.materias.find((item) => item.id === this.selectedMateriaIdActualizar);

    if (materia) {
      this.selectedEstadoActualizar = this.mapEstadoBDToUI(materia.estado);
    }
  }

  protected async actualizarMateriaSeleccionada(): Promise<void> {
    if (this.selectedMateriaIdActualizar === null) {
      return;
    }

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

      const materia = this.materias.find((item) => item.id === this.selectedMateriaIdActualizar);

      if (materia) {
        materia.estado = request.estado;
      }

      this.toastService.success('malla.UpdateCourse.success');
      this.closeActualizarModal();
    } catch {
      this.toastService.error('malla.UpdateCourse.errorUpdate');
    } finally {
      this.savingEstado = false;
    }
  }

  protected getEstadoLabelKey(estado: 'APROBADA' | 'CURSANDO' | 'PENDIENTE'): string {
    const labelMap = {
      APROBADA: 'malla.UpdateCourse.aprobada',
      CURSANDO: 'malla.UpdateCourse.cursando',
      PENDIENTE: 'malla.UpdateCourse.pendiente',
    };

    return labelMap[estado];
  }

  protected mapEstadoBDToUI(estado: string | null | undefined): 'APROBADA' | 'CURSANDO' | 'PENDIENTE' {
    if (!estado) {
      return 'PENDIENTE';
    }

    if (estado === 'aprobada') {
      return 'APROBADA';
    }

    if (estado === 'cursando') {
      return 'CURSANDO';
    }

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
    if (
      this.selectedUniversidadId === null ||
      this.selectedCarreraId === null ||
      this.selectedMallaId === null
    ) {
      this.mallaRequiredError = this.selectedMallaId === null;
      return;
    }

    if (this.editMode === 'malla' && this.previousSelectionSnapshot !== null) {
      const mallaAnteriorId = this.previousSelectionSnapshot.mallaId;
      const isChangingMalla = mallaAnteriorId !== null && this.selectedMallaId !== mallaAnteriorId;

      if (isChangingMalla) {
        this.pendingConfirmAction = 'changeMalla';
        this.confirmModalTitle = 'confirmModal.defaultTitle';
        this.confirmModalMessage = 'confirmModal.changeMalla';
        this.showConfirmModal = true;
        return;
      }
    }

    void this.guardarSeleccion();
  }

  protected onMateriaClick(materia: MallaMateria): void {
    this.selectedMateriaParaAccion = materia;
    this.showAccionesModal = true;
  }

  protected onMateriaHover(materiaId: number | null): void {
    if (window.innerWidth < 768) {
      return;
    }

    this.hoveredMateriaId = materiaId;
    this.updatePrereqLines();
  }

  @HostListener('window:resize')
  protected updatePrereqLines(): void {
    if (!this.hoveredMateriaId) {
      this.prereqLines = [];
      return;
    }

    const lines: { x1: number; y1: number; x2: number; y2: number }[] = [];
    const target = this.materias.find((materia) => materia.id === this.hoveredMateriaId);

    if (!target || !target.prerequisitosIds || target.prerequisitosIds.length === 0) {
      this.prereqLines = [];
      return;
    }

    const boardWrapper = document.querySelector('.malla-board-wrapper') as HTMLElement;
    const wrapperRect = boardWrapper?.getBoundingClientRect();

    if (!wrapperRect) {
      return;
    }

    const targetEl = document.getElementById(`subject-${target.id}`);

    if (targetEl) {
      const targetRect = targetEl.getBoundingClientRect();
      const scrollLeft = boardWrapper.scrollLeft || 0;
      const x2 = targetRect.left - wrapperRect.left + scrollLeft;
      const y2 = targetRect.top - wrapperRect.top + targetRect.height / 2;

      for (const prerequisiteId of target.prerequisitosIds) {
        const prerequisiteEl = document.getElementById(`subject-${prerequisiteId}`);

        if (prerequisiteEl) {
          const prerequisiteRect = prerequisiteEl.getBoundingClientRect();
          const x1 = prerequisiteRect.right - wrapperRect.left + scrollLeft;
          const y1 = prerequisiteRect.top - wrapperRect.top + prerequisiteRect.height / 2;
          lines.push({ x1, y1, x2, y2 });
        }
      }
    }

    this.prereqLines = lines;
  }

  protected getMateriaCodigo(id: number): string {
    return this.materias.find((materia) => materia.id === id)?.codigoMateria ?? '???';
  }

  protected enfocarMateria(id: number, event: Event): void {
    event.stopPropagation();

    const element = document.getElementById(`subject-${id}`);

    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center' });
      element.classList.add('malla-subject--highlighted');
      setTimeout(() => element.classList.remove('malla-subject--highlighted'), 2000);
    }
  }

  protected closeAccionesModal(): void {
    this.showAccionesModal = false;
    this.selectedMateriaParaAccion = null;
  }

  protected abrirModalPrerequisitos(): void {
    const materia = this.selectedMateriaParaAccion;

    if (!materia) {
      return;
    }

    this.showAccionesModal = false;
    this.prereqError = null;
    this.prereqSuccess = null;
    this.prereqGuardando = false;
    this.prereqSeleccionadosTemp = new Set();
    this.prereqQuitarTemp = new Set();

    const semestreMateria = materia.semestreSugerido;

    this.prereqDisponibles = this.materias.filter((m) => {
      if (m.id === materia.id) {
        return false;
      }

      if (m.semestreSugerido >= semestreMateria) {
        return false;
      }

      return true;
    });

    this.prereqDisponiblesPorSemestre = new Map();
    this.prereqSemestres = [];

    this.prereqDisponibles.forEach((m) => {
      const sem = m.semestreSugerido;

      if (!this.prereqDisponiblesPorSemestre.has(sem)) {
        this.prereqDisponiblesPorSemestre.set(sem, []);
        this.prereqSemestres.push(sem);
      }

      this.prereqDisponiblesPorSemestre.get(sem)!.push(m);
    });
    this.prereqSemestres.sort((a, b) => a - b);

    this.showPrerequisitoModal = true;
  }

  protected cerrarModalPrerequisitos(): void {
    this.showPrerequisitoModal = false;
    this.prereqDisponibles = [];
    this.prereqDisponiblesPorSemestre = new Map();
    this.prereqSemestres = [];
    this.prereqSeleccionadosTemp = new Set();
    this.prereqQuitarTemp = new Set();
    this.prereqError = null;
    this.prereqSuccess = null;
  }

  protected esPrerequisitoExistente(materiaId: number): boolean {
    const materia = this.selectedMateriaParaAccion;

    if (!materia) {
      return false;
    }

    return materia.prerequisitosIds.includes(materiaId);
  }

  protected togglePrerequisitoTemp(materiaId: number): void {
    if (this.prereqSeleccionadosTemp.has(materiaId)) {
      this.prereqSeleccionadosTemp.delete(materiaId);
    } else {
      this.prereqSeleccionadosTemp.add(materiaId);
    }
  }

  protected toggleQuitarPrerequisito(materiaId: number): void {
    if (this.prereqQuitarTemp.has(materiaId)) {
      this.prereqQuitarTemp.delete(materiaId);
    } else {
      this.prereqQuitarTemp.add(materiaId);
    }
  }

  protected async guardarPrerequisitos(): Promise<void> {
    const materia = this.selectedMateriaParaAccion;

    if (!materia) {
      this.cerrarModalPrerequisitos();
      return;
    }

    if (this.prereqSeleccionadosTemp.size === 0 && this.prereqQuitarTemp.size === 0) {
      this.cerrarModalPrerequisitos();
      return;
    }

    this.prereqGuardando = true;
    this.prereqError = null;
    this.prereqSuccess = null;

    let addedCount = 0;
    let removedCount = 0;
    let errorCount = 0;

    for (const prereqId of this.prereqSeleccionadosTemp) {
      try {
        await firstValueFrom(
          this.mallaCatalogoService.crearPrerequisito(materia.id, prereqId)
        );
        materia.prerequisitosIds.push(prereqId);
        addedCount++;
      } catch (err: any) {
        errorCount++;
        console.error('[PREREQ] Error al crear prerrequisito:', err);
        const errorMsg = err?.error?.error || err?.message || this.translateService.instant('malla.prerequisito.errorGeneric');

        if (!this.prereqError) {
          this.prereqError = errorMsg + (err?.error?.message ? ' - ' + err?.error?.message : '');
        }
      }
    }

    for (const prereqId of this.prereqQuitarTemp) {
      try {
        await firstValueFrom(
          this.mallaCatalogoService.eliminarPrerequisito(materia.id, prereqId)
        );
        const idx = materia.prerequisitosIds.indexOf(prereqId);
        if (idx >= 0) {
          materia.prerequisitosIds.splice(idx, 1);
        }
        removedCount++;
      } catch (err: any) {
        errorCount++;
        console.error('[PREREQ] Error al quitar prerrequisito:', err);
        const errorMsg = err?.error?.error || err?.message || this.translateService.instant('malla.prerequisito.errorGeneric');

        if (!this.prereqError) {
          this.prereqError = errorMsg + (err?.error?.message ? ' - ' + err?.error?.message : '');
        }
      }
    }

    this.prereqGuardando = false;

    const totalOk = addedCount + removedCount;

    if (errorCount === 0 && totalOk > 0) {
      if (addedCount > 0 && removedCount === 0) {
        this.prereqSuccess = addedCount === 1
          ? this.translateService.instant('malla.prerequisito.successAdd')
          : this.translateService.instant('malla.prerequisito.successAddMultiple', { count: addedCount });
      } else {
        this.prereqSuccess = this.translateService.instant('malla.prerequisito.successPartial', { added: addedCount, removed: removedCount });
      }
    } else if (totalOk > 0) {
      this.prereqSuccess = this.translateService.instant('malla.prerequisito.successPartial', { added: addedCount, removed: removedCount });
    }

    if (totalOk > 0) {
      this.updatePrereqLines();
    }
  }

  protected onTomarMateriaClick(): void {
    const materia = this.selectedMateriaParaAccion;

    if (!materia) {
      return;
    }

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
        this.toastService.error('malla.modal.detailLoadError');
        this.closeModal();
      },
    });
  }

  protected confirmarSeleccionModal(): void {
    if (!this.materiaDetalle || !this.selectedOfertaId || this.loadingSelecciones) {
      return;
    }

    this.loadingSelecciones = true;
    this.seleccionTemporalService.agregarSeleccion({ ofertaMateriaId: this.selectedOfertaId }).subscribe({
      next: () => {
        void this.cargarSeleccionesTemporales();
        this.closeModal();
        this.showFloatingPanel = true;
        this.panelExpanded = true;
        this.loadingSelecciones = false;
        this.toastService.success('malla.selection.added');
      },
      error: () => {
        this.toastService.error('malla.selection.errorAdd');
        this.loadingSelecciones = false;
      }
    });
  }

  protected async cargarSeleccionesTemporales(): Promise<void> {
    this.loadingSelecciones = true;
    try {
      const selecciones = await firstValueFrom(this.seleccionTemporalService.listarSelecciones());
      this.seleccionesTemporales = selecciones;
      this.showFloatingPanel = selecciones.length > 0;
    } catch (error) {
      console.error('Error loading temporal selections', error);
      this.toastService.error('malla.selection.errorLoad');
    } finally {
      this.loadingSelecciones = false;
    }
  }

  protected togglePanel(): void {
    this.panelExpanded = !this.panelExpanded;
  }

  protected irATomaDeMaterias(): void {
    void this.router.navigate(['/toma-de-materias']);
  }

  protected cancelarSeleccion(): void {
    this.pendingConfirmAction = 'clearSelection';
    this.confirmModalTitle = 'confirmModal.defaultTitle';
    this.confirmModalMessage = 'confirmModal.clearSelection';
    this.showConfirmModal = true;
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
    const element = document.querySelector(`.malla-board__column[data-semester="${semestre}"]`);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'start' });
    }
  }

  protected onImportarOfertasClick(): void {
    if (this.selectedMallaId === null || this.materias.length === 0) {
      return;
    }

    this.showImportarOfertasModal = true;
  }

  protected async onExportarAvanceClick(): Promise<void> {
    if (this.exportingAvance) {
      return;
    }

    if (!this.authSessionService.isLoggedIn()) {
      this.toastService.error('malla.export.notAuthenticated');
      return;
    }

    this.exportingAvance = true;

    try {
      const response = await firstValueFrom(this.mallaCatalogoService.exportarAvanceGraduacion());
      const blob = response.body;

      if (!blob) {
        throw new Error('empty-response');
      }

      this.downloadBlob(blob, this.getExportFilename(response));
      this.toastService.success('malla.export.success');
    } catch (error) {
      this.toastService.error(this.getExportErrorKey(error));
    } finally {
      this.exportingAvance = false;
    }
  }

  protected closeImportarOfertasModal(): void {
    this.showImportarOfertasModal = false;
  }

  protected onGestionarOfertasClick(): void {
    this.showGestionarOfertasModal = true;
  }

  protected closeGestionarOfertasModal(): void {
    this.showGestionarOfertasModal = false;
  }

  protected onOfertasImportFinished(): void {
    this.toastService.success('malla.offers.importSuccessToast');

    if (this.selectedMallaId !== null) {
      this.materiasLoadedForMallaId = null;
      void this.loadMaterias(this.selectedMallaId);
    }
  }

  private mapEstadoUIToBD(estado: 'APROBADA' | 'CURSANDO' | 'PENDIENTE'): 'aprobada' | 'pendiente' | 'cursando' {
    const map = {
      APROBADA: 'aprobada' as const,
      CURSANDO: 'cursando' as const,
      PENDIENTE: 'pendiente' as const,
    };

    return map[estado];
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

  private getExportFilename(response: HttpResponse<Blob>): string {
    const contentDisposition = response.headers.get('content-disposition') ?? '';
    const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
    const asciiMatch = /filename="?([^";]+)"?/i.exec(contentDisposition);
    const encodedFilename = utf8Match?.[1] ?? asciiMatch?.[1];

    if (!encodedFilename) {
      return 'avance_graduacion.pdf';
    }

    try {
      return decodeURIComponent(encodedFilename);
    } catch {
      return encodedFilename;
    }
  }

  private getExportErrorKey(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401 || error.status === 403) {
        return 'malla.export.notAuthenticated';
      }

      if (error.status === 422 || error.status === 404 || error.status === 400) {
        return 'malla.export.insufficientData';
      }

      if (error.status === 0) {
        return 'malla.export.connectionError';
      }
    }

    return 'malla.export.downloadError';
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
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
      this.toastService.error('malla.error.loadSeleccionActual');
    }
  }

  private async prepareMallaEditMode(): Promise<void> {
    if (this.selectedUniversidadId === null || this.selectedCarreraId === null || this.selectedMallaId === null) {
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
      const carreras = await firstValueFrom(
        this.carreraService.getCarrerasActivasPorUniversidad(this.selectedUniversidadId),
      );

      this.carreras = carreras;

      const mallasPorCarrera = await Promise.all(
        carreras.map((carrera) => firstValueFrom(this.mallaCatalogoService.getMallasActivasPorCarrera(carrera.id))),
      );

      const mallasPlanas = mallasPorCarrera.flat();

      this.mallas = mallasPlanas.filter(
        (malla, index, source) => source.findIndex((candidate) => candidate.id === malla.id) === index,
      );

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

  private async loadMaterias(mallaId: number, force = false): Promise<void> {
    if (!force && this.materiasLoadedForMallaId === mallaId && this.materias.length > 0) {
      return;
    }

    this.loadingMaterias = true;
    this.loadMateriasError = false;
    this.materias = [];
    this.materiasPorSemestre.clear();
    this.semestres = [];

    try {
      this.setMaterias(await firstValueFrom(this.mallaCatalogoService.getMateriasPorMalla(mallaId)));
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

  private setMaterias(materias: MallaMateria[]): void {
    this.materias = materias;
    this.materiasPorSemestre.clear();
    this.semestres = [];

    this.materias.forEach((materia) => {
      const semestre = materia.semestreSugerido;

      if (!this.materiasPorSemestre.has(semestre)) {
        this.materiasPorSemestre.set(semestre, []);
        this.semestres.push(semestre);
      }

      this.materiasPorSemestre.get(semestre)!.push(materia);
    });

    this.semestres.sort((a, b) => a - b);
  }

  private toEditRow(materia: MallaEditableMateria): MallaEditRow {
    return {
      rowId: this.nextEditRowId++,
      codigo: materia.codigo,
      nombre: materia.nombre,
      semestre: materia.semestre,
      creditos: materia.creditos,
      prerequisitos: (materia.prerequisitos ?? []).join(';'),
    };
  }

  private toEditableMateria(row: MallaEditRow): MallaEditableMateria {
    return {
      codigo: this.normalizeMateriaCode(row.codigo),
      nombre: row.nombre.trim(),
      semestre: Number(row.semestre),
      creditos: Number(row.creditos),
      prerequisitos: this.parsePrerequisitos(row.prerequisitos),
    };
  }

  private validateEditMallaRows(): string | null {
    if (this.editMallaRows.length === 0) {
      return this.translateService.instant('malla.edit.errorAtLeastOne');
    }

    const codes = new Map<string, MallaEditRow>();
    for (const row of this.editMallaRows) {
      const code = this.normalizeMateriaCode(row.codigo);
      if (!code || !row.nombre.trim()) {
        return this.translateService.instant('malla.edit.errorRequiredFields');
      }

      if (!Number.isFinite(Number(row.semestre)) || Number(row.semestre) < 1 || Number(row.semestre) > 50) {
        return this.translateService.instant('malla.edit.errorSemester');
      }

      if (!Number.isFinite(Number(row.creditos)) || Number(row.creditos) < 1 || Number(row.creditos) > 99) {
        return this.translateService.instant('malla.edit.errorCredits');
      }

      if (codes.has(code)) {
        return this.translateService.instant('malla.edit.errorDuplicateCode', { code });
      }

      codes.set(code, row);
    }

    for (const row of this.editMallaRows) {
      const code = this.normalizeMateriaCode(row.codigo);
      for (const prereqCode of this.parsePrerequisitos(row.prerequisitos)) {
        const prereq = codes.get(prereqCode);
        if (!prereq) {
          return this.translateService.instant('malla.edit.errorUnknownPrereq', { code: prereqCode });
        }

        if (prereqCode === code) {
          return this.translateService.instant('malla.edit.errorSelfPrereq');
        }

        if (Number(prereq.semestre) >= Number(row.semestre)) {
          return this.translateService.instant('malla.edit.errorPrereqSemester');
        }
      }
    }

    return null;
  }

  private parsePrerequisitos(value: string): string[] {
    return value
      .split(';')
      .map((item) => this.normalizeMateriaCode(item))
      .filter((item, index, values) => item.length > 0 && values.indexOf(item) === index);
  }

  private normalizeMateriaCode(value: string): string {
    return (value ?? '').trim();
  }

  private async parseMallaCsv(file: File): Promise<MallaEditableMateria[]> {
    const content = await file.text();
    const lines = content.split(/\r?\n/).filter((line) => line.trim().length > 0);

    if (lines.length <= 1) {
      throw new Error(this.translateService.instant('malla.edit.errorCsvEmpty'));
    }

    const materias: MallaEditableMateria[] = [];
    for (let index = 1; index < lines.length; index++) {
      const line = index === 1 ? lines[index].replace(/^\uFEFF/, '') : lines[index];
      const parts = line.split(',', -1);

      if (parts.length < 4) {
        throw new Error(this.translateService.instant('malla.edit.errorCsvColumns', { line: index + 1 }));
      }

      const semestre = Number(parts[2].trim());
      const creditos = parts[3].trim() ? Number(parts[3].trim()) : 0;
      if (!Number.isFinite(semestre) || !Number.isFinite(creditos)) {
        throw new Error(this.translateService.instant('malla.edit.errorCsvNumbers', { line: index + 1 }));
      }

      materias.push({
        codigo: parts[0].trim(),
        nombre: parts[1].trim(),
        semestre,
        creditos,
        prerequisitos: parts.length > 4 ? this.parsePrerequisitos(parts[4]) : [],
      });
    }

    return materias;
  }

  private resolveMallaEditError(error: any, fallbackKey: string): string {
    const backendMessage = error?.error?.message;
    if (typeof backendMessage === 'string' && backendMessage.trim()) {
      return backendMessage;
    }

    if (typeof error?.message === 'string' && error.message.trim()) {
      return error.message;
    }

    return this.translateService.instant(fallbackKey);
  }

  private getTourStorageKey(): string {
    const username = this.authSessionService.getCurrentUsername();
    return username ? `malla.tourCompleted.${username}` : 'malla.tourCompleted';
  }

  protected iniciarTour(): void {
    const storageKey = this.getTourStorageKey();
    const legacyKey = 'malla.tourCompleted';

    if (localStorage.getItem(legacyKey) === 'true') {
      localStorage.removeItem(legacyKey);
    }

    if (localStorage.getItem(storageKey) === 'true') {
      return;
    }

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
          this.lanzarTourConRetraso();
        },
      });
    } else {
      this.lanzarTourConRetraso();
    }
  }

  private lanzarTourConRetraso(): void {
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
      if (step === 1) {
        this.popoverStep1?.open();
      }

      if (step === 2) {
        this.popoverStep2?.open();
      }

      if (step === 3) {
        this.tourHintsService.openTomaMateriasPopover();
      }

      if (step === 4) {
        this.popoverStep4?.open();
      }
    }, 100);
  }

  protected finalizarTour(): void {
    this.cerrarTodosLosPopovers();
    localStorage.setItem(this.getTourStorageKey(), 'true');
    this.persistirTourCompletado();
  }

  protected toggleCrearUniversidad(): void {
    this.showCrearUniversidad = !this.showCrearUniversidad;
    if (!this.showCrearUniversidad) {
      this.cancelarCrearUniversidad();
    }
  }

  protected cancelarCrearUniversidad(): void {
    this.showCrearUniversidad = false;
    this.nuevaUniversidadNombre = '';
    this.nuevaUniversidadCodigo = '';
    this.crearUniversidadError = null;
  }

  protected async guardarNuevaUniversidad(): Promise<void> {
    this.crearUniversidadError = null;
    if (!this.nuevaUniversidadNombre.trim()) {
      this.crearUniversidadError = 'El nombre de la universidad es requerido';
      return;
    }
    if (!this.nuevaUniversidadCodigo.trim()) {
      this.crearUniversidadError = 'El codigo de la universidad es requerido';
      return;
    }

    this.creandoUniversidad = true;
    try {
      const nueva = await firstValueFrom(
        this.universidadService.createUniversidad({
          nombre: this.nuevaUniversidadNombre.trim(),
          codigo: this.nuevaUniversidadCodigo.trim(),
        })
      );
      await this.loadUniversidades();
      this.selectedUniversidadId = nueva.id;
      this.cancelarCrearUniversidad();
      this.toastService.success('Universidad creada exitosamente');
    } catch (error: any) {
      this.crearUniversidadError = error.error?.message || error.message || 'Error al crear la universidad';
    } finally {
      this.creandoUniversidad = false;
    }
  }

  protected toggleCrearCarrera(): void {
    this.showCrearCarrera = !this.showCrearCarrera;
    if (!this.showCrearCarrera) {
      this.cancelarCrearCarrera();
    }
  }

  protected cancelarCrearCarrera(): void {
    this.showCrearCarrera = false;
    this.nuevaCarreraNombre = '';
    this.nuevaCarreraCodigo = '';
    this.crearCarreraError = null;
  }

  protected async guardarNuevaCarrera(): Promise<void> {
    this.crearCarreraError = null;
    if (!this.nuevaCarreraNombre.trim()) {
      this.crearCarreraError = 'El nombre de la carrera es requerido';
      return;
    }
    if (!this.nuevaCarreraCodigo.trim()) {
      this.crearCarreraError = 'El codigo de la carrera es requerido';
      return;
    }
    if (this.selectedUniversidadId === null) {
      this.crearCarreraError = 'Debe seleccionar una universidad primero';
      return;
    }

    this.creandoCarrera = true;
    try {
      const nueva = await firstValueFrom(
        this.carreraService.createCarrera({
          universidadId: this.selectedUniversidadId,
          nombre: this.nuevaCarreraNombre.trim(),
          codigo: this.nuevaCarreraCodigo.trim(),
        })
      );
      await this.loadCarreras(this.selectedUniversidadId, false);
      this.selectedCarreraId = nueva.id;
      this.cancelarCrearCarrera();
      this.toastService.success('Carrera creada exitosamente');
    } catch (error: any) {
      this.crearCarreraError = error.error?.message || error.message || 'Error al crear la carrera';
    } finally {
      this.creandoCarrera = false;
    }
  }

  public openImportModal(): void {
    this.showImportModal = true;
    this.importFile = null;
    this.importFileName = '';
    this.importError = null;
    this.importSuccess = null;
    this.importMallaName = '';
    this.importMallaVersion = String(new Date().getFullYear());
  }

  public closeImportModal(): void {
    this.showImportModal = false;
    this.importFile = null;
    this.importFileName = '';
    this.importError = null;
    this.importSuccess = null;
    this.importMallaVersion = '';
  }

  public isValidImportVersion(): boolean {
    return /^\d{4}$/.test(this.importMallaVersion.trim());
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

  protected canImportMalla(): boolean {
    return !!this.importFile && !this.importLoading && !!this.importMallaName.trim() && this.isValidImportVersion();
  }

  public onImportMallaClick(): void {
    if (!this.canImportMalla()) {
      return;
    }
    this.pendingConfirmAction = 'importMalla';
    this.confirmModalTitle = 'confirmModal.defaultTitle';
    this.confirmModalMessage = 'confirmModal.importMalla';
    this.showConfirmModal = true;
  }

  protected onConfirmAction(): void {
    const action = this.pendingConfirmAction;
    this.showConfirmModal = false;
    this.pendingConfirmAction = null;

    switch (action) {
      case 'changeMalla':
        void this.ejecutarGuardarSeleccion();
        break;
      case 'changeUniversidad':
        this.ejecutarCambioUniversidad();
        break;
      case 'clearSelection':
        this.ejecutarLimpiarSeleccion();
        break;
      case 'importMalla':
        void this.ejecutarImportMalla();
        break;
    }
  }

  protected onCancelConfirm(): void {
    this.showConfirmModal = false;
    this.pendingConfirmAction = null;
  }

  private ejecutarCambioUniversidad(): void {
    this.editMode = 'universidad';
    this.step = 'universidad';
    this.mallaChangeWarningVisible = false;
    this.clearErrors();
    this.createSelectionSnapshot();
    this.onUniversidadChange(null);
  }

  private ejecutarLimpiarSeleccion(): void {
    this.seleccionTemporalService.limpiarSelecciones().subscribe({
      next: () => {
        this.seleccionesTemporales = [];
        this.showFloatingPanel = false;
        this.toastService.success('malla.selection.cleared');
      },
      error: () => {
        this.toastService.error('malla.selection.errorClear');
      }
    });
  }

  private async ejecutarGuardarSeleccion(): Promise<void> {
    await this.guardarSeleccion();
  }

  private async ejecutarImportMalla(): Promise<void> {
    await this.importMalla();
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
    if (!this.isValidImportVersion()) {
      this.importError = this.translateService.instant('malla.import.errorVersionInvalid');
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
      formData.append('version', this.importMallaVersion.trim());

      const result: any = await firstValueFrom(
        this.http.post(`${environment.backendUrl}/api/academico/mallas/importar`, formData, {
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

    if (!username) {
      return;
    }

    this.perfilService.completeTour(username).subscribe({
      next: () => {},
      error: () => {},
    });
  }
}
