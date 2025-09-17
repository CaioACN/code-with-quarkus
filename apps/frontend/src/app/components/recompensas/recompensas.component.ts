import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { trigger, state, style, transition, animate } from '@angular/animations';
import { RecompensaService } from '../../services/recompensa.service';
import { PontosService } from '../../services/pontos.service';
import { NotificationService } from '../../services/notification.service';
import { RecompensaRequestDTO, RecompensaResponseDTO, TipoRecompensa, ResgateRequestDTO } from '../../models/recompensa.model';
import { SaldoUsuarioDTO } from '../../models/pontos.model';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-recompensas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recompensas.component.html',
  styleUrls: ['./recompensas.component.scss'],
  animations: [
    trigger('fadeInOut', [
      state('in', style({ opacity: 1, transform: 'translateY(0) scale(1)' })),
      transition('void => in', [
        style({ opacity: 0, transform: 'translateY(-20px) scale(0.8)' }),
        animate('300ms ease-in')
      ]),
      transition('in => void', [
        animate('300ms ease-out', style({ opacity: 0, transform: 'translateY(-20px) scale(0.8)' }))
      ])
    ])
  ]
})
export class RecompensasComponent implements OnInit, OnDestroy {
  recompensas: RecompensaResponseDTO[] = [];
  saldoUsuario: SaldoUsuarioDTO | null = null;
  usuarioId: number = 4; // Em uma aplicação real, viria da autenticação
  cartaoId: number | null = null;

  novaRecompensa: RecompensaRequestDTO = {
    tipo: TipoRecompensa.GIFT,
    descricao: '',
    custoPontos: 0,
    estoque: 0,
    parceiroId: null,
    ativo: true
  };

  tiposRecompensa = [
    { value: TipoRecompensa.MILHAS, label: 'Milhas' },
    { value: TipoRecompensa.GIFT, label: 'Vale Presente' },
    { value: TipoRecompensa.CASHBACK, label: 'Cashback' },
    { value: TipoRecompensa.PRODUTO, label: 'Produto' }
  ];

  loading = false;
  creating = false;
  error: string | null = null;
  success: string | null = null; // texto do toast
  showForm = false;

