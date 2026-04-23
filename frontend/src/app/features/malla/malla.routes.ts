import { Routes } from '@angular/router';
import { Malla } from './malla';

export const MALLA_ROUTES: Routes = [
  { path: '', component: Malla },
  {
    path: 'actualizar',
    loadComponent: () =>
      import('./actualizar-malla/actualizar-malla').then((m) => m.ActualizarMalla),
  },
];