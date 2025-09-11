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
    cartaoId: 0,
    usuarioId: 0,
    valor: 0,
    moeda: 'BRL',
    mcc: '',
    categoria: '',
    parceiroId: undefined,
    dataEvento: new Date().toISOString().slice(0, 16),
    autorizacao: ''
  };
  showForm = false;
  submitting = false;

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
    this.submitting = true;
    this.error = null;

    this.transacaoService.criarTransacao(this.novaTransacao).subscribe({
      next: (response: SuccessResponse<TransacaoResponseDTO>) => {
        this.submitting = false;
        this.showForm = false;
        this.resetarFormulario();
        this.carregarTransacoes();
        alert('Transação criada com sucesso!');
      },
      error: (err) => {
        this.error = 'Erro ao criar transação: ' + (err.error?.message || err.message);
        this.submitting = false;
      }
    });
  }

  estornarTransacao(transacao: TransacaoResponseDTO): void {
    if (!confirm(`Deseja estornar a transação ${transacao.id}?`)) {
      return;
    }

    this.transacaoService.estornarTransacao(transacao.id).subscribe({
      next: (response: SuccessResponse<TransacaoResponseDTO>) => {
        this.carregarTransacoes();
        alert('Transação estornada com sucesso!');
      },
      error: (err) => {
        this.error = 'Erro ao estornar transação: ' + (err.error?.message || err.message);
      }
    });
  }

  resetarFormulario(): void {
    this.novaTransacao = {
      cartaoId: 0,
      usuarioId: 0,
      valor: 0,
      moeda: 'BRL',
      mcc: '',
      categoria: '',
      parceiroId: undefined,
      dataEvento: new Date().toISOString().slice(0, 16),
      autorizacao: ''
    };
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
}