  // Timer do toast
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private recompensaService: RecompensaService,
    private pontosService: PontosService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.carregarRecompensas();
    this.carregarSaldoUsuario();
  }

  ngOnDestroy(): void {
    if (this.toastTimer) {
      clearTimeout(this.toastTimer);
      this.toastTimer = null;
    }
  }

  carregarRecompensas(): void {
    this.loading = true;
    this.error = null;

    this.recompensaService.getRecompensas(true).subscribe({
      next: (recompensas) => {
        this.recompensas = recompensas;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erro ao carregar recompensas: ' + (err?.message || err);
        this.loading = false;
      }
    });
  }

  /** remove foco do botão para evitar visual de "selecionado" */
  onSubmitClick(event: Event): void {
    const el = event.currentTarget as HTMLElement | null;
    el?.blur();
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  }

  /** Helper: mostra um toast por 5s */
  private showToast(message: string, durationMs = 5000): void {
    this.success = message;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.success = null;
      this.toastTimer = null;
    }, durationMs);
  }

  criarRecompensa(): void {
    // tira foco do botão para não ficar com estilo "pressionado"
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }

    if (!this.validarFormulario()) {
      this.showToast('Validação falhou. Verifique os campos obrigatórios.');
      return;
    }

    this.error = null;

    this.recompensaService
      .criarRecompensa(this.novaRecompensa)
      .subscribe({
        next: (recompensa) => {
          // Sucesso
          this.recompensas.unshift(recompensa);
          this.showToast('Recompensa criada com sucesso!');
          this.resetarFormulario();
          this.showForm = false; // feche o formulário após criar
        },
        error: (err) => {
          // Erro
          this.error = 'Erro ao criar recompensa: ' + (err?.message || err);
          this.showToast('Erro ao criar recompensa.');
        }
      });
  }

  validarFormulario(): boolean {
    if (!this.novaRecompensa.descricao || !this.novaRecompensa.descricao.trim()) {
      this.error = 'Descrição é obrigatória';
      return false;
    }
    if (!this.novaRecompensa.custoPontos || this.novaRecompensa.custoPontos <= 0) {
      this.error = 'Custo em pontos deve ser maior que zero';
      return false;
    }
    if (this.novaRecompensa.estoque < 0) {
      this.error = 'Estoque deve ser maior ou igual a zero';
      return false;
    }
    if (!this.novaRecompensa.tipo) {
      this.error = 'Tipo é obrigatório';
      return false;
    }
    return true;
  }

  resetarFormulario(): void {
    this.novaRecompensa = {
      tipo: TipoRecompensa.GIFT,
      descricao: '',
      custoPontos: 0,
      estoque: 0,
      parceiroId: null,
      ativo: true
    };
    this.error = null;
    // creating/loading/success são controlados pelos fluxos de criação e pelo toast
  }

  toggleForm(): void {
    // tira o foco do botão toggle, se houver
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }

    this.showForm = !this.showForm;

    // Ao FECHAR o formulário sem criar recompensa
    if (!this.showForm && !this.success) {
      this.resetarFormulario();
    }
  }

  formatarTipo(tipo: string): string {
    const tipoObj = this.tiposRecompensa.find(t => t.value === tipo);
    return tipoObj ? tipoObj.label : tipo;
  }

  formatarEstoque(estoque: number | null): string {
    return estoque === null ? 'Ilimitado' : estoque.toString();
  }

  carregarSaldoUsuario(): void {
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

  resgatar(recompensa: RecompensaResponseDTO): void {
    if (!recompensa.ativo) {
      this.showToast('Esta recompensa não está ativa.');
      return;
    }

    if (!this.cartaoId) {
      this.showToast('Nenhum cartão encontrado para realizar o resgate.');
      return;
    }

    if (!this.validarSaldoParaResgate(recompensa)) {
      this.showToast('Saldo insuficiente para este resgate.');
      return;
    }

    const resgate: ResgateRequestDTO = {
      usuarioId: this.usuarioId,
      recompensaId: recompensa.id,
      cartaoId: this.cartaoId,
      observacao: `Resgate de ${recompensa.descricao}`
    };

    this.recompensaService.solicitarResgate(resgate).subscribe({
      next: (response) => {
        this.showToast(`Resgate de "${recompensa.descricao}" solicitado com sucesso!`);
        
        // Notificar outros componentes
        this.notificationService.notifyResgateCreated({
          recompensaId: recompensa.id,
          recompensaDescricao: recompensa.descricao,
          custoPontos: recompensa.custoPontos
        });
        
        this.carregarSaldoUsuario(); // Atualiza o saldo
        this.carregarRecompensas(); // Recarrega as recompensas para refletir mudanças no estoque
        
        // Mostra mensagem informando que as telas foram atualizadas
        setTimeout(() => {
          this.showToast('Telas de Recompensas, Resgates e Painel atualizadas!');
        }, 2500);
      },
      error: (err) => {
        this.error = 'Erro ao solicitar resgate: ' + (err?.message || err);
        this.showToast('Erro ao solicitar resgate.');
      }
    });
  }

  private validarSaldoParaResgate(recompensa: RecompensaResponseDTO): boolean {
    if (!this.saldoUsuario || !this.cartaoId) return false;
    
    const saldoCartao = this.getSaldoCartao(this.cartaoId);
    return saldoCartao >= recompensa.custoPontos;
  }

  private getSaldoCartao(cartaoId: number): number {
    if (!this.saldoUsuario) return 0;
    
    const saldo = this.saldoUsuario.saldos.find(s => s.cartaoId === cartaoId);
    return saldo ? saldo.saldo : 0;
  }
}
