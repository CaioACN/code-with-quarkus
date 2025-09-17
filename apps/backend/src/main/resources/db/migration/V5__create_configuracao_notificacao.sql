-- Criação da sequência para configuracao_notificacao (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.configuracao_notificacao_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Criação da tabela de configuração de notificação
CREATE TABLE IF NOT EXISTS loyalty.configuracao_notificacao (
    id BIGINT NOT NULL DEFAULT nextval('loyalty.configuracao_notificacao_SEQ'),
    usuario_id BIGINT NOT NULL,
    
    -- Canais
    email_ativo BOOLEAN NOT NULL DEFAULT true,
    sms_ativo BOOLEAN NOT NULL DEFAULT false,
    push_ativo BOOLEAN NOT NULL DEFAULT true,
    
    -- Eventos
    notificar_acumulo BOOLEAN NOT NULL DEFAULT true,
    notificar_expiracao BOOLEAN NOT NULL DEFAULT true,
    notificar_resgate BOOLEAN NOT NULL DEFAULT true,
    notificar_campanha BOOLEAN DEFAULT false,
    
    -- Regras extras
    limite_minimo_pontos_notificar INTEGER DEFAULT 0,
    idioma_preferido VARCHAR(10) DEFAULT 'pt-BR',
    timezone VARCHAR(60) DEFAULT 'America/Sao_Paulo',
    
    -- Janela de silêncio
    silencio_inicio TIME,
    silencio_fim TIME,
    
    -- Digest/Resumo
    digest VARCHAR(10) NOT NULL DEFAULT 'OFF',
    
    -- Constraints
    CONSTRAINT pk_configuracao_notificacao PRIMARY KEY (id),
    CONSTRAINT fk_config_notif_usuario FOREIGN KEY (usuario_id) REFERENCES loyalty.usuario(id) ON DELETE CASCADE,
    CONSTRAINT uk_config_notif_usuario UNIQUE (usuario_id),
    CONSTRAINT chk_config_notif_digest CHECK (digest IN ('OFF', 'DAILY', 'WEEKLY')),
    CONSTRAINT chk_config_notif_limite_positivo CHECK (limite_minimo_pontos_notificar >= 0)
);

-- Índice para performance
CREATE INDEX IF NOT EXISTS idx_config_notif_usuario ON loyalty.configuracao_notificacao(usuario_id);

-- Inserir configurações padrão para usuários existentes
INSERT INTO loyalty.configuracao_notificacao (id, usuario_id, email_ativo, sms_ativo, push_ativo, notificar_acumulo, notificar_expiracao, notificar_resgate, notificar_campanha, digest)
SELECT nextval('loyalty.configuracao_notificacao_SEQ'), id, true, false, true, true, true, true, false, 'OFF'
FROM loyalty.usuario
WHERE id NOT IN (SELECT usuario_id FROM loyalty.configuracao_notificacao);