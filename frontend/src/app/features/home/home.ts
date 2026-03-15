import { Component, OnInit, signal } from '@angular/core';

import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  protected readonly healthStatus = signal<string>($localize`:@@home.checkingConnection:Comprobando conexión...`);

  constructor(private readonly apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService.get<unknown[]>('/api/estudiantes').subscribe({
      next: () => {
        this.healthStatus.set($localize`:@@home.connectionSuccess:Conexión exitosa`);
      },
      error: (error) => {
        this.healthStatus.set(
          $localize`:@@home.connectionError:Error de conexión: ${error.message}`,
        );
      },
    });
  }
}
