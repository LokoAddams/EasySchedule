import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, Output, ViewChild } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './confirm-modal.html',
  styleUrl: './confirm-modal.scss',
})
export class ConfirmModal implements AfterViewInit {
  @Input({ required: true }) title = 'confirmModal.defaultTitle';
  @Input({ required: true }) message = 'confirmModal.defaultMessage';
  @Input() confirmLabel = 'confirmModal.confirm';
  @Input() cancelLabel = 'confirmModal.cancel';

  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  @ViewChild('focusTrapStart') focusTrapStart!: ElementRef<HTMLButtonElement>;
  @ViewChild('focusTrapEnd') focusTrapEnd!: ElementRef<HTMLButtonElement>;
  @ViewChild('confirmBtn') confirmBtn!: ElementRef<HTMLButtonElement>;

  protected processing = false;

  ngAfterViewInit(): void {
    this.confirmBtn?.nativeElement?.focus();
  }

  @HostListener('keydown', ['$event'])
  protected handleKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.onCancel();
      return;
    }
    if (event.key !== 'Tab') {
      return;
    }
    if (event.shiftKey && event.target === this.focusTrapStart?.nativeElement) {
      event.preventDefault();
      this.focusTrapEnd?.nativeElement?.focus();
      return;
    }
    if (!event.shiftKey && event.target === this.focusTrapEnd?.nativeElement) {
      event.preventDefault();
      this.focusTrapStart?.nativeElement?.focus();
    }
  }

  protected onConfirm(): void {
    if (this.processing) {
      return;
    }
    this.processing = true;
    this.confirm.emit();
  }

  protected onCancel(): void {
    if (this.processing) {
      return;
    }
    this.cancel.emit();
  }
}
