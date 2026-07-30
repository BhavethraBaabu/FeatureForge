import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Project } from './models';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  constructor(private http: HttpClient) {}

  listForOrganization(organizationId: string): Observable<Project[]> {
    return this.http.get<Project[]>(`${environment.apiBaseUrl}/organizations/${organizationId}/projects`);
  }

  create(organizationId: string, name: string, key: string): Observable<Project> {
    return this.http.post<Project>(
      `${environment.apiBaseUrl}/organizations/${organizationId}/projects`,
      { name, key }
    );
  }
}
