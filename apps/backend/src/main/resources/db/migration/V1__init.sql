-- =====================================================
-- V1: CRIAÇÃO DO SCHEMA E TABELAS DO SISTEMA DE PONTOS
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- =====================================================
-- 1. CRIAÇÃO DO SCHEMA LOYALTY
-- =====================================================
CREATE SCHEMA IF NOT EXISTS loyalty;

-- Configurações iniciais
SET search_path TO loyalty, public;

-- =====================================================
-- 2. TABELAS DO SISTEMA DE LOYALTY
-- =====================================================

-- 2.1 TABELA USUARIO
CREATE TABLE IF NOT EXISTS loyalty.usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    data_cadastro DATE NOT NULL DEFAULT CURRENT_DATE
);

-- 2.2 TABELA CARTAO
CREATE TABLE IF NOT EXISTS loyalty.cartao (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(19) NOT NULL UNIQUE,
    nome_impresso VARCHAR(100) NOT NULL,
    validade DATE NOT NULL,
    limite NUMERIC(12,2) NOT NULL,
    id_usuario BIGINT NOT NULL,
    
    CONSTRAINT fk_cartao_usuario FOREIGN KEY (id_usuario) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT chk_cartao_limite_positivo CHECK (limite >= 0)
);

-- 2.3 TABELA TRANSACAO
CREATE TABLE IF NOT EXISTS loyalty.transacao (
    id BIGSERIAL PRIMARY KEY,
    cartao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    valor NUMERIC(12,2) NOT NULL,
    moeda VARCHAR(3) NOT NULL,
    mcc VARCHAR(4),
    categoria VARCHAR(60),
    parceiro_id BIGINT,
    status VARCHAR(20) NOT NULL,
    autorizacao VARCHAR(100),
    data_evento TIMESTAMP NOT NULL,
    processado_em TIMESTAMP,
    pontos_gerados INTEGER,
    
    CONSTRAINT fk_transacao_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT fk_transacao_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT chk_transacao_valor_positivo CHECK (valor >= 0),
    CONSTRAINT chk_transacao_moeda CHECK (LENGTH(moeda) = 3),
    CONSTRAINT chk_transacao_mcc CHECK (mcc IS NULL OR LENGTH(mcc) = 4),
    CONSTRAINT chk_transacao_status CHECK (status IN ('APROVADA', 'NEGADA', 'ESTORNADA', 'AJUSTE'))
);

-- 2.4 TABELA REGRA_CONVERSAO
CREATE TABLE IF NOT EXISTS loyalty.regra_conversao (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    multiplicador NUMERIC(8,4) NOT NULL,
    mcc_regex VARCHAR(100),
    categoria VARCHAR(100),
    parceiro_id BIGINT,
    vigencia_ini TIMESTAMP NOT NULL,
    vigencia_fim TIMESTAMP,
    prioridade INTEGER NOT NULL,
    teto_mensal BIGINT,
    ativo BOOLEAN NOT NULL DEFAULT true,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    
    CONSTRAINT chk_regra_multiplicador_positivo CHECK (multiplicador >= 0),
    CONSTRAINT chk_regra_prioridade_positiva CHECK (prioridade >= 0),
    CONSTRAINT chk_regra_teto_positivo CHECK (teto_mensal IS NULL OR teto_mensal > 0)
);

-- 2.5 TABELA CAMPANHA_BONUS
CREATE TABLE IF NOT EXISTS loyalty.campanha_bonus (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    multiplicador_extra NUMERIC(8,4) NOT NULL DEFAULT 0.0000,
    vigencia_ini DATE NOT NULL,
    vigencia_fim DATE,
    segmento VARCHAR(60),
    prioridade INTEGER NOT NULL DEFAULT 0,
    teto BIGINT,
    
    CONSTRAINT chk_campanha_multiplicador_positivo CHECK (multiplicador_extra >= 0),
    CONSTRAINT chk_campanha_prioridade_positiva CHECK (prioridade >= 0),
    CONSTRAINT chk_campanha_teto_positivo CHECK (teto IS NULL OR teto > 0)
);

