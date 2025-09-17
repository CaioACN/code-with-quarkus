import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  TransacaoRequestDTO, 
  TransacaoResponseDTO, 
  ExtratoTransacaoDTO,
  PageRequestDTO,
  PageResponseDTO,
  SuccessResponse 
} from '../models';
import { API_CONFIG } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class TransacaoService {
  private readonly baseUrl = `${API_CONFIG.baseUrl}/transacoes`;

  constructor(private http: HttpClient) { }

  /**
   * Cria uma nova transação
   */
  criarTransacao(transacao: TransacaoRequestDTO): Observable<SuccessResponse<TransacaoResponseDTO>> {
    return this.http.post<SuccessResponse<TransacaoResponseDTO>>(this.baseUrl, transacao);
  }

  /**
   * Lista transações com paginação
   */
  listarTransacoes(
    pageRequest: PageRequestDTO,
    filtros?: {
      usuarioId?: number;
      cartaoId?: number;
      status?: string;
      dataInicio?: string;
      dataFim?: string;
    }
  ): Observable<SuccessResponse<PageResponseDTO<TransacaoResponseDTO>>> {
    let params = new HttpParams()
      .set('pagina', pageRequest.pagina.toString())
      .set('tamanho', pageRequest.tamanho.toString());

    if (pageRequest.ordenacao) {
      params = params.set('ordenacao', pageRequest.ordenacao);
    }
    if (pageRequest.direcao) {
      params = params.set('direcao', pageRequest.direcao);
    }

    if (filtros) {
      if (filtros.usuarioId) params = params.set('usuarioId', filtros.usuarioId.toString());
      if (filtros.cartaoId) params = params.set('cartaoId', filtros.cartaoId.toString());
      if (filtros.status && filtros.status.trim() !== '') params = params.set('status', filtros.status);
      if (filtros.dataInicio && filtros.dataInicio.trim() !== '') params = params.set('dataInicio', filtros.dataInicio);
      if (filtros.dataFim && filtros.dataFim.trim() !== '') params = params.set('dataFim', filtros.dataFim);
    }

    return this.http.get<SuccessResponse<PageResponseDTO<TransacaoResponseDTO>>>(this.baseUrl, { params });
  }

  /**
   * Consulta uma transação específica
   */
  consultarTransacao(id: number): Observable<SuccessResponse<TransacaoResponseDTO>> {
    return this.http.get<SuccessResponse<TransacaoResponseDTO>>(`${this.baseUrl}/${id}`);
  }

  /**
   * Estorna uma transação
   */
  estornarTransacao(id: number, motivo?: string): Observable<SuccessResponse<TransacaoResponseDTO>> {
    const body = motivo ? { motivo } : {};
    return this.http.post<SuccessResponse<TransacaoResponseDTO>>(`${this.baseUrl}/${id}/estorno`, body);
  }

  /**
   * Consulta extrato de transações do usuário
   */
  consultarExtratoUsuario(
    usuarioId: number,
    pageRequest: PageRequestDTO,
    filtros?: {
      cartaoId?: number;
      dataInicio?: string;
      dataFim?: string;
    }
  ): Observable<SuccessResponse<PageResponseDTO<ExtratoTransacaoDTO>>> {
    let params = new HttpParams()
      .set('pagina', pageRequest.pagina.toString())
      .set('tamanho', pageRequest.tamanho.toString());

    if (filtros) {
      if (filtros.cartaoId) params = params.set('cartaoId', filtros.cartaoId.toString());
      if (filtros.dataInicio && filtros.dataInicio.trim() !== '') params = params.set('dataInicio', filtros.dataInicio);
      if (filtros.dataFim && filtros.dataFim.trim() !== '') params = params.set('dataFim', filtros.dataFim);
    }

    return this.http.get<SuccessResponse<PageResponseDTO<ExtratoTransacaoDTO>>>(
      `${this.baseUrl}/usuario/${usuarioId}/extrato`, 
      { params }
    );
  }
}
