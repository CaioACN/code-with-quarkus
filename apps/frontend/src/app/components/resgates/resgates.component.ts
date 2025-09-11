import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { RecompensaService } from '../../services/recompensa.service';
import { PontosService } from '../../services/pontos.service';
import { NotificationService } from '../../services/notification.service';
import { RecompensaResponseDTO, ResgateResponseDTO } from '../../models/recompensa.model';
import { SaldoUsuarioDTO } from '../../models/pontos.model';

@Component({
  selector: 'app-resgates',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resgates.component.html',
  styleUrl: './resgates.component.scss'
})
export class ResgatesComponent implements OnInit, OnDestroy {
  resgates: ResgateResponseDTO[] = [];
  saldoUsuario: SaldoUsuarioDTO | null = null;
  usuarioId: number = 1; // Em uma aplicação real, viria da autenticação
  
  resgateSelecionado: ResgateResponseDTO | null = null;
  cartaoId: number | null = null;
  
  loading = false;
  error: string | null = null;
  success: string | null = null;
  private notificationSubscription?: Subscription;

  constructor(
    private recompensaService: RecompensaService,
    private pontosService: PontosService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.carregarDados();
    
    // Escutar notificações de novos resgates
    this.notificationSubscription = this.notificationService.getNotifications().subscribe(notification => {
      if (notification && notification.type === 'resgate_created') {
        console.log('Notificação recebida: novo resgate criado', notification.data);
        this.carregarDados();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.notificationSubscription) {
      this.notificationSubscription.unsubscribe();
    }
  }

  carregarDados(): void {
    this.loading = true;
    this.error = null;
    
    // Carregar resgates do usuário
    const pageRequest = { pagina: 1, tamanho: 20 };
    this.recompensaService.listarResgatesUsuario(this.usuarioId, pageRequest).subscribe({
      next: (response) => {
        this.resgates = response.data?.content || [];
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erro ao carregar resgates: ' + err.message;
        this.loading = false;
      }
    });

    // Carregar saldo do usuário
    this.pontosService.consultarSaldo(this.usuarioId).subscribe({
      next: (response) => {
        this.saldoUsuario = response.data;
        if (response.data.saldos.length > 0) {
          this.cartaoId = response.data.saldos[0].cartaoId;
        }
      },
      error: (err) => {
        console.error('Erro ao carregar saldo:', err);
      }
    });
  }

  selecionarResgate(resgate: ResgateResponseDTO): void {
    this.resgateSelecionado = resgate;
    this.error = null;
    this.success = null;
  }

  cancelarVisualizacao(): void {
    this.resgateSelecionado = null;
    this.error = null;
    this.success = null;
  }

  formatarStatus(status: string): string {
    const statusMap: { [key: string]: string } = {
      'PENDENTE': 'Pendente',
      'APROVADO': 'Aprovado',
      'CONCLUIDO': 'Concluído',
      'NEGADO': 'Negado',
      'CANCELADO': 'Cancelado'
    };
    return statusMap[status] || status;
  }

  formatarData(data: string): string {
    return new Date(data).toLocaleDateString('pt-BR');
  }

  getSaldoTotal(): number {
    if (!this.saldoUsuario) return 0;
    return this.saldoUsuario.saldos.reduce((total, saldo) => total + saldo.saldo, 0);
  }

  getSaldoCartao(cartaoId: number): number {
    if (!this.saldoUsuario) return 0;
    const saldo = this.saldoUsuario.saldos.find(s => s.cartaoId === cartaoId);
    return saldo ? saldo.saldo : 0;
  }
}
