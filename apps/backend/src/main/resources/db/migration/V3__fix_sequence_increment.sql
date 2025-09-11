-- =====================================================
-- V3: CORRIGIR INCREMENTO DAS SEQUÊNCIAS
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- Configurações iniciais
SET search_path TO loyalty, public;

-- =====================================================
-- CORRIGIR INCREMENTO DAS SEQUÊNCIAS PARA 50
-- =====================================================

-- Corrigir sequência campanha_bonus_SEQ
ALTER SEQUENCE loyalty.campanha_bonus_SEQ INCREMENT BY 50;

-- Corrigir sequência regra_conversao_SEQ
ALTER SEQUENCE loyalty.regra_conversao_SEQ INCREMENT BY 50;

-- Corrigir sequência recompensa_SEQ
ALTER SEQUENCE loyalty.recompensa_SEQ INCREMENT BY 50;

-- Corrigir sequência resgate_SEQ
ALTER SEQUENCE loyalty.resgate_SEQ INCREMENT BY 50;

-- Corrigir sequência notificacao_SEQ
ALTER SEQUENCE loyalty.notificacao_SEQ INCREMENT BY 50;

-- Corrigir sequência movimento_pontos_SEQ
ALTER SEQUENCE loyalty.movimento_pontos_SEQ INCREMENT BY 50;

-- Corrigir sequência transacao_SEQ
ALTER SEQUENCE loyalty.transacao_SEQ INCREMENT BY 50;

-- Corrigir sequência cartao_SEQ
ALTER SEQUENCE loyalty.cartao_SEQ INCREMENT BY 50;

-- Corrigir sequência usuario_SEQ
ALTER SEQUENCE loyalty.usuario_SEQ INCREMENT BY 50;
