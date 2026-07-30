export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  yourRole: 'OWNER' | 'ADMIN' | 'MEMBER';
  createdAt: string;
}

export interface Project {
  id: string;
  organizationId: string;
  name: string;
  key: string;
  createdAt: string;
}

export interface FeatureFlag {
  id: string;
  projectId: string;
  key: string;
  name: string;
  description: string | null;
  enabled: boolean;
  rolloutPercentage: number;
  createdAt: string;
  updatedAt: string;
}

export interface EvaluateFlagResponse {
  flagKey: string;
  targetingKey: string;
  enabled: boolean;
  reason: 'FLAG_DISABLED' | 'OVERRIDE' | 'ROLLOUT_MATCH' | 'ROLLOUT_MISS';
}

export interface FlagChangeEvent {
  type: 'CREATED' | 'UPDATED' | 'DELETED';
  flag: FeatureFlag;
}
