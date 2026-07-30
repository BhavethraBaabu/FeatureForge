import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { EvaluateFlagResponse, FeatureFlag } from './models';

export interface CreateFlagRequest {
  key: string;
  name: string;
  description?: string;
  enabled: boolean;
  rolloutPercentage: number;
}

export interface UpdateFlagRequest {
  name?: string;
  description?: string;
  enabled?: boolean;
  rolloutPercentage?: number;
}

@Injectable({ providedIn: 'root' })
export class FlagService {
  constructor(private http: HttpClient) {}

  listForProject(projectId: string): Observable<FeatureFlag[]> {
    return this.http.get<FeatureFlag[]>(`${environment.apiBaseUrl}/projects/${projectId}/flags`);
  }

  create(projectId: string, request: CreateFlagRequest): Observable<FeatureFlag> {
    return this.http.post<FeatureFlag>(`${environment.apiBaseUrl}/projects/${projectId}/flags`, request);
  }

  update(flagId: string, request: UpdateFlagRequest): Observable<FeatureFlag> {
    return this.http.patch<FeatureFlag>(`${environment.apiBaseUrl}/flags/${flagId}`, request);
  }

  delete(flagId: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/flags/${flagId}`);
  }

  evaluate(flagId: string, targetingKey: string): Observable<EvaluateFlagResponse> {
    return this.http.post<EvaluateFlagResponse>(
      `${environment.apiBaseUrl}/flags/${flagId}/evaluate`,
      { targetingKey }
    );
  }
}
