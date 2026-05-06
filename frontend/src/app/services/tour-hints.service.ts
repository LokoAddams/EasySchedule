import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { NgbPopover } from '@ng-bootstrap/ng-bootstrap';

@Injectable({
  providedIn: 'root'
})
export class TourHintsService {
  private tomaMateriasPopover?: NgbPopover;

  private tomaMateriasPopoverOpenSubject = new Subject<boolean>();

  public tomaMateriasPopoverOpen = this.tomaMateriasPopoverOpenSubject.asObservable();

  setTomaMateriasPopover(popover: NgbPopover): void {
    this.tomaMateriasPopover = popover;
  }

  openTomaMateriasPopover(): void {
    this.tomaMateriasPopover?.open();
    this.tomaMateriasPopoverOpenSubject.next(true);
  }

  closeTomaMateriasPopover(): void {
    this.tomaMateriasPopover?.close();
    this.tomaMateriasPopoverOpenSubject.next(false);
  }

  requestOpenTomaMateriasPopover(): void {
    this.tomaMateriasPopoverOpenSubject.next(true);
  }

  requestCloseTomaMateriasPopover(): void {
    this.tomaMateriasPopoverOpenSubject.next(false);
  }
}