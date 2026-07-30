import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html',
  styleUrl: './auth.component.css'
})
export class AuthComponent {
  mode = signal<'login' | 'register'>('login');
  email = '';
  password = '';
  fullName = '';
  errorMessage = signal<string | null>(null);
  isSubmitting = signal(false);

  constructor(private authService: AuthService, private router: Router) {}

  toggleMode(): void {
    this.mode.set(this.mode() === 'login' ? 'register' : 'login');
    this.errorMessage.set(null);
  }

  submit(): void {
    this.errorMessage.set(null);
    this.isSubmitting.set(true);

    const request$ =
      this.mode() === 'login'
        ? this.authService.login(this.email, this.password)
        : this.authService.register(this.email, this.password, this.fullName);

    request$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message ?? 'Something went wrong. Try again.');
      }
    });
  }
}
