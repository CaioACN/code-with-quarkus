-- =====================================================
-- V4: SINCRONIZAR SEQUÊNCIAS COM DADOS EXISTENTES
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- Configurações iniciais
SET search_path TO loyalty, public;

-- =====================================================
-- SINCRONIZAR SEQUÊNCIAS COM DADOS EXISTENTES
-- =====================================================

-- Sincronizar sequência recompensa_SEQ com dados existentes
-- A migration V1 insere recompensas com IDs 1-5, então a sequência deve começar em 6
SELECT setval('loyalty.recompensa_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.recompensa), 0) + 1, false);

-- Sincronizar outras sequências também para garantir consistência
SELECT setval('loyalty.usuario_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.usuario), 0) + 1, false);
SELECT setval('loyalty.cartao_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.cartao), 0) + 1, false);
SELECT setval('loyalty.transacao_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.transacao), 0) + 1, false);
SELECT setval('loyalty.movimento_pontos_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.movimento_pontos), 0) + 1, false);
SELECT setval('loyalty.resgate_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.resgate), 0) + 1, false);
SELECT setval('loyalty.notificacao_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.notificacao), 0) + 1, false);
SELECT setval('loyalty.regra_conversao_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.regra_conversao), 0) + 1, false);
SELECT setval('loyalty.campanha_bonus_SEQ', COALESCE((SELECT MAX(id) FROM loyalty.campanha_bonus), 0) + 1, false);