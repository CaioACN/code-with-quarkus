import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransacaoService } from '../../services';
import { 
  TransacaoRequestDTO, 
  TransacaoResponseDTO, 
  PageRequestDTO, 
  PageResponseDTO,
  StatusTransacao,
  SuccessResponse 
} from '../../models';

@Component({
  selector: 'app-transacoes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './transacoes.component.html',
  styleUrls: ['./transacoes.component.scss']
})
export class TransacoesComponent implements OnInit {
  transacoes: TransacaoResponseDTO[] = [];
  loading = false;
  error: string | null = null;
  
  // Paginação
  pageRequest: PageRequestDTO = {
    pagina: 1,
    tamanho: 10,
    ordenacao: 'dataEvento',
    direcao: 'DESC'
  };
  totalElements = 0;
  totalPages = 0;

  // Filtros
  filtros = {
    usuarioId: undefined as number | undefined,
    cartaoId: undefined as number | undefined,
    status: undefined as string | undefined,
    dataInicio: undefined as string | undefined,
    dataFim: undefined as string | undefined
  };

  // Formulário de nova transação
  novaTransacao: TransacaoRequestDTO = {
    cartaoId: 1,
    usuarioId: 4,
    valor: 0,
    moeda: 'BRL',
    mcc: '',
    categoria: '',
    parceiroId: undefined,
    dataEvento: this.getLocalDateTimeString(),
    autorizacao: ''
  };
  showForm = false;
  submitting = false;
  
  // Notificações e Modal de Confirmação
  notificacao: { mensagem: string; tipo: 'success' | 'error' } | null = null;
  modalConfirmacao: { mensagem: string; acao: () => void } | null = null;

  // Status options
  statusOptions = [
    { value: '', label: 'Todos' },
    { value: StatusTransacao.APROVADA, label: 'Aprovada' },
    { value: StatusTransacao.NEGADA, label: 'Negada' },
    { value: StatusTransacao.ESTORNADA, label: 'Estornada' },
    { value: StatusTransacao.AJUSTE, label: 'Ajuste' }
  ];

  constructor(private transacaoService: TransacaoService) { }

  ngOnInit(): void {
    this.carregarTransacoes();
  }

