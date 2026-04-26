import { NgFor, NgIf } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import {
  HorarioActualResponse,
  HorarioActualService,
  HorarioClase,
} from '../../services/academico/horario-actual.service';
import { ApiService } from '../../services/api.service';
import { TomaSeleccionService } from '../../services/academico/toma-seleccion.service';

export interface MateriaSeleccionada {
  id: number;
  nombre: string;
  creditos: number;
  ofertaId: number;
}

@Component({
  selector: 'app-toma-de-materias',
  imports: [NgIf, NgFor],
  templateUrl: './toma-de-materias.html',
  styleUrl: './toma-de-materias.scss',
})
export class TomaDeMaterias implements OnInit {
  protected readonly dias = ['Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado'];
  protected readonly timeRowsDefault = [
    '07:00 - 08:30',
    '08:45 - 10:15',
    '10:30 - 12:00',
    '12:15 - 13:45',
    '14:00 - 15:30',
    '15:45 - 17:15',
    '17:30 - 19:00',
    '19:15 - 20:45',
  ];

  protected horario: HorarioActualResponse | null = null;
  protected loading = true;
  protected error = false;
  protected timeRows: string[] = [...this.timeRowsDefault];
  protected cellMap = new Map<string, HorarioClase[]>();

  protected materiasSeleccionadas: MateriaSeleccionada[] = [];
  protected totalCreditosSeleccionados = 0;
  protected creditosConfirmados = 0;

  constructor(
    private readonly horarioActualService: HorarioActualService,
    private readonly apiService: ApiService,
    private readonly tomaSeleccionService: TomaSeleccionService
  ) {}

  ngOnInit(): void {
    this.cargarHorarioYSelecciones();

    this.tomaSeleccionService.seleccion$.subscribe(materias => {
      this.materiasSeleccionadas = materias;
      this.calcularTotalCreditos();
    });
  }

  private calcularTotalCreditos(): void {
    const creditosEnCesta = this.materiasSeleccionadas.reduce((sum, m) => sum + (Number(m.creditos) || 0), 0);
    this.totalCreditosSeleccionados = this.creditosConfirmados + creditosEnCesta;
  }

  protected removerMateria(id: number): void {
    const confirmacion = window.confirm('¿Estás seguro de deseleccionar esta materia?');
    if (confirmacion) {
      this.tomaSeleccionService.removerMateria(id);
    }
  }

  protected confirmarInscripcion(): void {
    const body = {
      ofertaIds: this.materiasSeleccionadas.map(m => m.ofertaId)
    };

    this.apiService.post('/api/academico/toma-materias', body).subscribe({
      next: () => {
        alert('Registro exitoso');
        const creditosNuevos = this.materiasSeleccionadas.reduce((sum, m) => sum + (Number(m.creditos) || 0), 0);
        this.creditosConfirmados += creditosNuevos;

        this.tomaSeleccionService.limpiar();
        this.cargarHorarioYSelecciones();
      },
      error: (err) => {
        alert('Error al registrar: ' + (err.error?.message || 'Error desconocido'));
      }
    });
  }

  private cargarHorarioYSelecciones(): void {
    this.loading = true;
    this.error = false;
    this.horarioActualService.getHorarioActual().subscribe({
      next: (horario) => {
        this.horario = horario;
        this.buildGrid(horario.clases ?? []);
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      },
    });
  }

  protected getCellItems(timeRow: string, dia: string): HorarioClase[] {
    return this.cellMap.get(this.cellKey(timeRow, dia)) ?? [];
  }

  private buildGrid(clases: HorarioClase[]): void {
    this.cellMap.clear();
    const rowsFromData = new Set<string>();

    for (const clase of clases) {
      const diaNorm = this.normalizeDay(clase.dia);
      if (!diaNorm) {
        continue;
      }

      const timeRow = `${clase.horaInicio} - ${clase.horaFin}`;
      rowsFromData.add(timeRow);

      const key = this.cellKey(timeRow, diaNorm);
      const current = this.cellMap.get(key) ?? [];
      current.push(clase);
      this.cellMap.set(key, current);
    }

    const finalRows = rowsFromData.size > 0 ? Array.from(rowsFromData) : [...this.timeRowsDefault];
    this.timeRows = finalRows.sort((a, b) => {
      const aStart = a.split(' - ')[0] ?? '';
      const bStart = b.split(' - ')[0] ?? '';
      return aStart.localeCompare(bStart);
    });
  }

  private normalizeDay(day: string): string | null {
    const normalized = (day ?? '')
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '');

    switch (normalized) {
      case 'lunes':
        return 'Lunes';
      case 'martes':
        return 'Martes';
      case 'miercoles':
        return 'Miercoles';
      case 'jueves':
        return 'Jueves';
      case 'viernes':
        return 'Viernes';
      case 'sabado':
        return 'Sabado';
      default:
        return null;
    }
  }

  private cellKey(timeRow: string, dia: string): string {
    return `${timeRow}|${dia}`;
  }
}
