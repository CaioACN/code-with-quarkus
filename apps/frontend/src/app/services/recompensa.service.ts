import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { 
  RecompensaRequestDTO, 
  RecompensaResponseDTO,
  ResgateRequestDTO,
  ResgateResponseDTO,
  PageRequestDTO,
  PageResponseDTO,
  SuccessResponse 
} from '../models';
import { API_CONFIG } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class RecompensaService {
  private readonly baseUrl = `${API_CONFIG.baseUrl}/recompensas`;
  private readonly resgatesUrl = `${API_CONFIG.baseUrl}/resgates`;

  constructor(private http: HttpClient) { }

  /**
   * Lista recompensas disponíveis (método simplificado)
   */
  getRecompensas(ativo?: boolean): Observable<RecompensaResponseDTO[]> {
    // Se ativo=true, usar o endpoint específico de disponíveis
    if (ativo === true) {
      return this.http.get<SuccessResponse<RecompensaResponseDTO[]>>(`${this.baseUrl}/disponiveis`)
        .pipe(
          map(response => {
            console.log('Response do backend (getRecompensas - disponiveis):', response);
            console.log('Data do response (getRecompensas - disponiveis):', response.data);
            
            // Retornar os dados do backend ou array vazio se null
            return response.data || [];
          })
        );
    }
    
    // Para outros casos, usar o endpoint geral
    let params = new HttpParams();
    if (ativo !== undefined) {
      params = params.set('ativo', ativo.toString());
    }
    return this.http.get<SuccessResponse<RecompensaResponseDTO[]>>(this.baseUrl, { params })
      .pipe(
        map(response => {
          console.log('Response do backend (getRecompensas):', response);
          console.log('Data do response (getRecompensas):', response.data);
          
          // Retornar os dados do backend ou array vazio se null
          return response.data || [];
        })
      );
  }

  /**
   * Lista recompensas disponíveis
   */
  listarRecompensas(
    pageRequest: PageRequestDTO,
    filtros?: {
      ativo?: boolean;
      tipo?: string;
      parceiroId?: number;
      disponivel?: boolean;
    }
  ): Observable<SuccessResponse<PageResponseDTO<RecompensaResponseDTO>>> {
    let params = new HttpParams()
      .set('pagina', pageRequest.pagina.toString())
      .set('tamanho', pageRequest.tamanho.toString());

    if (filtros) {
      if (filtros.ativo !== undefined) params = params.set('ativo', filtros.ativo.toString());
      if (filtros.tipo) params = params.set('tipo', filtros.tipo);
      if (filtros.parceiroId) params = params.set('parceiroId', filtros.parceiroId.toString());
      if (filtros.disponivel !== undefined) params = params.set('disponivel', filtros.disponivel.toString());
    }

    return this.http.get<SuccessResponse<PageResponseDTO<RecompensaResponseDTO>>>(this.baseUrl, { params });
  }

  /**
   * Consulta uma recompensa específica
   */
  consultarRecompensa(id: number): Observable<SuccessResponse<RecompensaResponseDTO>> {
    return this.http.get<SuccessResponse<RecompensaResponseDTO>>(`${this.baseUrl}/${id}`);
  }

  /**
   * Cria uma nova recompensa (admin)
   */
  criarRecompensa(recompensa: RecompensaRequestDTO): Observable<RecompensaResponseDTO> {
    return this.http.post<SuccessResponse<RecompensaResponseDTO>>(this.baseUrl, recompensa)
      .pipe(
        map(response => {
          console.log('Response do backend:', response);
          console.log('Data do response:', response.data);
          
          // Retornar os dados do backend ou lançar erro se null
          if (!response.data || !response.data.id) {
            throw new Error('Backend retornou dados null. Verifique a persistência.');
          }
          return response.data;
        })
      );
  }

  /**
   * Atualiza uma recompensa (admin)
   */
  atualizarRecompensa(id: number, recompensa: Partial<RecompensaRequestDTO>): Observable<SuccessResponse<RecompensaResponseDTO>> {
    return this.http.put<SuccessResponse<RecompensaResponseDTO>>(`${this.baseUrl}/${id}`, recompensa);
  }

  /**
   * Solicita resgate de uma recompensa
   */
  solicitarResgate(resgate: ResgateRequestDTO): Observable<SuccessResponse<ResgateResponseDTO>> {
    return this.http.post<SuccessResponse<ResgateResponseDTO>>(this.resgatesUrl, resgate);
  }

  /**
   * Lista resgates do usuário
   */
  listarResgatesUsuario(
    usuarioId: number,
    pageRequest: PageRequestDTO,
    filtros?: {
      status?: string;
      cartaoId?: number;
    }
  ): Observable<SuccessResponse<PageResponseDTO<ResgateResponseDTO>>> {
    let params = new HttpParams()
      .set('pagina', pageRequest.pagina.toString())
      .set('tamanho', pageRequest.tamanho.toString());

    if (filtros) {
      if (filtros.status) params = params.set('status', filtros.status);
      if (filtros.cartaoId) params = params.set('cartaoId', filtros.cartaoId.toString());
    }

    return this.http.get<SuccessResponse<PageResponseDTO<ResgateResponseDTO>>>(
      `${this.resgatesUrl}/usuario/${usuarioId}`, 
      { params }
    );
  }

  /**
   * Consulta um resgate específico
   */
  consultarResgate(id: number): Observable<SuccessResponse<ResgateResponseDTO>> {
    return this.http.get<SuccessResponse<ResgateResponseDTO>>(`${this.resgatesUrl}/${id}`);
  }

  /**
   * Cancela um resgate
   */
  cancelarResgate(id: number, motivo?: string): Observable<SuccessResponse<ResgateResponseDTO>> {
    const body = motivo ? { motivo } : {};
    return this.http.post<SuccessResponse<ResgateResponseDTO>>(`${this.resgatesUrl}/${id}/cancelar`, body);
  }
}
