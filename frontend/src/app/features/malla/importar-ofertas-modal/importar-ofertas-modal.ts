import { NgClass, NgFor, NgIf } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

interface OfertaArchivoRow {
  rowNumber: number;
  codigoMateria: string;
  nombreMateria?: string;
  paralelo: string;
  semestreAcademico: string;
  dia: string;
  horaInicio: string;
  horaFin: string;
  docente?: string;
  aula?: string;
}

interface OfertaPreview {
  key: string;
  codigoMateria: string;
  nombreMateria?: string;
  paralelo: string;
  semestreAcademico: string;
  docente?: string;
  aula?: string;
  horarios: Array<{
    rowNumber: number;
    dia: string;
    horaInicio: string;
    horaFin: string;
  }>;
}

interface ImportError {
  rowNumber: number;
  field: string;
  reason: string;
  critical: boolean;
}

interface ImportSummary {
  totalRows: number;
  offersCreated: number;
  offersUpdated: number;
  scheduleBlocks: number;
  skippedRows: number;
  errors: number;
}

@Component({
  selector: 'app-importar-ofertas-modal',
  imports: [NgIf, NgFor, NgClass, TranslatePipe],
  templateUrl: './importar-ofertas-modal.html',
  styleUrl: './importar-ofertas-modal.scss',
})
export class ImportarOfertasModal {
  @ViewChild('fileInput') private fileInput?: ElementRef<HTMLInputElement>;

  @Input() mallaNombre = '';
  @Input() materiasMalla: Array<{ codigoMateria: string; nombreMateria: string }> = [];

  @Output() closeModal = new EventEmitter<void>();
  @Output() importFinished = new EventEmitter<ImportSummary>();

  protected selectedFileName = '';
  protected preview: OfertaPreview[] = [];
  protected errors: ImportError[] = [];
  protected warnings: ImportError[] = [];
  protected summary: ImportSummary | null = null;
  protected processing = false;
  protected completed = false;
  protected totalRowsRead = 0;

  protected requiredColumns = [
    'codigo_materia',
    'paralelo',
    'semestre_academico',
    'dia',
    'hora_inicio',
    'hora_fin',
    'docente',
    'aula',
  ];

  protected get hasCriticalErrors(): boolean {
    return this.errors.some((error) => error.critical);
  }

  protected get scheduleBlocksCount(): number {
    return this.preview.reduce((total, offer) => total + offer.horarios.length, 0);
  }

  protected async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    this.resetImportState();

    if (!file) {
      return;
    }