  carregarTransacoes(): void {
    this.loading = true;
    this.error = null;

    this.transacaoService.listarTransacoes(this.pageRequest, this.filtros).subscribe({
      next: (response: SuccessResponse<PageResponseDTO<TransacaoResponseDTO>>) => {
        this.transacoes = response.data.content;
        this.totalElements = response.data.totalElements;
        this.totalPages = response.data.totalPages;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erro ao carregar transações: ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  aplicarFiltros(): void {
    this.pageRequest.pagina = 1; // Reset para primeira página
    this.carregarTransacoes();
  }

  limparFiltros(): void {
    this.filtros = {
      usuarioId: undefined,
      cartaoId: undefined,
      status: undefined,
      dataInicio: undefined,
      dataFim: undefined
    };
    this.aplicarFiltros();
  }

  mudarPagina(pagina: number): void {
    this.pageRequest.pagina = pagina;
    this.carregarTransacoes();
  }

  ordenar(campo: string): void {
    if (this.pageRequest.ordenacao === campo) {
      this.pageRequest.direcao = this.pageRequest.direcao === 'ASC' ? 'DESC' : 'ASC';
    } else {
      this.pageRequest.ordenacao = campo;
      this.pageRequest.direcao = 'ASC';
    }
    this.carregarTransacoes();
  }

  criarTransacao(): void {
    // Validações básicas
    if (!this.novaTransacao.cartaoId || this.novaTransacao.cartaoId <= 0) {
      this.error = 'ID do cartão deve ser maior que zero';
      return;
    }
    
    if (!this.novaTransacao.usuarioId || this.novaTransacao.usuarioId <= 0) {
      this.error = 'ID do usuário deve ser maior que zero';
      return;
    }
    
    if (!this.novaTransacao.valor || this.novaTransacao.valor <= 0) {
      this.error = 'Valor deve ser maior que zero';
      return;
    }

    // Garantir que dataEvento está no formato correto
    if (!this.novaTransacao.dataEvento) {
      this.novaTransacao.dataEvento = new Date().toISOString().slice(0, 19);
    } else {
      // Converter para formato yyyy-MM-ddTHH:mm:ss
      const date = new Date(this.novaTransacao.dataEvento);
      this.novaTransacao.dataEvento = date.toISOString().slice(0, 19);
    }

    this.submitting = true;
    this.error = null;

    this.transacaoService.criarTransacao(this.novaTransacao).subscribe({
      next: (response: SuccessResponse<TransacaoResponseDTO>) => {
        this.submitting = false;
        this.showForm = false;
        this.resetarFormulario();
        this.carregarTransacoes();
        // Criar notificação sem mostrar localhost:4200
        this.mostrarNotificacao('Transação criada com sucesso!', 'success');
      },
      error: (err) => {
        let errorMessage = 'Erro ao criar transação';
        if (err.error?.message) {
          errorMessage += ': ' + err.error.message;
        } else if (err.message) {
          errorMessage += ': ' + err.message;
        } else if (err.status === 404) {
          errorMessage += ': Usuário ou cartão não encontrado';
        } else if (err.status === 400) {
          errorMessage += ': Dados inválidos';
        }
        this.error = errorMessage;
        this.submitting = false;
      }
    });
  }

  estornarTransacao(transacao: TransacaoResponseDTO): void {
    this.mostrarConfirmacao(
      `Deseja estornar a transação ${transacao.id}?`,
      () => this.executarEstornoTransacao(transacao)
    );
  }

  private executarEstornoTransacao(transacao: TransacaoResponseDTO): void {
    this.transacaoService.estornarTransacao(transacao.id).subscribe({
      next: (response: SuccessResponse<TransacaoResponseDTO>) => {
        this.carregarTransacoes();
        this.mostrarNotificacao('Transação estornada com sucesso!', 'success');
      },
      error: (err) => {
        this.error = 'Erro ao estornar transação: ' + (err.error?.message || err.message);
      }
    });
  }

  resetarFormulario(): void {
    this.novaTransacao = {
      cartaoId: 1,
      usuarioId: 4,
      valor: 0,
      moeda: 'BRL',
      mcc: '',
      categoria: '',
      parceiroId: undefined,
      dataEvento: this.getLocalDateTimeString(),
      autorizacao: ''
    };
  }

  private getLocalDateTimeString(): string {
    // Obtém a data/hora atual no timezone local do usuário
    const now = new Date();
    // Converte para o formato ISO mas mantém o timezone local
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');
    
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
  }

  formatarValor(valor: number): string {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(valor);
  }

  formatarData(data: string): string {
    return new Date(data).toLocaleString('pt-BR');
  }

  obterCorStatus(status: StatusTransacao): string {
    const cores: { [key in StatusTransacao]: string } = {
      [StatusTransacao.APROVADA]: '#28a745',
      [StatusTransacao.NEGADA]: '#dc3545',
      [StatusTransacao.ESTORNADA]: '#ffc107',
      [StatusTransacao.AJUSTE]: '#17a2b8'
    };
    return cores[status] || '#6c757d';
  }

  podeEstornar(transacao: TransacaoResponseDTO): boolean {
    return transacao.status === StatusTransacao.APROVADA;
  }

  get paginas(): number[] {
    const paginas: number[] = [];
    const inicio = Math.max(1, this.pageRequest.pagina - 2);
    const fim = Math.min(this.totalPages, this.pageRequest.pagina + 2);
    
    for (let i = inicio; i <= fim; i++) {
      paginas.push(i);
    }
    
    return paginas;
  }

  mostrarNotificacao(mensagem: string, tipo: 'success' | 'error'): void {
    this.notificacao = { mensagem, tipo };
    // Auto-remover após 3 segundos
    setTimeout(() => {
      this.notificacao = null;
    }, 3000);
  }

  fecharNotificacao(): void {
    this.notificacao = null;
  }

  mostrarConfirmacao(mensagem: string, acao: () => void): void {
    this.modalConfirmacao = { mensagem, acao };
  }

  confirmarAcao(): void {
    if (this.modalConfirmacao) {
      this.modalConfirmacao.acao();
      this.modalConfirmacao = null;
    }
  }

  cancelarAcao(): void {
    this.modalConfirmacao = null;
  }
}