-- 2.6 TABELA MOVIMENTO_PONTOS
CREATE TABLE IF NOT EXISTS loyalty.movimento_pontos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    cartao_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    pontos INTEGER NOT NULL,
    ref_transacao_id BIGINT,
    transacao_id BIGINT,
    observacao VARCHAR(500),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    job_id VARCHAR(100),
    regra_aplicada VARCHAR(200),
    campanha_aplicada VARCHAR(200),
    
    CONSTRAINT fk_movimento_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_movimento_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT fk_movimento_transacao FOREIGN KEY (transacao_id) REFERENCES loyalty.transacao(id) ON DELETE SET NULL,
    CONSTRAINT chk_movimento_tipo CHECK (tipo IN ('ACUMULO', 'EXPIRACAO', 'RESGATE', 'ESTORNO', 'AJUSTE')),
    CONSTRAINT chk_movimento_pontos_nao_zero CHECK (pontos != 0)
);

-- 2.7 TABELA SALDO_PONTOS
CREATE TABLE IF NOT EXISTS loyalty.saldo_pontos (
    usuario_id BIGINT NOT NULL,
    cartao_id BIGINT NOT NULL,
    saldo BIGINT NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pontos_expirando_30_dias BIGINT DEFAULT 0,
    pontos_expirando_60_dias BIGINT DEFAULT 0,
    pontos_expirando_90_dias BIGINT DEFAULT 0,
    
    PRIMARY KEY (usuario_id, cartao_id),
    
    CONSTRAINT fk_saldo_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_saldo_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT chk_saldo_nao_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_pontos_expirando_30_nao_negativo CHECK (pontos_expirando_30_dias >= 0),
    CONSTRAINT chk_pontos_expirando_60_nao_negativo CHECK (pontos_expirando_60_dias >= 0),
    CONSTRAINT chk_pontos_expirando_90_nao_negativo CHECK (pontos_expirando_90_dias >= 0)
);

-- 2.8 TABELA RECOMPENSA
CREATE TABLE IF NOT EXISTS loyalty.recompensa (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    custo_pontos BIGINT NOT NULL,
    estoque BIGINT NOT NULL,
    parceiro_id BIGINT,
    ativo BOOLEAN NOT NULL DEFAULT true,
    detalhes VARCHAR(500),
    imagem_url VARCHAR(500),
    validade_recompensa TIMESTAMP,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP,
    
    CONSTRAINT chk_recompensa_custo_positivo CHECK (custo_pontos > 0),
    CONSTRAINT chk_recompensa_estoque_nao_negativo CHECK (estoque >= 0),
    CONSTRAINT chk_recompensa_tipo CHECK (tipo IN ('MILHAS', 'GIFT', 'CASHBACK', 'PRODUTO'))
);

-- 2.9 TABELA RESGATE
CREATE TABLE IF NOT EXISTS loyalty.resgate (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    cartao_id BIGINT NOT NULL,
    recompensa_id BIGINT NOT NULL,
    pontos_utilizados BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aprovado_em TIMESTAMP,
    concluido_em TIMESTAMP,
    negado_em TIMESTAMP,
    observacao VARCHAR(500),
    motivo_negacao VARCHAR(100),
    codigo_rastreio VARCHAR(100),
    parceiro_processador VARCHAR(100),
    
    CONSTRAINT fk_resgate_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_resgate_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT fk_resgate_recompensa FOREIGN KEY (recompensa_id) REFERENCES loyalty.recompensa(id) ON DELETE CASCADE,
    CONSTRAINT chk_resgate_pontos_positivos CHECK (pontos_utilizados > 0),
    CONSTRAINT chk_resgate_status CHECK (status IN ('PENDENTE', 'APROVADO', 'CONCLUIDO', 'NEGADO', 'CANCELADO'))
);

