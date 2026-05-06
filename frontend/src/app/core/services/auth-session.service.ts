import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthSessionService {
  private readonly usernameStorageKey = 'easySchedule.currentUsername';
  private readonly tokenStorageKey = 'easySchedule.token';
  private readonly tokenExpiresAtStorageKey = 'easySchedule.tokenExpiresAt';
  private readonly profileCompletedStorageKey = 'easySchedule.profileCompleted';

  private readonly profileCompletedSubject = new Subject<boolean>();
  public readonly profileCompleted$ = this.profileCompletedSubject.asObservable();

  setCurrentUsername(username: string): void {
    const trimmedUsername = username.trim();

    if (!trimmedUsername) {
      return;
    }

    localStorage.setItem(this.usernameStorageKey, trimmedUsername);
  }

  setAuthToken(token: string, expiresInSeconds = 3600): void {
    const normalizedToken = token.trim();
    if (!normalizedToken) {
      return;
    }

    const ttlSeconds = Number.isFinite(expiresInSeconds) && expiresInSeconds > 0
      ? Math.floor(expiresInSeconds)
      : 3600;
    const expiresAt = Date.now() + ttlSeconds * 1000;

    localStorage.setItem(this.tokenStorageKey, normalizedToken);
    localStorage.setItem('token', normalizedToken);
    localStorage.setItem(this.tokenExpiresAtStorageKey, String(expiresAt));
  }

  getAuthToken(): string | null {
    const token = localStorage.getItem(this.tokenStorageKey) ?? localStorage.getItem('token');
    if (!token) {
      return null;
    }

    const rawExpiresAt = localStorage.getItem(this.tokenExpiresAtStorageKey);
    const expiresAt = rawExpiresAt ? Number(rawExpiresAt) : NaN;

    if (!Number.isFinite(expiresAt) || Date.now() >= expiresAt) {
      this.clearSession();
      return null;
    }

    const normalizedToken = token.trim();
    if (!normalizedToken) {
      this.clearSession();
      return null;
    }

    return normalizedToken;
  }

  isLoggedIn(): boolean {
    return this.getAuthToken() !== null;
  }

  setProfileCompleted(profileCompleted: boolean): void {
    const wasCompleted = this.isProfileCompleted();
    localStorage.setItem(this.profileCompletedStorageKey, String(profileCompleted));
    
    if (!wasCompleted && profileCompleted) {
      this.profileCompletedSubject.next(true);
    }
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
    localStorage.removeItem(this.tokenExpiresAtStorageKey);
    localStorage.removeItem(this.usernameStorageKey);
    localStorage.removeItem(this.profileCompletedStorageKey);
  }
}