    this.selectedFileName = file.name;

    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.errors.push({
        rowNumber: 0,
        field: 'archivo',
        reason: 'Solo se permite archivo CSV en esta versión.',
        critical: true,
      });
      return;
    }

    const content = await file.text();
    this.processCsv(content);
  }

  protected clearFile(): void {
    this.selectedFileName = '';
    this.resetImportState();
    this.clearNativeFileInput();
  }

  protected cancel(): void {
    this.clearFile();
    this.closeModal.emit();
  }

  protected confirmImport(): void {
    if (this.hasCriticalErrors || this.preview.length === 0 || this.processing) {
      return;
    }

    this.processing = true;
    this.completed = false;

    setTimeout(() => {
      const scheduleBlocks = this.scheduleBlocksCount;

      this.summary = {
        totalRows: this.totalRowsRead,
        offersCreated: Math.max(1, this.preview.length - 1),
        offersUpdated: this.preview.length > 1 ? 1 : 0,
        scheduleBlocks,
        skippedRows: this.errors.filter((error) => error.critical).length,
        errors: this.errors.length,
      };

      this.processing = false;
      this.completed = true;

      this.importFinished.emit(this.summary);
      this.clearNativeFileInput();
      this.closeModal.emit();
    }, 900);
  }

  private processCsv(content: string): void {
    const lines = content
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line.length > 0);

    if (lines.length < 2) {
      this.errors.push({
        rowNumber: 0,
        field: 'archivo',
        reason: 'El archivo debe contener una fila de encabezados y al menos una fila de datos.',
        critical: true,
      });
      return;
    }

    const headers = this.parseCsvLine(lines[0]).map((header) => this.normalizeColumn(header));
    const missingColumns = this.requiredColumns.filter((column) => !headers.includes(column));

    if (missingColumns.length > 0) {
      missingColumns.forEach((column) => {
        this.errors.push({
          rowNumber: 1,
          field: column,
          reason: `Falta la columna obligatoria "${column}".`,
          critical: true,
        });
      });
      return;
    }

    const rows = lines.slice(1).map((line, index) => {
      const values = this.parseCsvLine(line);
      return this.mapRow(headers, values, index + 2);
    });

    this.totalRowsRead = rows.length;

    rows.forEach((row) => this.validateRow(row));

    this.preview = this.groupRows(rows.filter((row) => this.isRowImportable(row)));
  }

  private parseCsvLine(line: string): string[] {
    const values: string[] = [];
    let current = '';
    let insideQuotes = false;

    for (const char of line) {
      if (char === '"') {
        insideQuotes = !insideQuotes;
        continue;
      }

      if (char === ',' && !insideQuotes) {
        values.push(current.trim());
        current = '';
        continue;
      }

      current += char;
    }

    values.push(current.trim());
    return values;
  }

  private mapRow(headers: string[], values: string[], rowNumber: number): OfertaArchivoRow {
    const getValue = (column: string): string => {
      const index = headers.indexOf(column);
      return index >= 0 ? values[index]?.trim() ?? '' : '';
    };

    return {
      rowNumber,
      codigoMateria: getValue('codigo_materia'),
      nombreMateria: getValue('nombre_materia'),
      paralelo: getValue('paralelo'),
      semestreAcademico: getValue('semestre_academico'),
      dia: getValue('dia'),
      horaInicio: getValue('hora_inicio'),
      horaFin: getValue('hora_fin'),
      docente: getValue('docente'),
      aula: getValue('aula'),
    };
  }

  private validateRow(row: OfertaArchivoRow): void {
    this.validateRequired(row.rowNumber, 'codigo_materia', row.codigoMateria);
    this.validateRequired(row.rowNumber, 'paralelo', row.paralelo);
    this.validateRequired(row.rowNumber, 'semestre_academico', row.semestreAcademico);
    this.validateRequired(row.rowNumber, 'dia', row.dia);
    this.validateRequired(row.rowNumber, 'hora_inicio', row.horaInicio);
    this.validateRequired(row.rowNumber, 'hora_fin', row.horaFin);

    if (row.horaInicio && row.horaFin && !this.isValidHourRange(row.horaInicio, row.horaFin)) {
      this.errors.push({
        rowNumber: row.rowNumber,
        field: 'horario',
        reason: 'La hora de inicio debe ser menor a la hora de fin.',
        critical: true,
      });
    }

    if (row.codigoMateria && !this.existsInMalla(row.codigoMateria)) {
      this.errors.push({
        rowNumber: row.rowNumber,
        field: 'codigo_materia',
        reason: 'El código de materia no existe en la malla seleccionada.',
        critical: true,
      });
    }

    if (!row.docente) {
      this.warnings.push({
        rowNumber: row.rowNumber,
        field: 'docente',
        reason: 'Docente vacío. El registro podrá procesarse si el backend acepta este campo como opcional.',
        critical: false,
      });
    }

    if (!row.aula) {
      this.warnings.push({
        rowNumber: row.rowNumber,
        field: 'aula',
        reason: 'Aula vacía. El registro podrá procesarse si el backend acepta este campo como opcional.',
        critical: false,
      });
    }
  }

  private validateRequired(rowNumber: number, field: string, value: string): void {
    if (!value) {
      this.errors.push({
        rowNumber,
        field,
        reason: 'Campo obligatorio vacío.',
        critical: true,
      });
    }
  }

  private isValidHourRange(start: string, end: string): boolean {
    const hourFormat = /^([01]\d|2[0-3]):[0-5]\d$/;

    if (!hourFormat.test(start) || !hourFormat.test(end)) {
      return false;
    }

    return start < end;
  }

  private existsInMalla(codigoMateria: string): boolean {
    return this.materiasMalla.some(
      (materia) => materia.codigoMateria.toUpperCase() === codigoMateria.toUpperCase(),
    );
  }

  private isRowImportable(row: OfertaArchivoRow): boolean {
    return !this.errors.some((error) => error.rowNumber === row.rowNumber && error.critical);
  }

  private groupRows(rows: OfertaArchivoRow[]): OfertaPreview[] {
    const grouped = new Map<string, OfertaPreview>();

    rows.forEach((row) => {
      const key = [
        row.codigoMateria.toUpperCase(),
        row.paralelo.toUpperCase(),
        row.semestreAcademico.toUpperCase(),
      ].join('|');

      if (!grouped.has(key)) {
        grouped.set(key, {
          key,
          codigoMateria: row.codigoMateria,
          nombreMateria: row.nombreMateria,
          paralelo: row.paralelo,
          semestreAcademico: row.semestreAcademico,
          docente: row.docente,
          aula: row.aula,
          horarios: [],
        });
      }

      grouped.get(key)?.horarios.push({
        rowNumber: row.rowNumber,
        dia: row.dia,
        horaInicio: row.horaInicio,
        horaFin: row.horaFin,
      });
    });

    return Array.from(grouped.values());
  }

  private normalizeColumn(column: string): string {
    return column
      .trim()
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/\s+/g, '_');
  }

  private resetImportState(): void {
    this.preview = [];
    this.errors = [];
    this.warnings = [];
    this.summary = null;
    this.completed = false;
    this.totalRowsRead = 0;
  }

  private clearNativeFileInput(): void {
    if (this.fileInput?.nativeElement) {
      this.fileInput.nativeElement.value = '';
    }
  }
}