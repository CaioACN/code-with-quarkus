import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  CampanhaBonus, 
  CampanhaBonusRequest, 
  CampanhaBonusResponse,
  CampanhaBonusListResponse,
  CampanhaBonusCreateResponse 
} from '../models/campanha-bonus.model';
import { API_CONFIG } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class CampanhaBonusService {
  private apiUrl = `${API_CONFIG.baseUrl}/campanhas-bonus`;

  constructor(private http: HttpClient) { }

  // Listar campanhas com filtros opcionais
  listarCampanhas(filtros?: {
    ativo?: boolean;
    vigente?: boolean;
    segmento?: string;
    prioridade?: number;
    pagina?: number;
    tamanho?: number;
  }): Observable<CampanhaBonusListResponse> {
    let params = new HttpParams();
    
    if (filtros) {
      if (filtros.ativo !== undefined) {
        params = params.set('ativo', filtros.ativo.toString());
      }
      if (filtros.vigente !== undefined) {
        params = params.set('vigente', filtros.vigente.toString());
      }
      if (filtros.segmento) {
        params = params.set('segmento', filtros.segmento);
      }
      if (filtros.prioridade !== undefined) {
        params = params.set('prioridade', filtros.prioridade.toString());
      }
      if (filtros.pagina !== undefined) {
        params = params.set('pagina', filtros.pagina.toString());
      }
      if (filtros.tamanho !== undefined) {
        params = params.set('tamanho', filtros.tamanho.toString());
      }
    }

    return this.http.get<CampanhaBonusListResponse>(this.apiUrl, { params });
  }

  // Buscar campanha por ID
  buscarCampanha(id: number): Observable<CampanhaBonusResponse> {
    return this.http.get<CampanhaBonusResponse>(`${this.apiUrl}/${id}`);
  }

  // Criar nova campanha
  criarCampanha(campanha: CampanhaBonusRequest): Observable<CampanhaBonusCreateResponse> {
    return this.http.post<CampanhaBonusCreateResponse>(this.apiUrl, campanha);
  }

  // Atualizar campanha
  atualizarCampanha(id: number, campanha: CampanhaBonusRequest): Observable<CampanhaBonusCreateResponse> {
    return this.http.put<CampanhaBonusCreateResponse>(`${this.apiUrl}/${id}`, campanha);
  }

  // Deletar campanha
  deletarCampanha(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // Ativar campanha
  ativarCampanha(id: number): Observable<CampanhaBonusCreateResponse> {
    return this.http.post<CampanhaBonusCreateResponse>(`${this.apiUrl}/${id}/ativar`, {});
  }

  // Desativar campanha
  desativarCampanha(id: number): Observable<CampanhaBonusCreateResponse> {
    return this.http.post<CampanhaBonusCreateResponse>(`${this.apiUrl}/${id}/desativar`, {});
  }

  // Listar campanhas vigentes
  listarCampanhasVigentes(): Observable<CampanhaBonusListResponse> {
    return this.listarCampanhas({ vigente: true });
  }

  // Listar campanhas por segmento
  listarCampanhasPorSegmento(segmento: string): Observable<CampanhaBonusListResponse> {
    return this.listarCampanhas({ segmento });
  }

  // Validar dados da campanha
  validarCampanha(campanha: CampanhaBonusRequest): string[] {
    const erros: string[] = [];

    if (!campanha.nome || campanha.nome.trim().length === 0) {
      erros.push('Nome é obrigatório');
    } else if (campanha.nome.length > 120) {
      erros.push('Nome deve ter no máximo 120 caracteres');
    }

    if (campanha.multiplicadorExtra < 0) {
      erros.push('Multiplicador extra deve ser maior ou igual a zero');
    }

    if (!campanha.vigenciaIni) {
      erros.push('Data de início é obrigatória');
    } else {
      // Validar formato da data (deve estar em formato ISO: YYYY-MM-DD)
      const regexDataISO = /^\d{4}-\d{2}-\d{2}$/;
      if (!regexDataISO.test(campanha.vigenciaIni)) {
        erros.push('Formato de data de início inválido');
      } else {
        // Usar comparação de strings de data para evitar problemas de timezone
        const dataIniStr = campanha.vigenciaIni.split('T')[0]; // Pegar apenas a parte da data
        const hojeStr = new Date().toISOString().split('T')[0];
        
        if (dataIniStr < hojeStr) {
          erros.push('Data de início deve ser hoje ou no futuro');
        }
      }
    }

    if (campanha.vigenciaFim) {
      // Validar formato da data de fim
      const regexDataISO = /^\d{4}-\d{2}-\d{2}$/;
      if (!regexDataISO.test(campanha.vigenciaFim)) {
        erros.push('Formato de data de fim inválido');
      } else {
        const dataIni = new Date(campanha.vigenciaIni);
        const dataFim = new Date(campanha.vigenciaFim);
        
        if (dataFim <= dataIni) {
          erros.push('Data de fim deve ser posterior à data de início');
        }
      }
    }

    if (campanha.segmento && campanha.segmento.length > 60) {
      erros.push('Segmento deve ter no máximo 60 caracteres');
    }

    if (campanha.prioridade < 0) {
      erros.push('Prioridade deve ser maior ou igual a zero');
    }

    if (campanha.teto !== undefined && campanha.teto <= 0) {
      erros.push('Teto deve ser maior que zero');
    }

    return erros;
  }

  // Formatar multiplicador para exibição
  formatarMultiplicador(multiplicador: number): string {
    return (multiplicador * 100).toFixed(2) + '%';
  }

  // Calcular status da vigência
  calcularStatusVigencia(vigenciaIni: string, vigenciaFim?: string): {
    status: string;
    estaVigente: boolean;
    estaExpirada: boolean;
    estaProximaExpiracao: boolean;
  } {
    const hoje = new Date();
    const dataIni = new Date(vigenciaIni);
    const dataFim = vigenciaFim ? new Date(vigenciaFim) : null;

    const estaVigente = hoje >= dataIni && (!dataFim || hoje <= dataFim);
    const estaExpirada = dataFim ? hoje > dataFim : false;
    const estaProximaExpiracao = dataFim ? 
      (hoje >= new Date(dataFim.getTime() - 7 * 24 * 60 * 60 * 1000) && !estaExpirada) : false;

    let status = 'AGUARDANDO_INICIO';
    if (estaExpirada) {
      status = 'EXPIRADA';
    } else if (estaProximaExpiracao) {
      status = 'PROXIMA_EXPIRACAO';
    } else if (estaVigente) {
      status = 'VIGENTE';
    }

    return {
      status,
      estaVigente,
      estaExpirada,
      estaProximaExpiracao
    };
  }
}
