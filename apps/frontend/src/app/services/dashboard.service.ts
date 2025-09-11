import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardDTO, SuccessResponse } from '../models';
import { API_CONFIG } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private readonly baseUrl = `${API_CONFIG.baseUrl}/admin`;

  constructor(private http: HttpClient) { }

  /**
   * Consulta o dashboard administrativo com métricas consolidadas
   */
  consultarDashboard(): Observable<SuccessResponse<DashboardDTO>> {
    return this.http.get<SuccessResponse<DashboardDTO>>(`${this.baseUrl}/dashboard`);
  }

  /**
   * Consulta estatísticas detalhadas do sistema
   */
  consultarEstatisticas(periodo?: string): Observable<SuccessResponse<any>> {
    let params = new HttpParams();
    if (periodo) {
      params = params.set('periodo', periodo);
    }
    
    return this.http.get<SuccessResponse<any>>(`${this.baseUrl}/estatisticas`, { params });
  }

  /**
   * Consulta métricas de performance do sistema
   */
  consultarMetricas(): Observable<SuccessResponse<any>> {
    return this.http.get<SuccessResponse<any>>(`${this.baseUrl}/metricas`);
  }

  /**
   * Consulta relatório de saúde do sistema
   */
  consultarSaudeSistema(): Observable<SuccessResponse<any>> {
    return this.http.get<SuccessResponse<any>>(`${this.baseUrl}/saude`);
  }
}
