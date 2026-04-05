import { Injectable } from '@angular/core';

export interface MallaCatalogoItem {
  id: number;
  nombre: string;
  version: string;
}

@Injectable({
  providedIn: 'root',
})
export class MallaCatalogoService {}
