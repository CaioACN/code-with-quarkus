import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { 
  SaldoUsuarioDTO, 
  ExtratoPontosDTO,
  PageRequestDTO,
  PageResponseDTO,
  MovimentoPontos,
  NotificacaoResponseDTO,
  SuccessResponse 
} from '../models';
import { API_CONFIG } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class PontosService {
  private readonly baseUrl = `${API_CONFIG.baseUrl}/usuarios`;

  constructor(private http: HttpClient) { }

  /**
   * Consulta saldo de pontos do usuário (método simplificado)
   */
  getSaldoUsuario(usuarioId: number): Observable<SaldoUsuarioDTO> {
    return this.http.get<SaldoUsuarioDTO>(`${this.baseUrl}/${usuarioId}/pontos/saldo`);
  }

  /**
   * Consulta saldo de pontos do usuário
   */
  consultarSaldo(usuarioId: number): Observable<SuccessResponse<SaldoUsuarioDTO>> {
    return this.http.get<SuccessResponse<SaldoUsuarioDTO>>(`${this.baseUrl}/${usuarioId}/pontos/saldo`);
  }

  /**
   * Consulta extrato de pontos do usuário
   */
  consultarExtrato(
    usuarioId: number,
    pageRequest: PageRequestDTO,
    filtros?: {
      cartaoId?: number;
      tipo?: string;
      dataInicio?: string;
      dataFim?: string;
    }
  ): Observable<SuccessResponse<PageResponseDTO<MovimentoPontos>>> {
    let params = new HttpParams()
      .set('pagina', pageRequest.pagina.toString())
      .set('tamanho', pageRequest.tamanho.toString());

    if (filtros) {
      if (filtros.cartaoId) params = params.set('cartaoId', filtros.cartaoId.toString());
      if (filtros.tipo) params = params.set('tipo', filtros.tipo);
      if (filtros.dataInicio) params = params.set('dataInicio', filtros.dataInicio);
      if (filtros.dataFim) params = params.set('dataFim', filtros.dataFim);
    }

    return this.http.get<SuccessResponse<PageResponseDTO<MovimentoPontos>>>(
      `${this.baseUrl}/${usuarioId}/pontos/extrato`, 
      { params }
    );
  }

  /**
   * Consulta histórico detalhado de pontos
   */
  consultarHistorico(
    usuarioId: number,
    filtros?: {
      cartaoId?: number;
      dataInicio?: string;
      dataFim?: string;
    }
  ): Observable<SuccessResponse<ExtratoPontosDTO>> {
    let params = new HttpParams();

    if (filtros) {
      if (filtros.cartaoId) params = params.set('cartaoId', filtros.cartaoId.toString());
      if (filtros.dataInicio) params = params.set('dataInicio', filtros.dataInicio);
      if (filtros.dataFim) params = params.set('dataFim', filtros.dataFim);
    }

    return this.http.get<SuccessResponse<ExtratoPontosDTO>>(
      `${this.baseUrl}/${usuarioId}/pontos/historico`, 
      { params }
    );
  }

  /**
   * Lista notificações do usuário
   */
  listarNotificacoes(
    usuarioId: number,
    pageRequest: PageRequestDTO,
    filtros?: {
      tipo?: string;
      lida?: boolean;
    }
  ): Observable<SuccessResponse<PageResponseDTO<NotificacaoResponseDTO>>> {
    let params = new HttpParams()
      .set('pagina', pageRequest.pagina.toString())
      .set('tamanho', pageRequest.tamanho.toString());

    if (filtros) {
      if (filtros.tipo) params = params.set('tipo', filtros.tipo);
      if (filtros.lida !== undefined) params = params.set('lida', filtros.lida.toString());
    }

    return this.http.get<SuccessResponse<PageResponseDTO<NotificacaoResponseDTO>>>(
      `${API_CONFIG.baseUrl}/notificacoes/usuario/${usuarioId}`, 
      { params }
    );
  }

  /**
   * Marca notificação como lida
   */
  marcarNotificacaoComoLida(notificacaoId: number): Observable<SuccessResponse<any>> {
    return this.http.put<SuccessResponse<any>>(
      `${API_CONFIG.baseUrl}/notificacoes/${notificacaoId}/ler`, 
      {}
    );
  }
}
