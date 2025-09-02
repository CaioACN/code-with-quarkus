-- =====================================================
-- SCRIPT DE CRIAÇÃO/ATUALIZAÇÃO DE TODAS AS TABELAS
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- Configurações iniciais
SET search_path TO loyalty, public;

-- =====================================================
-- 1. CRIAÇÃO DO SCHEMA LOYALTY (se não existir)
-- =====================================================
CREATE SCHEMA IF NOT EXISTS loyalty;

-- =====================================================
-- 2. TABELAS DO CONTEXTO SOCIAL (EXISTENTES - BASELINE)
-- =====================================================
-- Estas tabelas já existem no banco, apenas documentamos aqui

-- Tabela users (contexto social)
-- CREATE TABLE IF NOT EXISTS public.users (
--     id BIGSERIAL PRIMARY KEY,
--     name VARCHAR(100) NOT NULL,
--     age INTEGER
-- );

-- Tabela posts (contexto social)  
-- CREATE TABLE IF NOT EXISTS public.posts (
--     id BIGSERIAL PRIMARY KEY,
--     post_text TEXT,
--     datetime TIMESTAMP,
--     user_id BIGINT REFERENCES public.users(id)
-- );

-- Tabela followers (contexto social)
-- CREATE TABLE IF NOT EXISTS public.followers (
--     id BIGSERIAL PRIMARY KEY,
--     user_id BIGINT REFERENCES public.users(id),
--     follower_id BIGINT REFERENCES public.users(id)
-- );

-- =====================================================
-- 3. TABELAS DO CONTEXTO LOYALTY (EXISTENTES + NOVAS)
-- =====================================================

-- 3.1 TABELA USUARIO (existente - pode precisar de alterações)
-- =====================================================
CREATE TABLE IF NOT EXISTS loyalty.usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    data_cadastro DATE NOT NULL DEFAULT CURRENT_DATE
);

-- Alterações na tabela usuario (se necessário)
DO $$
BEGIN
    -- Adicionar coluna se não existir
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_schema = 'loyalty' 
                   AND table_name = 'usuario' 
                   AND column_name = 'data_cadastro') THEN
        ALTER TABLE loyalty.usuario ADD COLUMN data_cadastro DATE DEFAULT CURRENT_DATE;
    END IF;
    
    -- Modificar coluna se necessário
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_schema = 'loyalty' 
               AND table_name = 'usuario' 
               AND column_name = 'data_cadastro' 
               AND data_type = 'timestamp without time zone') THEN
        ALTER TABLE loyalty.usuario ALTER COLUMN data_cadastro TYPE DATE;
    END IF;
END $$;

-- 3.2 TABELA CARTAO (existente - pode precisar de alterações)
-- =====================================================
CREATE TABLE IF NOT EXISTS loyalty.cartao (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(19) NOT NULL UNIQUE,
    nome_impresso VARCHAR(100) NOT NULL,
    validade DATE NOT NULL,
    limite NUMERIC(12,2) NOT NULL,
    id_usuario BIGINT NOT NULL
);

-- Alterações na tabela cartao (se necessário)
DO $$
BEGIN
    -- Adicionar constraint de FK se não existir
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE table_schema = 'loyalty' 
                   AND table_name = 'cartao' 
                   AND constraint_name = 'fk_cartao_usuario') THEN
        ALTER TABLE loyalty.cartao 
        ADD CONSTRAINT fk_cartao_usuario 
        FOREIGN KEY (id_usuario) REFERENCES loyalty.usuario(id) ON DELETE CASCADE;
    END IF;
    
    -- Adicionar constraint de limite se não existir
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE table_schema = 'loyalty' 
                   AND table_name = 'cartao' 
                   AND constraint_name = 'chk_cartao_limite_positivo') THEN
        ALTER TABLE loyalty.cartao 
        ADD CONSTRAINT chk_cartao_limite_positivo CHECK (limite >= 0);
    END IF;
END $$;

-- 3.3 TABELA TRANSACAO (nova)
-- =====================================================
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
    
    -- Constraints
    CONSTRAINT fk_transacao_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT fk_transacao_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT chk_transacao_valor_positivo CHECK (valor >= 0),
    CONSTRAINT chk_transacao_moeda CHECK (LENGTH(moeda) = 3),
    CONSTRAINT chk_transacao_mcc CHECK (mcc IS NULL OR LENGTH(mcc) = 4),
    CONSTRAINT chk_transacao_status CHECK (status IN ('APROVADA', 'NEGADA', 'ESTORNADA', 'AJUSTE'))
);

