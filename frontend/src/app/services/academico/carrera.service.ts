import { Injectable } from '@angular/core';

export interface CarreraCatalogoItem {
  id: number;
  universidadId: number;
  nombre: string;
  codigo: string;
}

@Injectable({
  providedIn: 'root',
})
export class CarreraService {}
