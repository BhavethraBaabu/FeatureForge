import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from './models';

const ACCESS_TOKEN_KEY = 'ff_access_token';

/**
 * Token lives in localStorage so a page refresh doesn't boot the user back
 * to /login. This is a real standalone Angular app running in its own
 * origin, so localStorage is the normal, correct choice here.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly isAuthenticated = signal<boolean>(!!localStorage.getItem(ACCESS_TOKEN_KEY));

  constructor(private http: HttpClient) {}

  register(email: string, password: string, fullName: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/register`, { email, password, fullName })
      .pipe(tap((res) => this.storeToken(res)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiBaseUrl}/auth/login`, { email, password })
      .pipe(tap((res) => this.storeToken(res)));
  }

  logout(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    this.isAuthenticated.set(false);
  }

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  private storeToken(res: AuthResponse): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, res.accessToken);
    this.isAuthenticated.set(true);
  }
}
