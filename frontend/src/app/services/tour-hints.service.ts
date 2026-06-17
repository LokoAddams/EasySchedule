import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TourHintsService {
  private tomaMateriasPopoverOpen$ = new BehaviorSubject<boolean>(false);
  tomaMateriasPopoverOpen = this.tomaMateriasPopoverOpen$.asObservable();

  private tourStepRequest$ = new BehaviorSubject<number | null>(null);
  tourStepRequest = this.tourStepRequest$.asObservable();

  openTomaMateriasPopover(): void {
    this.tomaMateriasPopoverOpen$.next(true);
  }

  closeTomaMateriasPopover(): void {
    this.tomaMateriasPopoverOpen$.next(false);
  }

  requestTourStep(step: number): void {
    this.tourStepRequest$.next(step);
  }
}
