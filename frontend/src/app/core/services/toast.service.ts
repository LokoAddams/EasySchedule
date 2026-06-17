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
  private readonly errorTimeouts = new Map<string, number>();

  readonly toasts = this.toastList.asReadonly();

  success(messageKey: string, durationMs = 2800, translateParams?: Record<string, string | number>): void {
    this.show('success', messageKey, durationMs, translateParams);
  }

  error(messageKey: string, durationMs = 3600, translateParams?: Record<string, string | number>): void {
    const existing = this.toastList().find((t) => t.type === 'error' && t.messageKey === messageKey);

    if (existing) {
      const oldTimer = this.errorTimeouts.get(messageKey);
      if (oldTimer !== undefined) {
        clearTimeout(oldTimer);
      }

      const timer = window.setTimeout(() => {
        this.dismiss(existing.id);
        this.errorTimeouts.delete(messageKey);
      }, durationMs);

      this.errorTimeouts.set(messageKey, timer);
      return;
    }

    this.show('error', messageKey, durationMs, translateParams);
  }

  dismiss(id: number): void {
    const toast = this.toastList().find((t) => t.id === id);
    if (toast?.type === 'error') {
      const timer = this.errorTimeouts.get(toast.messageKey);
      if (timer !== undefined) {
        clearTimeout(timer);
        this.errorTimeouts.delete(toast.messageKey);
      }
    }

    this.toastList.update((current) => current.filter((t) => t.id !== id));
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

    const timer = window.setTimeout(() => {
      this.dismiss(id);
    }, durationMs);

    if (type === 'error') {
      this.errorTimeouts.set(messageKey, timer);
    }
  }
}
