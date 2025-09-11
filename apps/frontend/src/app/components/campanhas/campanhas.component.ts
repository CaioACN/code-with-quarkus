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
    ativo: undefined as boolean | undefined,
    vigente: undefined as boolean | undefined,
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
      pagina: this.paginaAtual,
      tamanho: this.tamanhoPagina
    };

    // Remove filtros vazios
    Object.keys(filtros).forEach(key => {
      if (filtros[key as keyof typeof filtros] === undefined || filtros[key as keyof typeof filtros] === '') {
        delete filtros[key as keyof typeof filtros];
      }
    });

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
    if (confirm(`Tem certeza que deseja deletar a campanha "${campanha.nome}"?`)) {
      this.carregando = true;
      this.erro = null;

      this.campanhaService.deletarCampanha(campanha.id).subscribe({
        next: () => {
          this.sucesso = 'Campanha deletada com sucesso';
          this.carregarCampanhas();
          this.carregando = false;
        },
        error: (error) => {
          this.erro = 'Erro ao deletar campanha: ' + (error.error?.message || error.message);
          this.carregando = false;
        }
      });
    }
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
      ativo: undefined,
      vigente: undefined,
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
}
