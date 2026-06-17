import { NgFor, NgIf } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';

import { 
  OfertaImportService, 
  OfertaListResponse, 
  OfertaMateriaEdicionResponse, 
  OfertaMateriaUpdateRequest 
} from '../../../services/academico/oferta-import.service';

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

  protected editingOferta: OfertaMateriaEdicionResponse | null = null;
  protected editErrorMsg = '';
  protected editSuccessMsg = '';
  protected isValidating = false;
  protected isSaving = false;

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

  protected async abrirEdicion(id: number): Promise<void> {
    try {
      this.editingOferta = await firstValueFrom(this.ofertaImportService.obtenerOfertaParaEdicion(id));
      if (this.editingOferta && this.editingOferta.horarios) {
        this.editingOferta.horarios.forEach(h => {
          if (h.dia) {
            h.dia = h.dia.toUpperCase();
          }
        });
      }
      this.editErrorMsg = '';
      this.editSuccessMsg = '';
    } catch {
      alert('Error al obtener la oferta para edición.');
    }
  }

  protected cerrarEdicion(): void {
    this.editingOferta = null;
  }

  protected addHorarioBlock(): void {
    if (this.editingOferta) {
      this.editingOferta.horarios.push({
        dia: 'LUNES',
        horaInicio: '08:15',
        horaFin: '09:45'
      });
    }
  }

  protected removeHorarioBlock(index: number): void {
    if (this.editingOferta) {
      this.editingOferta.horarios.splice(index, 1);
    }
  }

  private buildUpdateRequest(): OfertaMateriaUpdateRequest {
    if (!this.editingOferta) throw new Error('No editing oferta');
    return {
      codigoMateria: this.editingOferta.codigoMateria || '',
      nombreMateria: this.editingOferta.nombreMateria || '',
      paralelo: this.editingOferta.paralelo || '',
      semestre: this.editingOferta.semestre || '',
      docente: this.editingOferta.docente || '',
      aula: this.editingOferta.aula || '',
      horarios: this.editingOferta.horarios || []
    };
  }

  protected async validarEdicion(): Promise<void> {
    if (!this.editingOferta) return;
    this.isValidating = true;
    this.editErrorMsg = '';
    this.editSuccessMsg = '';
    
    try {
      await firstValueFrom(this.ofertaImportService.validarActualizacion(
        this.editingOferta.id, 
        this.buildUpdateRequest()
      ));
      this.editSuccessMsg = '¡Validación exitosa! No se detectaron cruces de horario.';
    } catch (e: any) {
      this.editErrorMsg = e.error?.message || 'Error durante la validación.';
    } finally {
      this.isValidating = false;
    }
  }

  protected async guardarEdicion(): Promise<void> {
    if (!this.editingOferta) return;
    this.isSaving = true;
    this.editErrorMsg = '';
    this.editSuccessMsg = '';
    
    try {
      await firstValueFrom(this.ofertaImportService.actualizarOferta(
        this.editingOferta.id, 
        this.buildUpdateRequest()
      ));
      this.cerrarEdicion();
      await this.loadOfertas();
    } catch (e: any) {
      this.editErrorMsg = e.error?.message || 'Error al guardar la oferta.';
    } finally {
      this.isSaving = false;
    }
  }
}