-- 3.4 TABELA REGRA_CONVERSAO (nova)
-- =====================================================
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
    
    -- Constraints
    CONSTRAINT chk_regra_multiplicador_positivo CHECK (multiplicador >= 0),
    CONSTRAINT chk_regra_prioridade_positiva CHECK (prioridade >= 0),
    CONSTRAINT chk_regra_teto_positivo CHECK (teto_mensal IS NULL OR teto_mensal > 0)
);

-- 3.5 TABELA CAMPANHA_BONUS (nova)
-- =====================================================
CREATE TABLE IF NOT EXISTS loyalty.campanha_bonus (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    multiplicador_extra NUMERIC(8,4) NOT NULL DEFAULT 0.0000,
    vigencia_ini DATE NOT NULL,
    vigencia_fim DATE,
    segmento VARCHAR(60),
    prioridade INTEGER NOT NULL DEFAULT 0,
    teto BIGINT,
    
    -- Constraints
    CONSTRAINT chk_campanha_multiplicador_positivo CHECK (multiplicador_extra >= 0),
    CONSTRAINT chk_campanha_prioridade_positiva CHECK (prioridade >= 0),
    CONSTRAINT chk_campanha_teto_positivo CHECK (teto IS NULL OR teto > 0)
);

-- 3.6 TABELA MOVIMENTO_PONTOS (nova)
-- =====================================================
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
    
    -- Constraints
    CONSTRAINT fk_movimento_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_movimento_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT fk_movimento_transacao FOREIGN KEY (transacao_id) REFERENCES loyalty.transacao(id) ON DELETE SET NULL,
    CONSTRAINT chk_movimento_tipo CHECK (tipo IN ('ACUMULO', 'EXPIRACAO', 'RESGATE', 'ESTORNO', 'AJUSTE')),
    CONSTRAINT chk_movimento_pontos_nao_zero CHECK (pontos != 0)
);

-- 3.7 TABELA SALDO_PONTOS (nova - chave composta)
-- =====================================================
CREATE TABLE IF NOT EXISTS loyalty.saldo_pontos (
    usuario_id BIGINT NOT NULL,
    cartao_id BIGINT NOT NULL,
    saldo BIGINT NOT NULL DEFAULT 0,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pontos_expirando_30_dias BIGINT DEFAULT 0,
    pontos_expirando_60_dias BIGINT DEFAULT 0,
    pontos_expirando_90_dias BIGINT DEFAULT 0,
    
    -- Chave primária composta
    PRIMARY KEY (usuario_id, cartao_id),
    
    -- Constraints
    CONSTRAINT fk_saldo_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_saldo_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT chk_saldo_nao_negativo CHECK (saldo >= 0),
    CONSTRAINT chk_pontos_expirando_30_nao_negativo CHECK (pontos_expirando_30_dias >= 0),
    CONSTRAINT chk_pontos_expirando_60_nao_negativo CHECK (pontos_expirando_60_dias >= 0),
    CONSTRAINT chk_pontos_expirando_90_nao_negativo CHECK (pontos_expirando_90_dias >= 0)
);

-- 3.8 TABELA RECOMPENSA (nova)
-- =====================================================
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
    
    -- Constraints
    CONSTRAINT chk_recompensa_custo_positivo CHECK (custo_pontos > 0),
    CONSTRAINT chk_recompensa_estoque_nao_negativo CHECK (estoque >= 0),
    CONSTRAINT chk_recompensa_tipo CHECK (tipo IN ('MILHAS', 'GIFT', 'CASHBACK', 'PRODUTO'))
);

-- 3.9 TABELA RESGATE (nova)
-- =====================================================
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
    
    -- Constraints
    CONSTRAINT fk_resgate_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_resgate_cartao FOREIGN KEY (cartao_id) REFERENCES loyalty.cartao(id) ON DELETE CASCADE,
    CONSTRAINT fk_resgate_recompensa FOREIGN KEY (recompensa_id) REFERENCES loyalty.recompensa(id) ON DELETE CASCADE,
    CONSTRAINT chk_resgate_pontos_positivos CHECK (pontos_utilizados > 0),
    CONSTRAINT chk_resgate_status CHECK (status IN ('PENDENTE', 'APROVADO', 'CONCLUIDO', 'NEGADO', 'CANCELADO'))
);

-- 3.10 TABELA NOTIFICACAO (nova)
-- =====================================================
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
    
    -- Constraints
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
-- 4. ÍNDICES PARA PERFORMANCE
-- =====================================================

