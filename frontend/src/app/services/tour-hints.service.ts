import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TourHintsService {
  // Observable para controlar si el popover de "Toma de Materias" debe estar abierto
  private tomaMateriasPopoverOpen$ = new BehaviorSubject<boolean>(false);
  tomaMateriasPopoverOpen = this.tomaMateriasPopoverOpen$.asObservable();

  openTomaMateriasPopover(): void {
    this.tomaMateriasPopoverOpen$.next(true);
  }

  closeTomaMateriasPopover(): void {
    this.tomaMateriasPopoverOpen$.next(false);
  }
}
