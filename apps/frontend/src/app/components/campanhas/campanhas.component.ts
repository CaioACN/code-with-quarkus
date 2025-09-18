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
    this.campanhaSelecionada = campanha;
    this.formulario = {
      nome: campanha.nome,
      descricao: campanha.descricao || '',
      multiplicadorExtra: campanha.multiplicadorExtra,
      vigenciaIni: this.converterDataDeISO(campanha.vigenciaIni),
      vigenciaFim: campanha.vigenciaFim ? this.converterDataDeISO(campanha.vigenciaFim) : '',
      segmento: campanha.segmento || '',
      prioridade: campanha.prioridade,
      teto: campanha.teto
    };
    this.modoEdicao = true;
    this.erro = null;
    this.sucesso = null;
  }

  salvarCampanha(): void {
    // Converter datas para formato ISO antes da validação
    const formularioParaValidacao = { ...this.formulario };
    if (formularioParaValidacao.vigenciaIni) {
      formularioParaValidacao.vigenciaIni = this.converterDataParaISO(formularioParaValidacao.vigenciaIni);
    }
    if (formularioParaValidacao.vigenciaFim) {
      formularioParaValidacao.vigenciaFim = this.converterDataParaISO(formularioParaValidacao.vigenciaFim);
    }
    
    // Validar formulário
    const erros = this.campanhaService.validarCampanha(formularioParaValidacao);
    if (erros.length > 0) {
      this.erro = erros.join(', ');
      return;
    }

    this.carregando = true;
    this.erro = null;

    const operacao = this.campanhaSelecionada ? 
      this.campanhaService.atualizarCampanha(this.campanhaSelecionada.id, formularioParaValidacao) :
      this.campanhaService.criarCampanha(formularioParaValidacao);

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
    if (!data) return '';
    // Se a data já está no formato DD-MM-YYYY, retorna como está
    if (data.includes('-') && data.split('-').length === 3 && data.split('-')[0].length === 2) {
      return data;
    }
    // Se está no formato ISO, converte para DD-MM-YYYY
    const date = new Date(data);
    const dia = date.getDate().toString().padStart(2, '0');
    const mes = (date.getMonth() + 1).toString().padStart(2, '0');
    const ano = date.getFullYear();
    return `${dia}-${mes}-${ano}`;
  }

  obterDataHoje(): string {
    const hoje = new Date();
    const dia = hoje.getDate().toString().padStart(2, '0');
    const mes = (hoje.getMonth() + 1).toString().padStart(2, '0');
    const ano = hoje.getFullYear();
    return `${dia}-${mes}-${ano}`;
  }

  formatarDataInput(event: any, campo: string): void {
    let valor = event.target.value.replace(/\D/g, ''); // Remove caracteres não numéricos
    
    if (valor.length >= 2) {
      valor = valor.substring(0, 2) + '-' + valor.substring(2);
    }
    if (valor.length >= 5) {
      valor = valor.substring(0, 5) + '-' + valor.substring(5, 9);
    }
    
    event.target.value = valor;
    
    // Atualiza o modelo
    if (campo === 'vigenciaIni') {
      this.formulario.vigenciaIni = valor;
    } else if (campo === 'vigenciaFim') {
      this.formulario.vigenciaFim = valor;
    }
  }

  converterDataParaISO(dataDDMMYYYY: string): string {
    if (!dataDDMMYYYY || dataDDMMYYYY.length !== 10) return '';
    const partes = dataDDMMYYYY.split('-');
    if (partes.length !== 3) return '';
    const [dia, mes, ano] = partes;
    return `${ano}-${mes.padStart(2, '0')}-${dia.padStart(2, '0')}`;
  }

  converterDataDeISO(dataISO: string): string {
    if (!dataISO) return '';
    const date = new Date(dataISO);
    const dia = date.getDate().toString().padStart(2, '0');
    const mes = (date.getMonth() + 1).toString().padStart(2, '0');
    const ano = date.getFullYear();
    return `${dia}-${mes}-${ano}`;
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
