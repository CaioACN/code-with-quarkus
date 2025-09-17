export interface RecompensaRequestDTO {
  tipo: string; // Mudado para string para compatibilidade com backend
  descricao: string;
  custoPontos: number;
  estoque: number;
  parceiroId?: number | null;
  ativo: boolean;
  detalhes?: string;
  imagemUrl?: string;
  validadeRecompensa?: string;
}

export interface RecompensaResponseDTO {
  id: number;
  tipo: string; // Mudado para string para compatibilidade com backend
  descricao: string;
  custoPontos: number;
  estoque: number | null;
  parceiroId?: number;
  ativo: boolean;
  detalhes?: string;
  imagemUrl?: string;
  validadeRecompensa?: string;
  dataCriacao: string;
  dataAtualizacao: string;
  disponivelParaResgate: boolean;
  statusEstoque: StatusEstoque;
  diasParaVencer?: number;
}

export enum TipoRecompensa {
  MILHAS = 'MILHAS',
  GIFT = 'GIFT',
  CASHBACK = 'CASHBACK',
  PRODUTO = 'PRODUTO'
}

export enum StatusEstoque {
  DISPONIVEL = 'DISPONIVEL',
  BAIXO_ESTOQUE = 'BAIXO_ESTOQUE',
  ESGOTADO = 'ESGOTADO',
  INDISPONIVEL = 'INDISPONIVEL'
}

export interface ResgateRequestDTO {
  usuarioId: number;
  recompensaId: number;
  cartaoId: number;
  observacao?: string;
}

export interface ResgateResponseDTO {
  id: number;
  usuarioId: number;
  cartaoId: number;
  recompensaId: number;
  pontosUtilizados: number;
  status: StatusResgate;
  observacoes?: string;
  criadoEm: string;
  aprovadoEm?: string;
  concluidoEm?: string;
  negadoEm?: string;
  recompensa?: RecompensaResponseDTO;
}

export enum StatusResgate {
  PENDENTE = 'PENDENTE',
  APROVADO = 'APROVADO',
  CONCLUIDO = 'CONCLUIDO',
  NEGADO = 'NEGADO',
  CANCELADO = 'CANCELADO'
}
