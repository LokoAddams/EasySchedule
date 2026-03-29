import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error';

export interface AppToast {
  id: number;
  type: ToastType;
  messageKey: string;
  translateParams?: Record<string, string | number>;
  durationMs: number;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private readonly toastList = signal<AppToast[]>([]);
  private nextId = 1;

  readonly toasts = this.toastList.asReadonly();

  success(messageKey: string, durationMs = 2800, translateParams?: Record<string, string | number>): void {
    this.show('success', messageKey, durationMs, translateParams);
  }

  error(messageKey: string, durationMs = 3600, translateParams?: Record<string, string | number>): void {
    this.show('error', messageKey, durationMs, translateParams);
  }

  dismiss(id: number): void {
    this.toastList.update((current) => current.filter((toast) => toast.id !== id));
  }

  private show(
    type: ToastType,
    messageKey: string,
    durationMs: number,
    translateParams?: Record<string, string | number>,
  ): void {
    const id = this.nextId++;
    const toast: AppToast = { id, type, messageKey, durationMs, translateParams };

    this.toastList.update((current) => [...current, toast]);

    window.setTimeout(() => {
      this.dismiss(id);
    }, durationMs);
  }
}
