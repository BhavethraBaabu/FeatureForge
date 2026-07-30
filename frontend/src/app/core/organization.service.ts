import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Organization } from './models';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  constructor(private http: HttpClient) {}

  list(): Observable<Organization[]> {
    return this.http.get<Organization[]>(`${environment.apiBaseUrl}/organizations`);
  }

  create(name: string, slug: string): Observable<Organization> {
    return this.http.post<Organization>(`${environment.apiBaseUrl}/organizations`, { name, slug });
  }
}
