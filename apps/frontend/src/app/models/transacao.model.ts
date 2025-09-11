export interface TransacaoRequestDTO {
  cartaoId: number;
  usuarioId: number;
  valor: number;
  moeda: string;
  mcc?: string;
  categoria?: string;
  parceiroId?: number;
  dataEvento: string;
  autorizacao?: string;
}

export interface TransacaoResponseDTO {
  id: number;
  cartaoId: number;
  usuarioId: number;
  valor: number;
  moeda: string;
  mcc?: string;
  categoria?: string;
  parceiroId?: number;
  status: StatusTransacao;
  dataEvento: string;
  criadoEm: string;
  processadoEm?: string;
  pontosGerados?: number;
  autorizacao?: string;
}

export enum StatusTransacao {
  APROVADA = 'APROVADA',
  NEGADA = 'NEGADA',
  ESTORNADA = 'ESTORNADA',
  AJUSTE = 'AJUSTE'
}

export interface ExtratoTransacaoDTO {
  id: number;
  cartaoId: number;
  valor: number;
  moeda: string;
  categoria?: string;
  status: StatusTransacao;
  dataEvento: string;
  pontosGerados?: number;
}

export interface PageRequestDTO {
  pagina: number;
  tamanho: number;
  ordenacao?: string;
  direcao?: 'ASC' | 'DESC';
}

export interface PageResponseDTO<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
