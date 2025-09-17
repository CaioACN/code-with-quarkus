import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { 
  CampanhaBonus, 
  CampanhaBonusRequest, 
  CampanhaBonusResponse,
  CampanhaBonusListResponse 
} from '../../models/campanha-bonus.model';
import { CampanhaBonusService } from '../../services/campanha-bonus.service';

@Component({
  selector: 'app-campanhas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './campanhas.component.html',
  styleUrls: ['./campanhas.component.scss']
})
export class CampanhasComponent implements OnInit {
  campanhas: CampanhaBonusResponse[] = [];
  campanhaSelecionada: CampanhaBonusResponse | null = null;
  modoEdicao = false;
  carregando = false;
  erro: string | null = null;
  sucesso: string | null = null;
  
  // Notificações e Modal de Confirmação
  notificacao: { mensagem: string; tipo: 'success' | 'error' } | null = null;
  modalConfirmacao: { mensagem: string; acao: () => void } | null = null;

  // Formulário
  formulario: CampanhaBonusRequest = {
    nome: '',
    descricao: '',
    multiplicadorExtra: 0,
    vigenciaIni: '',
    vigenciaFim: '',
    segmento: '',
    prioridade: 0,
    teto: undefined
  };

  // Filtros
  filtros = {
    ativo: '' as any,
    vigente: '' as any,
    segmento: '',
    prioridade: undefined as number | undefined
  };

  // Paginação
  paginaAtual = 1;
  tamanhoPagina = 10;
  totalItens = 0;

  constructor(private campanhaService: CampanhaBonusService) {}

  ngOnInit(): void {
    this.carregarCampanhas();
  }

  carregarCampanhas(): void {
    this.carregando = true;
    this.erro = null;

    const filtros = {
      ...this.filtros,
      pagina: this.paginaAtual - 1, // Backend usa índice baseado em 0
      tamanho: this.tamanhoPagina
    };

    // Converter strings para boolean
    if (filtros.ativo === 'true') {
      filtros.ativo = true;
    } else if (filtros.ativo === 'false') {
      filtros.ativo = false;
    } else if (filtros.ativo === '') {
      delete filtros.ativo;
    }
    
    if (filtros.vigente === 'true') {
      filtros.vigente = true;
    } else if (filtros.vigente === 'false') {
      filtros.vigente = false;
    } else if (filtros.vigente === '') {
      delete filtros.vigente;
    }



    this.campanhaService.listarCampanhas(filtros).subscribe({
      next: (response) => {
        this.campanhas = response.data;
        this.totalItens = response.data.length; // Em uma implementação real, viria do backend
        this.carregando = false;
      },
      error: (error) => {
        this.erro = 'Erro ao carregar campanhas: ' + (error.error?.message || error.message);
        this.carregando = false;
      }
    });
  }

  novaCampanha(): void {
    this.modoEdicao = true;
    this.campanhaSelecionada = null;
    this.formulario = {
      nome: '',
      descricao: '',
      multiplicadorExtra: 0,
      vigenciaIni: '',
      vigenciaFim: '',
      segmento: '',
      prioridade: 0,
      teto: undefined
    };
    this.erro = null;
    this.sucesso = null;
  }

  editarCampanha(campanha: CampanhaBonusResponse): void {
    this.modoEdicao = true;
    this.campanhaSelecionada = campanha;
    this.formulario = {
      nome: campanha.nome,
      descricao: campanha.descricao || '',
      multiplicadorExtra: campanha.multiplicadorExtra,
      vigenciaIni: campanha.vigenciaIni,
      vigenciaFim: campanha.vigenciaFim || '',
      segmento: campanha.segmento || '',
      prioridade: campanha.prioridade,
      teto: campanha.teto
    };
    this.erro = null;
    this.sucesso = null;
  }

