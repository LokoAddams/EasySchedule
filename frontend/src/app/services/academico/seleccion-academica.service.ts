import { Injectable } from '@angular/core';

export interface SeleccionAcademica {
  universidadId: number | null;
  mallaId: number | null;
}

@Injectable({
  providedIn: 'root',
})
export class SeleccionAcademicaService {}
