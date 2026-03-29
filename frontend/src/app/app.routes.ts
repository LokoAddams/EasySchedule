import { Routes } from '@angular/router';

import { authGuard } from './auth.guard';
import { featureGuard } from './feature.guard';
import { guestGuard } from './guest.guard';
import { profileCompletedGuard } from './profile-completed.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  {
    path: 'home',
    loadChildren: () =>
      import('./features/home/home.routes').then((m) => m.HOME_ROUTES),
  },
  {
    path: 'malla',
    canActivate: [authGuard, featureGuard('malla'), profileCompletedGuard],
    loadChildren: () =>
      import('./features/malla/malla.routes').then((m) => m.MALLA_ROUTES),
  },
  {
    path: 'toma-de-materias',
    canActivate: [authGuard, featureGuard('tomaMaterias'), profileCompletedGuard],
    loadChildren: () =>
      import('./features/toma-de-materias/toma-de-materias.routes').then(
        (m) => m.TOMA_DE_MATERIAS_ROUTES,
      ),
  },
  {
    path: 'perfil',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/perfil/perfil.routes').then((m) => m.PERFIL_ROUTES),
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadChildren: () =>
      import('./features/login/login.routes').then((m) => m.LOGIN_ROUTES),
  },
  {
    path: 'registro',
    canActivate: [guestGuard],
    loadChildren: () =>
      import('./features/registro/registro.routes').then((m) => m.REGISTRO_ROUTES),
  },
  { path: '**', redirectTo: 'home' },
];
