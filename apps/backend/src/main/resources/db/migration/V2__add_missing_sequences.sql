-- =====================================================
-- V2: ADICIONAR SEQUÊNCIAS FALTANTES
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- Configurações iniciais
SET search_path TO loyalty, public;

-- =====================================================
-- SEQUÊNCIAS PARA ENTIDADES PANACHE
-- =====================================================

-- Sequência para campanha_bonus (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.campanha_bonus_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para regra_conversao (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.regra_conversao_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para recompensa (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.recompensa_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para resgate (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.resgate_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para notificacao (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.notificacao_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para movimento_pontos (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.movimento_pontos_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para transacao (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.transacao_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para cartao (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.cartao_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Sequência para usuario (PanacheEntity)
CREATE SEQUENCE IF NOT EXISTS loyalty.usuario_SEQ
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
