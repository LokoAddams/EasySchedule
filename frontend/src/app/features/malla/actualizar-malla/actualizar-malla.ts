import { Component, OnInit } from '@angular/core';
import { NgFor, NgIf, NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

import { ToastService } from '../../../core/services/toast.service';
import { MallaCatalogoService, MallaMateria } from '../../../services/academico/malla-catalogo.service';
import { EstadoMateriaService, EstadoMateriaItem, EstadoMateriaRequest } from '../../../services/academico/estado-materia.service';
import { SeleccionAcademica, SeleccionAcademicaService } from '../../../services/academico/seleccion-academica.service';

type EstadoUI = 'APROBADA' | 'CURSANDO' | 'PENDIENTE';

interface MateriaConEstado extends MallaMateria {
  estadoUI: EstadoUI;
}

@Component({
  selector: 'app-actualizar-malla',
  imports: [FormsModule, NgFor, NgIf, TranslatePipe],
  templateUrl: './actualizar-malla.html',
  styleUrl: './actualizar-malla.scss',
})
export class ActualizarMalla implements OnInit {
  protected materias: MateriaConEstado[] = [];
  protected loading = false;
  protected saving = false;
  protected loadError = false;
  protected noHayMallaSeleccionada = false;
  protected universidadNombre = '';
  protected carreraNombre = '';
  protected mallaNombre = '';
  protected mallaId: number | null = null;

  protected selectedMateriaId: number | null = null;
  protected selectedEstado: EstadoUI = 'PENDIENTE';
  private estadoAnterior: EstadoUI = 'PENDIENTE';

  constructor(
    private readonly mallaCatalogoService: MallaCatalogoService,
    private readonly estadoMateriaService: EstadoMateriaService,
    private readonly seleccionAcademicaService: SeleccionAcademicaService,
    private readonly toastService: ToastService,
    private readonly router: Router,
  ) {}

  async ngOnInit(): Promise<void> {
    await this.cargarDatos();
  }

  private async cargarDatos(): Promise<void> {
    this.loading = true;
    this.loadError = false;
    this.noHayMallaSeleccionada = false;

    try {
      const seleccion: SeleccionAcademica = await firstValueFrom(this.seleccionAcademicaService.getSeleccionActual());

      if (seleccion.mallaId === null) {
        console.warn('No hay malla seleccionada');
        this.noHayMallaSeleccionada = true;
        this.loading = false;
        return;
      }

      this.mallaId = seleccion.mallaId;
      this.universidadNombre = seleccion.universidad ?? '';
      this.carreraNombre = seleccion.carrera ?? '';
      this.mallaNombre = seleccion.malla ?? '';

      const [materiasBD, estados]: [MallaMateria[], EstadoMateriaItem[]] = await Promise.all([
        firstValueFrom(this.mallaCatalogoService.getMateriasPorMalla(seleccion.mallaId)),
        firstValueFrom(this.estadoMateriaService.getEstadosPorMalla(seleccion.mallaId)),
      ]);

      const estadosMap = new Map<number, string>(estados.map((e: EstadoMateriaItem) => [e.mallaMateriaId, e.estado]));

      this.materias = materiasBD.map((m: MallaMateria) => ({
        ...m,
        estadoUI: this.mapEstadoBDToUI(estadosMap.get(m.id)),
      }));

      this.materias.sort((a, b) => a.nombreMateria.localeCompare(b.nombreMateria));

      if (this.materias.length > 0) {
        this.selectedMateriaId = this.materias[0].id;
        this.onMateriaChange();
      }
    } catch (error) {
      console.error('Error al cargar datos:', error);
      this.loadError = true;
    } finally {
      this.loading = false;
    }
  }

  protected onMateriaChange(): void {
    if (this.selectedMateriaId !== null) {
      const materia = this.materias.find(m => m.id === this.selectedMateriaId);
      if (materia) {
        this.selectedEstado = materia.estadoUI;
        this.estadoAnterior = materia.estadoUI;
      }
    }
  }

  protected async actualizarMateria(): Promise<void> {
    if (this.selectedMateriaId === null) {
      return;
    }

    const estadoAActualizar = this.selectedEstado;
    this.saving = true;

    try {
      const request: EstadoMateriaRequest = {
        mallaMateriaId: this.selectedMateriaId,
        estado: this.mapEstadoUIToBD(estadoAActualizar),
      };

      await firstValueFrom(this.estadoMateriaService.guardarEstado(request));

      const materiaIndex = this.materias.findIndex(m => m.id === this.selectedMateriaId);
      if (materiaIndex !== -1) {
        this.materias[materiaIndex].estadoUI = estadoAActualizar;
      }

      this.estadoAnterior = estadoAActualizar;
      this.toastService.success('malla.UpdateCourse.success');
    } catch (error) {
      console.error('Error al actualizar:', error);
      this.toastService.error('malla.UpdateCourse.errorUpdate');
      const materiaIndex = this.materias.findIndex(m => m.id === this.selectedMateriaId);
      if (materiaIndex !== -1) {
        this.materias[materiaIndex].estadoUI = this.estadoAnterior;
        this.selectedEstado = this.estadoAnterior;
      }
    } finally {
      this.saving = false;
    }
  }

  protected volver(): void {
    this.router.navigate(['/malla']);
  }

  protected irAMalla(): void {
    this.router.navigate(['/malla']);
  }

  protected async reintentar(): Promise<void> {
    await this.cargarDatos();
  }

  protected getEstadoLabel(estado: EstadoUI): string {
    const labelMap: Record<EstadoUI, string> = {
      'APROBADA': 'Completada',
      'CURSANDO': 'En curso',
      'PENDIENTE': 'Pendiente',
    };
    return labelMap[estado];
  }

  private mapEstadoBDToUI(estado: string | undefined): EstadoUI {
    if (!estado) return 'PENDIENTE';
    const map: Record<string, EstadoUI> = {
      'aprobada': 'APROBADA',
      'cursando': 'CURSANDO',
      'pendiente': 'PENDIENTE',
    };
    return map[estado] ?? 'PENDIENTE';
  }

  private mapEstadoUIToBD(estado: EstadoUI): 'aprobada' | 'pendiente' | 'cursando' {
    const map: Record<EstadoUI, 'aprobada' | 'pendiente' | 'cursando'> = {
      'APROBADA': 'aprobada',
      'CURSANDO': 'cursando',
      'PENDIENTE': 'pendiente',
    };
    return map[estado];
  }
}