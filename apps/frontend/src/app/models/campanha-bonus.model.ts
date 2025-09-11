export interface CampanhaBonus {
  id?: number;
  nome: string;
  descricao?: string;
  multiplicadorExtra: number;
  vigenciaIni: string; // ISO date string
  vigenciaFim?: string; // ISO date string (opcional)
  segmento?: string;
  prioridade: number;
  teto?: number;
}

export interface CampanhaBonusRequest {
  nome: string;
  descricao?: string;
  multiplicadorExtra: number;
  vigenciaIni: string;
  vigenciaFim?: string;
  segmento?: string;
  prioridade: number;
  teto?: number;
}

export interface CampanhaBonusResponse {
  id: number;
  nome: string;
  descricao?: string;
  multiplicadorExtra: number;
  vigenciaIni: string;
  vigenciaFim?: string;
  segmento?: string;
  prioridade: number;
  teto?: number;
  statusVigencia?: string;
  estaVigente?: boolean;
  estaExpirada?: boolean;
  estaProximaExpiracao?: boolean;
  criadoEm?: string;
  atualizadoEm?: string;
}

export interface CampanhaBonusListResponse {
  message: string;
  data: CampanhaBonusResponse[];
  status: number;
  timestamp: string;
  path?: string;
  success?: boolean;
  formattedTimestamp: string;
  statusText: string;
}

export interface CampanhaBonusCreateResponse {
  message: string;
  data: CampanhaBonusResponse;
  status: number;
  timestamp: string;
  path?: string;
  success?: boolean;
  formattedTimestamp: string;
  statusText: string;
}