  salvarCampanha(): void {
    // Validar formulário
    const erros = this.campanhaService.validarCampanha(this.formulario);
    if (erros.length > 0) {
      this.erro = erros.join(', ');
      return;
    }

    this.carregando = true;
    this.erro = null;

    const operacao = this.campanhaSelecionada ? 
      this.campanhaService.atualizarCampanha(this.campanhaSelecionada.id, this.formulario) :
      this.campanhaService.criarCampanha(this.formulario);

    operacao.subscribe({
      next: (response) => {
        this.sucesso = response.message;
        this.modoEdicao = false;
        this.campanhaSelecionada = null;
        this.carregarCampanhas();
        this.carregando = false;
      },
      error: (error) => {
        this.erro = 'Erro ao salvar campanha: ' + (error.error?.message || error.message);
        this.carregando = false;
      }
    });
  }

  cancelarEdicao(): void {
    this.modoEdicao = false;
    this.campanhaSelecionada = null;
    this.erro = null;
    this.sucesso = null;
  }

  deletarCampanha(campanha: CampanhaBonusResponse): void {
    this.mostrarConfirmacao(
      `Tem certeza que deseja deletar a campanha "${campanha.nome}"?`,
      () => this.executarDelecaoCampanha(campanha)
    );
  }

  private executarDelecaoCampanha(campanha: CampanhaBonusResponse): void {
    this.carregando = true;
    this.erro = null;

    this.campanhaService.deletarCampanha(campanha.id).subscribe({
      next: () => {
        this.mostrarNotificacao('Campanha deletada com sucesso', 'success');
        this.carregarCampanhas();
        this.carregando = false;
      },
      error: (error) => {
        this.mostrarNotificacao('Erro ao deletar campanha', 'error');
        this.carregando = false;
      }
    });
  }

  ativarCampanha(campanha: CampanhaBonusResponse): void {
    this.carregando = true;
    this.erro = null;

    this.campanhaService.ativarCampanha(campanha.id).subscribe({
      next: (response) => {
        this.sucesso = response.message;
        this.carregarCampanhas();
        this.carregando = false;
      },
      error: (error) => {
        this.erro = 'Erro ao ativar campanha: ' + (error.error?.message || error.message);
        this.carregando = false;
      }
    });
  }

  desativarCampanha(campanha: CampanhaBonusResponse): void {
    this.carregando = true;
    this.erro = null;

    this.campanhaService.desativarCampanha(campanha.id).subscribe({
      next: (response) => {
        this.sucesso = response.message;
        this.carregarCampanhas();
        this.carregando = false;
      },
      error: (error) => {
        this.erro = 'Erro ao desativar campanha: ' + (error.error?.message || error.message);
        this.carregando = false;
      }
    });
  }

  aplicarFiltros(): void {
    this.paginaAtual = 1;
    this.carregarCampanhas();
  }

  limparFiltros(): void {
    this.filtros = {
      ativo: '' as any,
      vigente: '' as any,
      segmento: '',
      prioridade: undefined
    };
    this.paginaAtual = 1;
    this.carregarCampanhas();
  }

  formatarMultiplicador(multiplicador: number): string {
    return this.campanhaService.formatarMultiplicador(multiplicador);
  }

  formatarData(data: string): string {
    return new Date(data).toLocaleDateString('pt-BR');
  }

  obterStatusVigencia(campanha: CampanhaBonusResponse): {
    status: string;
    classe: string;
  } {
    const status = this.campanhaService.calcularStatusVigencia(
      campanha.vigenciaIni, 
      campanha.vigenciaFim
    );

    let classe = '';
    switch (status.status) {
      case 'VIGENTE':
        classe = 'status-vigente';
        break;
      case 'EXPIRADA':
        classe = 'status-expirada';
        break;
      case 'PROXIMA_EXPIRACAO':
        classe = 'status-proxima-expiracao';
        break;
      case 'AGUARDANDO_INICIO':
        classe = 'status-aguardando';
        break;
    }

    return { status: status.status, classe };
  }

  fecharAlerta(): void {
    this.erro = null;
    this.sucesso = null;
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