-- Índices para transacao
CREATE INDEX IF NOT EXISTS idx_transacao_usuario_data ON loyalty.transacao(usuario_id, data_evento);
CREATE INDEX IF NOT EXISTS idx_transacao_cartao_data ON loyalty.transacao(cartao_id, data_evento);
CREATE INDEX IF NOT EXISTS idx_transacao_status ON loyalty.transacao(status);
CREATE INDEX IF NOT EXISTS idx_transacao_autorizacao ON loyalty.transacao(autorizacao) WHERE autorizacao IS NOT NULL;

-- Índices para movimento_pontos
CREATE INDEX IF NOT EXISTS idx_movimento_usuario_criado ON loyalty.movimento_pontos(usuario_id, criado_em);
CREATE INDEX IF NOT EXISTS idx_movimento_cartao_criado ON loyalty.movimento_pontos(cartao_id, criado_em);
CREATE INDEX IF NOT EXISTS idx_movimento_tipo ON loyalty.movimento_pontos(tipo);
CREATE INDEX IF NOT EXISTS idx_movimento_transacao ON loyalty.movimento_pontos(transacao_id) WHERE transacao_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_movimento_job ON loyalty.movimento_pontos(job_id) WHERE job_id IS NOT NULL;

-- Índices para saldo_pontos
CREATE INDEX IF NOT EXISTS idx_saldo_usuario ON loyalty.saldo_pontos(usuario_id);
CREATE INDEX IF NOT EXISTS idx_saldo_cartao ON loyalty.saldo_pontos(cartao_id);
CREATE INDEX IF NOT EXISTS idx_saldo_valor ON loyalty.saldo_pontos(saldo);

-- Índices para regra_conversao
CREATE INDEX IF NOT EXISTS idx_regra_vigencia ON loyalty.regra_conversao(vigencia_ini, vigencia_fim);
CREATE INDEX IF NOT EXISTS idx_regra_prioridade ON loyalty.regra_conversao(prioridade DESC);
CREATE INDEX IF NOT EXISTS idx_regra_ativo ON loyalty.regra_conversao(ativo) WHERE ativo = true;

-- Índices para campanha_bonus
CREATE INDEX IF NOT EXISTS idx_campanha_vigencia ON loyalty.campanha_bonus(vigencia_ini, vigencia_fim);
CREATE INDEX IF NOT EXISTS idx_campanha_prioridade ON loyalty.campanha_bonus(prioridade DESC);

-- Índices para recompensa
CREATE INDEX IF NOT EXISTS idx_recompensa_ativo ON loyalty.recompensa(ativo) WHERE ativo = true;
CREATE INDEX IF NOT EXISTS idx_recompensa_tipo ON loyalty.recompensa(tipo);
CREATE INDEX IF NOT EXISTS idx_recompensa_custo ON loyalty.recompensa(custo_pontos);

-- Índices para resgate
CREATE INDEX IF NOT EXISTS idx_resgate_usuario ON loyalty.resgate(usuario_id);
CREATE INDEX IF NOT EXISTS idx_resgate_cartao ON loyalty.resgate(cartao_id);
CREATE INDEX IF NOT EXISTS idx_resgate_status ON loyalty.resgate(status);
CREATE INDEX IF NOT EXISTS idx_resgate_criado ON loyalty.resgate(criado_em);

-- Índices para notificacao
CREATE INDEX IF NOT EXISTS idx_notificacao_usuario ON loyalty.notificacao(usuario_id);
CREATE INDEX IF NOT EXISTS idx_notificacao_status ON loyalty.notificacao(status);
CREATE INDEX IF NOT EXISTS idx_notificacao_canal ON loyalty.notificacao(canal);
CREATE INDEX IF NOT EXISTS idx_notificacao_agendado ON loyalty.notificacao(agendado_para) WHERE agendado_para IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notificacao_proxima_tentativa ON loyalty.notificacao(proxima_tentativa_em) WHERE proxima_tentativa_em IS NOT NULL;

-- =====================================================
-- 5. TRIGGERS PARA AUDITORIA E CONSISTÊNCIA
-- =====================================================

-- Trigger para atualizar atualizado_em em regra_conversao
CREATE OR REPLACE FUNCTION loyalty.update_regra_conversao_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_regra_conversao_updated_at
    BEFORE UPDATE ON loyalty.regra_conversao
    FOR EACH ROW
    EXECUTE FUNCTION loyalty.update_regra_conversao_updated_at();

