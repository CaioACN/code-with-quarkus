export interface SaldoUsuarioDTO {
  usuarioId: number;
  saldos: SaldoCartao[];
  saldoTotal: number;
  totalPontosExpirando: number;
}

export interface SaldoCartao {
  cartaoId: number;
  saldo: number;
  atualizadoEm: string;
  pontosExpirando30Dias: number;
  pontosExpirando60Dias: number;
  pontosExpirando90Dias: number;
  statusSaldo: string;
}

export interface ExtratoPontosDTO {
  movimentos: MovimentoPontos[];
  saldoInicial: number;
  saldoFinal: number;
  totalCreditos: number;
  totalDebitos: number;
  periodo: {
    inicio: string;
    fim: string;
  };
}

export interface MovimentoPontos {
  id: number;
  usuarioId: number;
  cartaoId: number;
  tipo: TipoMovimento;
  pontos: number;
  refTransacaoId?: number;
  observacao?: string;
  criadoEm: string;
  jobId?: string;
  regraAplicada?: string;
  campanhaAplicada?: string;
  descricaoTipo?: string;
}

export enum TipoMovimento {
  ACUMULO = 'ACUMULO',
  EXPIRACAO = 'EXPIRACAO',
  RESGATE = 'RESGATE',
  ESTORNO = 'ESTORNO',
  AJUSTE = 'AJUSTE'
}

export interface NotificacaoResponseDTO {
  id: number;
  usuarioId: number;
  tipo: TipoNotificacao;
  canal: CanalNotificacao;
  titulo: string;
  mensagem: string;
  lida: boolean;
  criadoEm: string;
  lidoEm?: string;
  metadata?: any;
  status: StatusNotificacao;
  tentativasEnvio: number;
  proximoEnvio?: string;
  erroUltimoEnvio?: string;
}

export enum TipoNotificacao {
  ACUMULO = 'ACUMULO',
  EXPIRACAO = 'EXPIRACAO',
  RESGATE = 'RESGATE',
  SISTEMA = 'SISTEMA'
}

export enum CanalNotificacao {
  EMAIL = 'EMAIL',
  PUSH = 'PUSH',
  SMS = 'SMS',
  IN_APP = 'IN_APP'
}

export enum StatusNotificacao {
  AGENDADA = 'AGENDADA',
  ENFILEIRADA = 'ENFILEIRADA',
  ENVIADA = 'ENVIADA',
  FALHA = 'FALHA',
  CANCELADA = 'CANCELADA'
}
