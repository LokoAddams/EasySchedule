import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { MateriaSeleccionada } from '../../features/toma-de-materias/toma-de-materias';

@Injectable({
  providedIn: 'root',
})
export class TomaSeleccionService {
  private readonly seleccionSubject = new BehaviorSubject<MateriaSeleccionada[]>([]);
  seleccion$ = this.seleccionSubject.asObservable();

  agregarMateria(materia: MateriaSeleccionada): void {
    const actual = this.seleccionSubject.value;
    if (!actual.find(m => m.id === materia.id)) {
      this.seleccionSubject.next([...actual, materia]);
    }
  }

  removerMateria(id: number): void {
    const actual = this.seleccionSubject.value;
    this.seleccionSubject.next(actual.filter(m => m.id !== id));
  }

  limpiar(): void {
    this.seleccionSubject.next([]);
  }
}