-- Trigger para atualizar atualizado_em em recompensa
CREATE OR REPLACE FUNCTION loyalty.update_recompensa_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recompensa_updated_at
    BEFORE UPDATE ON loyalty.recompensa
    FOR EACH ROW
    EXECUTE FUNCTION loyalty.update_recompensa_updated_at();

-- =====================================================
-- 6. DADOS INICIAIS (SEEDS)
-- =====================================================

-- Inserir regras de conversão básicas
INSERT INTO loyalty.regra_conversao (nome, multiplicador, vigencia_ini, prioridade, ativo, criado_em)
VALUES 
    ('Regra Geral', 0.01, CURRENT_TIMESTAMP, 1, true, CURRENT_TIMESTAMP),
    ('Supermercados', 0.02, CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP),
    ('Postos de Gasolina', 0.015, CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP),
    ('Restaurantes', 0.025, CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Inserir campanhas de bônus básicas
INSERT INTO loyalty.campanha_bonus (nome, multiplicador_extra, vigencia_ini, vigencia_fim, prioridade)
VALUES 
    ('Bônus Black Friday', 0.5, CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 10),
    ('Bônus Natal', 0.3, CURRENT_DATE, CURRENT_DATE + INTERVAL '45 days', 8),
    ('Bônus Aniversário', 0.2, CURRENT_DATE, CURRENT_DATE + INTERVAL '7 days', 5)
ON CONFLICT DO NOTHING;

-- Inserir recompensas básicas
INSERT INTO loyalty.recompensa (tipo, descricao, custo_pontos, estoque, ativo, criado_em)
VALUES 
    ('CASHBACK', 'Cashback R$ 10,00', 1000, 1000, true, CURRENT_TIMESTAMP),
    ('CASHBACK', 'Cashback R$ 50,00', 5000, 500, true, CURRENT_TIMESTAMP),
    ('GIFT', 'Vale Presente R$ 25,00', 2500, 200, true, CURRENT_TIMESTAMP),
    ('MILHAS', '1.000 Milhas Aéreas', 2000, 1000, true, CURRENT_TIMESTAMP),
    ('PRODUTO', 'Fone de Ouvido Bluetooth', 15000, 50, true, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 7. COMENTÁRIOS E DOCUMENTAÇÃO
-- =====================================================

COMMENT ON SCHEMA loyalty IS 'Schema do sistema de fidelidade por pontos para cartões de crédito';
COMMENT ON TABLE loyalty.usuario IS 'Usuários do sistema de fidelidade';
COMMENT ON TABLE loyalty.cartao IS 'Cartões de crédito dos usuários';
COMMENT ON TABLE loyalty.transacao IS 'Transações financeiras que geram pontos';
COMMENT ON TABLE loyalty.regra_conversao IS 'Regras para conversão de transações em pontos';
COMMENT ON TABLE loyalty.campanha_bonus IS 'Campanhas de bônus temporárias';
COMMENT ON TABLE loyalty.movimento_pontos IS 'Movimentações de pontos (acúmulo, expiração, resgate, estorno)';
COMMENT ON TABLE loyalty.saldo_pontos IS 'Saldo atual de pontos por usuário/cartão';
COMMENT ON TABLE loyalty.recompensa IS 'Catálogo de recompensas disponíveis para resgate';
COMMENT ON TABLE loyalty.resgate IS 'Solicitações de resgate de pontos por recompensas';
COMMENT ON TABLE loyalty.notificacao IS 'Notificações enviadas aos usuários';

-- =====================================================
-- 8. VERIFICAÇÃO FINAL
-- =====================================================

-- Verificar se todas as tabelas foram criadas
DO $$
DECLARE
    table_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO table_count
    FROM information_schema.tables 
    WHERE table_schema = 'loyalty' 
    AND table_name IN ('usuario', 'cartao', 'transacao', 'regra_conversao', 
                       'campanha_bonus', 'movimento_pontos', 'saldo_pontos', 
                       'recompensa', 'resgate', 'notificacao');
    
    RAISE NOTICE 'Total de tabelas criadas no schema loyalty: %', table_count;
    
    IF table_count = 10 THEN
        RAISE NOTICE 'SUCESSO: Todas as 10 tabelas foram criadas/atualizadas com sucesso!';
    ELSE
        RAISE WARNING 'ATENÇÃO: Apenas % de 10 tabelas foram encontradas. Verifique os erros acima.', table_count;
    END IF;
END $$;

-- =====================================================
-- FIM DO SCRIPT
-- =====================================================
