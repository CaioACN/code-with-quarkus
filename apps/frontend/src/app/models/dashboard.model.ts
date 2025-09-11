export interface DashboardDTO {
  periodoIni: string;
  periodoFim: string;
  totalUsuarios: number;
  totalCartoes: number;
  totalTransacoes: number;
  pontosAcumulados: number;
  pontosExpirados: number;
  pontosResgatados: number;
  saldoTotal: number;
  resgatesPendentes: number;
  resgatesAprovados: number;
  resgatesConcluidos: number;
  resgatesNegados: number;
  resgatesCancelados: number;
  topRecompensas: TopRecompensa[];
  topUsuarios: TopUsuario[];
  evolucaoPontos: EvolucaoPontos[];
  evolucaoResgates: EvolucaoResgates[];
}

export interface TopRecompensa {
  recompensaId: number;
  descricao: string;
  quantidade: number;
  pontosUtilizados: number;
}

export interface TopUsuario {
  usuarioId: number;
  nome: string;
  pontosAcumulados: number;
  pontosResgatados: number;
}

export interface EvolucaoPontos {
  data: string;
  acumulados: number;
  expirados: number;
  resgatados: number;
  saldo: number;
}

export interface EvolucaoResgates {
  data: string;
  quantidade: number;
  pontosUtilizados: number;
}

export interface SuccessResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface ErrorResponse {
  success: boolean;
  error: string;
  message: string;
  timestamp: string;
  details?: any;
}