-- 2.10 TABELA NOTIFICACAO
CREATE TABLE IF NOT EXISTS loyalty.notificacao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    cartao_id BIGINT,
    transacao_id BIGINT,
    resgate_id BIGINT,
    movimento_id BIGINT,
    canal VARCHAR(16) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AGENDADA',
    titulo VARCHAR(200),
    mensagem VARCHAR(4000),
    destino VARCHAR(320) NOT NULL,
    provider VARCHAR(60),
    provider_message_id VARCHAR(120),
    erro_mensagem VARCHAR(180),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    agendado_para TIMESTAMP,
    enviado_em TIMESTAMP,
    tentativas INTEGER NOT NULL DEFAULT 0,
    ultima_tentativa_em TIMESTAMP,
    proxima_tentativa_em TIMESTAMP,
    correlation_id VARCHAR(120),
    tenant_id VARCHAR(60),
    template VARCHAR(120),
    metadata_json TEXT,
    
    CONSTRAINT fk_notificacao_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacao_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacao_transacao FOREIGN KEY (transacao_id) REFERENCES loyalty.transacao(id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacao_resgate FOREIGN KEY (resgate_id) REFERENCES loyalty.resgate(id) ON DELETE CASCADE,
    CONSTRAINT fk_notificacao_movimento FOREIGN KEY (movimento_id) REFERENCES loyalty.movimento_pontos(id) ON DELETE CASCADE,
    CONSTRAINT chk_notificacao_canal CHECK (canal IN ('EMAIL', 'PUSH', 'SMS', 'WEBHOOK')),
    CONSTRAINT chk_notificacao_tipo CHECK (tipo IN ('ACUMULO', 'EXPIRACAO', 'RESGATE', 'SISTEMA', 'AJUSTE')),
    CONSTRAINT chk_notificacao_status CHECK (status IN ('AGENDADA', 'ENFILEIRADA', 'RETENTANDO', 'ENVIADA', 'FALHA', 'CANCELADA')),
    CONSTRAINT chk_notificacao_tentativas_nao_negativas CHECK (tentativas >= 0)
);

-- =====================================================
-- 3. ÍNDICES PARA PERFORMANCE
-- =====================================================

-- Índices para transacao
CREATE INDEX IF NOT EXISTS idx_transacao_usuario_data ON loyalty.transacao(usuario_id, data_evento);
CREATE INDEX IF NOT EXISTS idx_transacao_cartao_data ON loyalty.transacao(cartao_id, data_evento);
CREATE INDEX IF NOT EXISTS idx_transacao_status ON loyalty.transacao(status);

-- Índices para movimento_pontos
CREATE INDEX IF NOT EXISTS idx_movimento_usuario_criado ON loyalty.movimento_pontos(usuario_id, criado_em);
CREATE INDEX IF NOT EXISTS idx_movimento_cartao_criado ON loyalty.movimento_pontos(cartao_id, criado_em);
CREATE INDEX IF NOT EXISTS idx_movimento_tipo ON loyalty.movimento_pontos(tipo);

-- Índices para saldo_pontos
CREATE INDEX IF NOT EXISTS idx_saldo_usuario ON loyalty.saldo_pontos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_saldo_cartao ON loyalty.saldo_pontos(cartao_id);

-- Índices para regra_conversao
CREATE INDEX IF NOT EXISTS idx_regra_vigencia ON loyalty.regra_conversao(vigencia_ini, vigencia_fim);
CREATE INDEX IF NOT EXISTS idx_regra_prioridade ON loyalty.regra_conversao(prioridade DESC);
CREATE INDEX IF NOT EXISTS idx_regra_ativo ON loyalty.regra_conversao(ativo) WHERE ativo = true;

-- Índices para recompensa
CREATE INDEX IF NOT EXISTS idx_recompensa_ativo ON loyalty.recompensa(ativo) WHERE ativo = true;
CREATE INDEX IF NOT EXISTS idx_recompensa_tipo ON loyalty.recompensa(tipo);

-- Índices para resgate
CREATE INDEX IF NOT EXISTS idx_resgate_usuario ON loyalty.resgate(usuario_id);
CREATE INDEX IF NOT EXISTS idx_resgate_status ON loyalty.resgate(status);

-- Índices para notificacao
CREATE INDEX IF NOT EXISTS idx_notificacao_usuario ON loyalty.notificacao(usuario_id);
CREATE INDEX IF NOT EXISTS idx_notificacao_status ON loyalty.notificacao(status);

-- =====================================================
-- 4. DADOS INICIAIS (SEEDS) - REMOVIDOS TEMPORARIAMENTE
-- =====================================================
-- Os dados iniciais serão inseridos após a confirmação de que as tabelas foram criadas corretamente