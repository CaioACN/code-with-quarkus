import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { DashboardService } from '../../services';
import { NotificationService } from '../../services/notification.service';
import { DashboardDTO, SuccessResponse } from '../../models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {
  dashboard: DashboardDTO | null = null;
  loading = false;
  error: string | null = null;
  private notificationSubscription?: Subscription;

  constructor(
    private dashboardService: DashboardService,
    private notificationService: NotificationService
  ) { }

  ngOnInit(): void {
    this.carregarDashboard();
    
    // Escutar notificações de novos resgates
    this.notificationSubscription = this.notificationService.getNotifications().subscribe(notification => {
      if (notification && notification.type === 'resgate_created') {
        console.log('Painel: Notificação recebida - novo resgate criado', notification.data);
        this.carregarDashboard();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }

  carregarDashboard(): void {
    this.loading = true;
    this.error = null;

    this.dashboardService.consultarDashboard().subscribe({
      next: (response: SuccessResponse<DashboardDTO>) => {
        this.dashboard = response.data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erro ao carregar painel: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  formatarNumero(numero: number): string {
    return new Intl.NumberFormat('pt-BR').format(numero);
  }

  formatarData(data: string): string {
    return new Date(data).toLocaleDateString('pt-BR');
  }

  obterCorStatus(status: string): string {
    const cores: { [key: string]: string } = {
      'PENDENTE': '#ffc107',
      'APROVADO': '#28a745',
      'CONCLUIDO': '#007bff',
      'NEGADO': '#dc3545',
      'CANCELADO': '#6c757d'
    };
    return cores[status] || '#6c757d';
  }
}
