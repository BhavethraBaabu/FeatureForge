import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { OrganizationService } from '../../core/organization.service';
import { ProjectService } from '../../core/project.service';
import { FlagService } from '../../core/flag.service';
import { WebsocketService } from '../../core/websocket.service';
import { EvaluateFlagResponse, FeatureFlag, Organization, Project } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  organizations = signal<Organization[]>([]);
  projects = signal<Project[]>([]);
  flags = signal<FeatureFlag[]>([]);

  selectedOrgId = signal<string | null>(null);
  selectedProjectId = signal<string | null>(null);

  showNewOrgForm = signal(false);
  newOrgName = '';
  newOrgSlug = '';

  showNewProjectForm = signal(false);
  newProjectName = '';
  newProjectKey = '';

  showNewFlagForm = signal(false);
  newFlagKey = '';
  newFlagName = '';
  newFlagRollout = 0;
  newFlagEnabled = false;

  evaluateTargetingKey: Record<string, string> = {};
  evaluateResult: Record<string, EvaluateFlagResponse> = {};

  private wsSubscription: Subscription | null = null;

  constructor(
    private authService: AuthService,
    private organizationService: OrganizationService,
    private projectService: ProjectService,
    private flagService: FlagService,
    private websocketService: WebsocketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrganizations();
  }

  ngOnDestroy(): void {
    this.wsSubscription?.unsubscribe();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  loadOrganizations(): void {
    this.organizationService.list().subscribe((orgs) => this.organizations.set(orgs));
  }

  createOrganization(): void {
    if (!this.newOrgName || !this.newOrgSlug) return;
    this.organizationService.create(this.newOrgName, this.newOrgSlug).subscribe((org) => {
      this.organizations.update((list) => [...list, org]);
      this.newOrgName = '';
      this.newOrgSlug = '';
      this.showNewOrgForm.set(false);
      this.selectOrganization(org.id);
    });
  }

  selectOrganization(orgId: string): void {
    this.selectedOrgId.set(orgId);
    this.selectedProjectId.set(null);
    this.flags.set([]);
    this.wsSubscription?.unsubscribe();
    this.projectService.listForOrganization(orgId).subscribe((projects) => this.projects.set(projects));
  }

  createProject(): void {
    const orgId = this.selectedOrgId();
    if (!orgId || !this.newProjectName || !this.newProjectKey) return;

    this.projectService.create(orgId, this.newProjectName, this.newProjectKey).subscribe((project) => {
      this.projects.update((list) => [...list, project]);
      this.newProjectName = '';
      this.newProjectKey = '';
      this.showNewProjectForm.set(false);
      this.selectProject(project.id);
    });
  }

  selectProject(projectId: string): void {
    this.selectedProjectId.set(projectId);
    this.wsSubscription?.unsubscribe();

    this.flagService.listForProject(projectId).subscribe((flags) => this.flags.set(flags));

    this.wsSubscription = this.websocketService.subscribeToProject(projectId).subscribe((event) => {
      if (event.type === 'DELETED') {
        this.flags.update((list) => list.filter((f) => f.id !== event.flag.id));
      } else {
        this.flags.update((list) => {
          const idx = list.findIndex((f) => f.id === event.flag.id);
          if (idx === -1) return [...list, event.flag];
          const copy = [...list];
          copy[idx] = event.flag;
          return copy;
        });
      }
    });
  }

  createFlag(): void {
    const projectId = this.selectedProjectId();
    if (!projectId || !this.newFlagKey || !this.newFlagName) return;

    this.flagService
      .create(projectId, {
        key: this.newFlagKey,
        name: this.newFlagName,
        enabled: this.newFlagEnabled,
        rolloutPercentage: this.newFlagRollout
      })
      .subscribe((flag) => {
        this.flags.update((list) => [...list, flag]);
        this.newFlagKey = '';
        this.newFlagName = '';
        this.newFlagRollout = 0;
        this.newFlagEnabled = false;
        this.showNewFlagForm.set(false);
      });
  }

  toggleEnabled(flag: FeatureFlag): void {
    this.flagService.update(flag.id, { enabled: !flag.enabled }).subscribe((updated) => {
      this.applyFlagUpdate(updated);
    });
  }

  updateRollout(flag: FeatureFlag, value: number): void {
    this.flagService.update(flag.id, { rolloutPercentage: value }).subscribe((updated) => {
      this.applyFlagUpdate(updated);
    });
  }

  deleteFlag(flag: FeatureFlag): void {
    if (!confirm(`Delete flag "${flag.key}"? This can't be undone.`)) return;
    this.flagService.delete(flag.id).subscribe(() => {
      this.flags.update((list) => list.filter((f) => f.id !== flag.id));
    });
  }

  runEvaluate(flag: FeatureFlag): void {
    const targetingKey = this.evaluateTargetingKey[flag.id];
    if (!targetingKey) return;
    this.flagService.evaluate(flag.id, targetingKey).subscribe((result) => {
      this.evaluateResult[flag.id] = result;
    });
  }

  private applyFlagUpdate(updated: FeatureFlag): void {
    this.flags.update((list) => list.map((f) => (f.id === updated.id ? updated : f)));
  }
}
