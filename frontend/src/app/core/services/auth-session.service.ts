import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class AuthSessionService {
  private readonly usernameStorageKey = 'easySchedule.currentUsername';
  private readonly tokenStorageKey = 'easySchedule.token';
  private readonly profileCompletedStorageKey = 'easySchedule.profileCompleted';

  setCurrentUsername(username: string): void {
    const trimmedUsername = username.trim();

    if (!trimmedUsername) {
      return;
    }

    localStorage.setItem(this.usernameStorageKey, trimmedUsername);
  }

  setAuthToken(token: string): void {
    const normalizedToken = token.trim();
    if (!normalizedToken) {
      return;
    }

    localStorage.setItem(this.tokenStorageKey, normalizedToken);
    localStorage.setItem('token', normalizedToken);
  }

  getAuthToken(): string | null {
    const token = localStorage.getItem(this.tokenStorageKey) ?? localStorage.getItem('token');
    if (!token) {
      return null;
    }

    const normalizedToken = token.trim();
    return normalizedToken || null;
  }

  isLoggedIn(): boolean {
    return this.getAuthToken() !== null;
  }

  setProfileCompleted(profileCompleted: boolean): void {
    localStorage.setItem(this.profileCompletedStorageKey, String(profileCompleted));
  }

  isProfileCompleted(): boolean {
    return localStorage.getItem(this.profileCompletedStorageKey) === 'true';
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
    localStorage.removeItem(this.tokenStorageKey);
    localStorage.removeItem('token');
    localStorage.removeItem(this.usernameStorageKey);
    localStorage.removeItem(this.profileCompletedStorageKey);
  }
}
