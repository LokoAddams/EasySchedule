import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class AuthSessionService {
  private readonly usernameStorageKey = 'easySchedule.currentUsername';

  setCurrentUsername(username: string): void {
    const trimmedUsername = username.trim();

    if (!trimmedUsername) {
      return;
    }

    localStorage.setItem(this.usernameStorageKey, trimmedUsername);
  }

  getCurrentUsername(): string | null {
    const username = localStorage.getItem(this.usernameStorageKey);
    
    if (!username) {
      return null;
    }

    const trimmedUsername = username.trim();
    return trimmedUsername || null;
  }

  clearSession(): void {
    localStorage.removeItem(this.usernameStorageKey);
  }
}
