-- =====================================================
-- DADOS COMPLETOS PARA POSTGRESQL LOCAL
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- Configurações
SET search_path TO loyalty, public;

-- =====================================================
-- 1. LIMPEZA DOS DADOS EXISTENTES (OPCIONAL)
-- =====================================================

-- Descomente as linhas abaixo se quiser limpar os dados existentes
-- DELETE FROM loyalty.notificacao;
-- DELETE FROM loyalty.resgate;
-- DELETE FROM loyalty.movimento_pontos;
-- DELETE FROM loyalty.saldo_pontos;
-- DELETE FROM loyalty.transacao;
-- DELETE FROM loyalty.cartao;
-- DELETE FROM loyalty.usuario;
-- DELETE FROM loyalty.recompensa;
-- DELETE FROM loyalty.regra_conversao;
-- DELETE FROM loyalty.campanha_bonus;

-- =====================================================
-- 2. DADOS DE TESTE - REGRAS DE CONVERSÃO
-- =====================================================

INSERT INTO loyalty.regra_conversao (nome, multiplicador, mcc_regex, categoria, vigencia_ini, prioridade, ativo, criado_em) VALUES
    ('Regra Geral', 0.01, NULL, NULL, CURRENT_TIMESTAMP, 1, true, CURRENT_TIMESTAMP),
    ('Supermercados', 0.02, '5411', 'SUPERMERCADO', CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP),
    ('Postos de Gasolina', 0.015, '5541', 'POSTO_GASOLINA', CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP),
    ('Restaurantes', 0.025, '5812', 'RESTAURANTE', CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP),
    ('Farmácias', 0.02, '5912', 'FARMACIA', CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP),
    ('Postos de Combustível', 0.015, '5542', 'POSTO_GASOLINA', CURRENT_TIMESTAMP, 2, true, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 3. DADOS DE TESTE - RECOMPENSAS
-- =====================================================

INSERT INTO loyalty.recompensa (tipo, descricao, custo_pontos, estoque, ativo, detalhes, criado_em) VALUES
    ('CASHBACK', 'Cashback R$ 10,00', 1000, 1000, true, 'Cashback direto na fatura do cartão', CURRENT_TIMESTAMP),
    ('CASHBACK', 'Cashback R$ 25,00', 2500, 500, true, 'Cashback direto na fatura do cartão', CURRENT_TIMESTAMP),
    ('CASHBACK', 'Cashback R$ 50,00', 5000, 300, true, 'Cashback direto na fatura do cartão', CURRENT_TIMESTAMP),
    ('CASHBACK', 'Cashback R$ 100,00', 10000, 100, true, 'Cashback direto na fatura do cartão', CURRENT_TIMESTAMP),
    ('GIFT', 'Vale Presente R$ 25,00', 2500, 200, true, 'Vale presente para uso em lojas parceiras', CURRENT_TIMESTAMP),
    ('GIFT', 'Vale Presente R$ 50,00', 5000, 150, true, 'Vale presente para uso em lojas parceiras', CURRENT_TIMESTAMP),
    ('GIFT', 'Vale Presente R$ 100,00', 10000, 100, true, 'Vale presente para uso em lojas parceiras', CURRENT_TIMESTAMP),
    ('MILHAS', '1.000 Milhas Aéreas', 2000, 1000, true, 'Milhas para uso em companhias aéreas parceiras', CURRENT_TIMESTAMP),
    ('MILHAS', '2.500 Milhas Aéreas', 5000, 500, true, 'Milhas para uso em companhias aéreas parceiras', CURRENT_TIMESTAMP),
    ('MILHAS', '5.000 Milhas Aéreas', 10000, 200, true, 'Milhas para uso em companhias aéreas parceiras', CURRENT_TIMESTAMP),
    ('PRODUTO', 'Fone de Ouvido Bluetooth', 15000, 50, true, 'Fone de ouvido sem fio com cancelamento de ruído', CURRENT_TIMESTAMP),
    ('PRODUTO', 'Power Bank 20.000mAh', 8000, 75, true, 'Carregador portátil de alta capacidade', CURRENT_TIMESTAMP),
    ('PRODUTO', 'Cabo USB-C Premium', 3000, 200, true, 'Cabo USB-C de alta qualidade', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 4. DADOS DE TESTE - CAMPANHAS DE BÔNUS
-- =====================================================

INSERT INTO loyalty.campanha_bonus (nome, multiplicador_extra, vigencia_ini, vigencia_fim, segmento, prioridade, teto) VALUES
    ('Black Friday 2024', 0.05, '2024-11-25', '2024-11-30', 'GERAL', 1, 10000),
    ('Natal 2024', 0.03, '2024-12-15', '2024-12-25', 'GERAL', 2, 5000),
    ('Supermercados Dezembro', 0.02, '2024-12-01', '2024-12-31', 'SUPERMERCADO', 3, 3000),
    ('Restaurantes Fim de Ano', 0.04, '2024-12-20', '2025-01-05', 'RESTAURANTE', 2, 2000)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 5. DADOS DE TESTE - USUÁRIOS
-- =====================================================

INSERT INTO loyalty.usuario (nome, email, data_cadastro) VALUES
    ('João Silva', 'joao.silva@email.com', '2024-01-15'),
    ('Maria Santos', 'maria.santos@email.com', '2024-02-20'),
    ('Pedro Oliveira', 'pedro.oliveira@email.com', '2024-03-10'),
    ('Ana Costa', 'ana.costa@email.com', '2024-04-05'),
    ('Carlos Ferreira', 'carlos.ferreira@email.com', '2024-05-12'),
    ('Lucia Mendes', 'lucia.mendes@email.com', '2024-06-18'),
    ('Roberto Alves', 'roberto.alves@email.com', '2024-07-22'),
    ('Fernanda Lima', 'fernanda.lima@email.com', '2024-08-30')
ON CONFLICT (email) DO NOTHING;

-- =====================================================
-- 6. DADOS DE TESTE - CARTÕES
-- =====================================================

INSERT INTO loyalty.cartao (numero, nome_impresso, validade, limite, id_usuario) VALUES
    ('1234567890123456', 'JOAO SILVA', '2026-12-31', 5000.00, 1),
    ('2345678901234567', 'MARIA SANTOS', '2027-06-30', 8000.00, 2),
    ('3456789012345678', 'PEDRO OLIVEIRA', '2026-09-15', 3000.00, 3),
    ('4567890123456789', 'ANA COSTA', '2027-03-20', 10000.00, 4),
    ('5678901234567890', 'CARLOS FERREIRA', '2026-11-10', 6000.00, 5),
    ('6789012345678901', 'LUCIA MENDES', '2027-01-15', 4000.00, 6),
    ('7890123456789012', 'ROBERTO ALVES', '2026-08-20', 7000.00, 7),
    ('8901234567890123', 'FERNANDA LIMA', '2027-04-10', 9000.00, 8)
ON CONFLICT (numero) DO NOTHING;

-- =====================================================
-- 7. DADOS DE TESTE - TRANSAÇÕES
-- =====================================================

INSERT INTO loyalty.transacao (cartao_id, usuario_id, valor, moeda, mcc, categoria, parceiro_id, status, autorizacao, data_evento, pontos_gerados) VALUES
    -- Transações do João Silva
    (1, 1, 150.75, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH001', '2024-12-01 12:30:00', 4),
    (1, 1, 89.90, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH002', '2024-12-02 09:15:00', 2),
    (1, 1, 45.00, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH003', '2024-12-03 18:45:00', 1),
    (1, 1, 120.50, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH004', '2024-12-04 19:20:00', 3),
    
    -- Transações da Maria Santos
    (2, 2, 320.50, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH005', '2024-12-01 19:20:00', 8),
    (2, 2, 125.30, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH006', '2024-12-02 14:10:00', 3),
    (2, 2, 78.90, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH007', '2024-12-03 08:30:00', 1),
    (2, 2, 200.00, 'BRL', '5912', 'FARMACIA', NULL, 'APROVADA', 'AUTH008', '2024-12-04 16:45:00', 4),
    
    -- Transações do Pedro Oliveira
    (3, 3, 95.60, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH009', '2024-12-01 13:45:00', 2),
    (3, 3, 67.80, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH010', '2024-12-02 16:20:00', 1),
    (3, 3, 35.50, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH011', '2024-12-03 07:30:00', 1),
    
    -- Transações da Ana Costa
    (4, 4, 450.00, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH012', '2024-12-01 20:00:00', 11),
    (4, 4, 200.00, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH013', '2024-12-02 11:30:00', 4),
    (4, 4, 120.00, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH014', '2024-12-03 07:15:00', 2),
    (4, 4, 300.00, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH015', '2024-12-04 21:00:00', 8),
    
    -- Transações do Carlos Ferreira
    (5, 5, 180.25, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH016', '2024-12-01 18:30:00', 5),
    (5, 5, 95.40, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH017', '2024-12-02 15:45:00', 2),
    (5, 5, 65.00, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH018', '2024-12-03 12:00:00', 1),
    (5, 5, 150.00, 'BRL', '5912', 'FARMACIA', NULL, 'APROVADA', 'AUTH019', '2024-12-04 14:20:00', 3),
    
    -- Transações da Lucia Mendes
    (6, 6, 85.30, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH020', '2024-12-01 12:15:00', 2),
    (6, 6, 110.20, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH021', '2024-12-02 10:30:00', 2),
    (6, 6, 55.80, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH022', '2024-12-03 17:45:00', 1),
    
    -- Transações do Roberto Alves
    (7, 7, 250.00, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH023', '2024-12-01 20:30:00', 6),
    (7, 7, 180.50, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH024', '2024-12-02 13:20:00', 4),
    (7, 7, 90.00, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH025', '2024-12-03 09:15:00', 1),
    
    -- Transações da Fernanda Lima
    (8, 8, 380.75, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH026', '2024-12-01 19:45:00', 10),
    (8, 8, 220.30, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH027', '2024-12-02 15:10:00', 4),
    (8, 8, 75.60, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH028', '2024-12-03 11:30:00', 1),
    (8, 8, 160.00, 'BRL', '5912', 'FARMACIA', NULL, 'APROVADA', 'AUTH029', '2024-12-04 16:00:00', 3)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 8. DADOS DE TESTE - MOVIMENTOS DE PONTOS
-- =====================================================

INSERT INTO loyalty.movimento_pontos (usuario_id, cartao_id, tipo, pontos, ref_transacao_id, transacao_id, observacao, criado_em) VALUES
    -- Movimentos de acúmulo baseados nas transações
    (1, 1, 'ACUMULO', 4, 1, 1, 'Acúmulo por transação em restaurante', '2024-12-01 12:30:00'),
    (1, 1, 'ACUMULO', 2, 2, 2, 'Acúmulo por transação em supermercado', '2024-12-02 09:15:00'),
    (1, 1, 'ACUMULO', 1, 3, 3, 'Acúmulo por transação em posto de gasolina', '2024-12-03 18:45:00'),
    (1, 1, 'ACUMULO', 3, 4, 4, 'Acúmulo por transação em restaurante', '2024-12-04 19:20:00'),
    
    (2, 2, 'ACUMULO', 8, 5, 5, 'Acúmulo por transação em restaurante', '2024-12-01 19:20:00'),
    (2, 2, 'ACUMULO', 3, 6, 6, 'Acúmulo por transação em supermercado', '2024-12-02 14:10:00'),
    (2, 2, 'ACUMULO', 1, 7, 7, 'Acúmulo por transação em posto de gasolina', '2024-12-03 08:30:00'),
    (2, 2, 'ACUMULO', 4, 8, 8, 'Acúmulo por transação em farmácia', '2024-12-04 16:45:00'),
    
    (3, 3, 'ACUMULO', 2, 9, 9, 'Acúmulo por transação em restaurante', '2024-12-01 13:45:00'),
    (3, 3, 'ACUMULO', 1, 10, 10, 'Acúmulo por transação em supermercado', '2024-12-02 16:20:00'),
    (3, 3, 'ACUMULO', 1, 11, 11, 'Acúmulo por transação em posto de gasolina', '2024-12-03 07:30:00'),
    
    (4, 4, 'ACUMULO', 11, 12, 12, 'Acúmulo por transação em restaurante', '2024-12-01 20:00:00'),
    (4, 4, 'ACUMULO', 4, 13, 13, 'Acúmulo por transação em supermercado', '2024-12-02 11:30:00'),
    (4, 4, 'ACUMULO', 2, 14, 14, 'Acúmulo por transação em posto de gasolina', '2024-12-03 07:15:00'),
    (4, 4, 'ACUMULO', 8, 15, 15, 'Acúmulo por transação em restaurante', '2024-12-04 21:00:00'),
    
    (5, 5, 'ACUMULO', 5, 16, 16, 'Acúmulo por transação em restaurante', '2024-12-01 18:30:00'),
    (5, 5, 'ACUMULO', 2, 17, 17, 'Acúmulo por transação em supermercado', '2024-12-02 15:45:00'),
    (5, 5, 'ACUMULO', 1, 18, 18, 'Acúmulo por transação em posto de gasolina', '2024-12-03 12:00:00'),
    (5, 5, 'ACUMULO', 3, 19, 19, 'Acúmulo por transação em farmácia', '2024-12-04 14:20:00'),
    
    (6, 6, 'ACUMULO', 2, 20, 20, 'Acúmulo por transação em restaurante', '2024-12-01 12:15:00'),
    (6, 6, 'ACUMULO', 2, 21, 21, 'Acúmulo por transação em supermercado', '2024-12-02 10:30:00'),
    (6, 6, 'ACUMULO', 1, 22, 22, 'Acúmulo por transação em posto de gasolina', '2024-12-03 17:45:00'),
    
    (7, 7, 'ACUMULO', 6, 23, 23, 'Acúmulo por transação em restaurante', '2024-12-01 20:30:00'),
    (7, 7, 'ACUMULO', 4, 24, 24, 'Acúmulo por transação em supermercado', '2024-12-02 13:20:00'),
    (7, 7, 'ACUMULO', 1, 25, 25, 'Acúmulo por transação em posto de gasolina', '2024-12-03 09:15:00'),
    
    (8, 8, 'ACUMULO', 10, 26, 26, 'Acúmulo por transação em restaurante', '2024-12-01 19:45:00'),
    (8, 8, 'ACUMULO', 4, 27, 27, 'Acúmulo por transação em supermercado', '2024-12-02 15:10:00'),
    (8, 8, 'ACUMULO', 1, 28, 28, 'Acúmulo por transação em posto de gasolina', '2024-12-03 11:30:00'),
    (8, 8, 'ACUMULO', 3, 29, 29, 'Acúmulo por transação em farmácia', '2024-12-04 16:00:00')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 9. DADOS DE TESTE - SALDOS DE PONTOS
-- =====================================================

INSERT INTO loyalty.saldo_pontos (usuario_id, cartao_id, saldo, atualizado_em, pontos_expirando_30_dias, pontos_expirando_60_dias, pontos_expirando_90_dias) VALUES
    (1, 1, 10, '2024-12-04 19:20:00', 0, 0, 0),
    (2, 2, 16, '2024-12-04 16:45:00', 0, 0, 0),
    (3, 3, 4, '2024-12-03 07:30:00', 0, 0, 0),
    (4, 4, 25, '2024-12-04 21:00:00', 0, 0, 0),
    (5, 5, 11, '2024-12-04 14:20:00', 0, 0, 0),
    (6, 6, 5, '2024-12-03 17:45:00', 0, 0, 0),
    (7, 7, 11, '2024-12-03 09:15:00', 0, 0, 0),
    (8, 8, 18, '2024-12-04 16:00:00', 0, 0, 0)
ON CONFLICT (usuario_id, cartao_id) DO UPDATE SET
    saldo = EXCLUDED.saldo,
    atualizado_em = EXCLUDED.atualizado_em;

-- =====================================================
-- 10. DADOS DE TESTE - RESGATES
-- =====================================================

INSERT INTO loyalty.resgate (usuario_id, cartao_id, recompensa_id, pontos_utilizados, status, criado_em, aprovado_em, concluido_em) VALUES
    (2, 2, 1, 1000, 'CONCLUIDO', '2024-11-15 10:00:00', '2024-11-15 10:30:00', '2024-11-15 11:00:00'),
    (4, 4, 2, 2500, 'APROVADO', '2024-11-20 14:00:00', '2024-11-20 14:15:00', NULL),
    (5, 5, 3, 2500, 'PENDENTE', '2024-12-01 16:00:00', NULL, NULL),
    (8, 8, 4, 5000, 'APROVADO', '2024-12-02 09:30:00', '2024-12-02 10:00:00', NULL),
    (1, 1, 5, 1000, 'CONCLUIDO', '2024-11-25 15:00:00', '2024-11-25 15:30:00', '2024-11-25 16:00:00')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 11. DADOS DE TESTE - NOTIFICAÇÕES
-- =====================================================

INSERT INTO loyalty.notificacao (usuario_id, cartao_id, transacao_id, canal, tipo, status, titulo, mensagem, destino, criado_em, enviado_em, tentativas) VALUES
    (1, 1, 1, 'EMAIL', 'ACUMULO', 'ENVIADA', 'Pontos Acumulados!', 'Você acumulou 4 pontos na sua compra de R$ 150,75.', 'joao.silva@email.com', '2024-12-01 12:30:00', '2024-12-01 12:31:00', 1),
    (2, 2, 5, 'EMAIL', 'ACUMULO', 'ENVIADA', 'Pontos Acumulados!', 'Você acumulou 8 pontos na sua compra de R$ 320,50.', 'maria.santos@email.com', '2024-12-01 19:20:00', '2024-12-01 19:21:00', 1),
    (4, 4, 12, 'EMAIL', 'ACUMULO', 'ENVIADA', 'Pontos Acumulados!', 'Você acumulou 11 pontos na sua compra de R$ 450,00.', 'ana.costa@email.com', '2024-12-01 20:00:00', '2024-12-01 20:01:00', 1),
    (2, 2, NULL, 'EMAIL', 'RESGATE', 'ENVIADA', 'Resgate Aprovado!', 'Seu resgate de R$ 10,00 foi aprovado e processado.', 'maria.santos@email.com', '2024-11-15 11:00:00', '2024-11-15 11:01:00', 1),
    (4, 4, NULL, 'EMAIL', 'RESGATE', 'ENVIADA', 'Resgate Aprovado!', 'Seu resgate de R$ 25,00 foi aprovado e está sendo processado.', 'ana.costa@email.com', '2024-11-20 14:15:00', '2024-11-20 14:16:00', 1),
    (8, 8, 26, 'EMAIL', 'ACUMULO', 'ENVIADA', 'Pontos Acumulados!', 'Você acumulou 10 pontos na sua compra de R$ 380,75.', 'fernanda.lima@email.com', '2024-12-01 19:45:00', '2024-12-01 19:46:00', 1),
    (1, 1, NULL, 'EMAIL', 'RESGATE', 'ENVIADA', 'Resgate Concluído!', 'Seu resgate de R$ 10,00 foi concluído com sucesso.', 'joao.silva@email.com', '2024-11-25 16:00:00', '2024-11-25 16:01:00', 1)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 12. VERIFICAÇÃO DOS DADOS INSERIDOS
-- =====================================================

-- Contar registros inseridos
SELECT 'REGRAS CONVERSÃO' as tabela, COUNT(*) as total FROM loyalty.regra_conversao
UNION ALL
SELECT 'RECOMPENSAS' as tabela, COUNT(*) as total FROM loyalty.recompensa
UNION ALL
SELECT 'CAMPANHAS BÔNUS' as tabela, COUNT(*) as total FROM loyalty.campanha_bonus
UNION ALL
SELECT 'USUÁRIOS' as tabela, COUNT(*) as total FROM loyalty.usuario
UNION ALL
SELECT 'CARTÕES' as tabela, COUNT(*) as total FROM loyalty.cartao
UNION ALL
SELECT 'TRANSAÇÕES' as tabela, COUNT(*) as total FROM loyalty.transacao
UNION ALL
SELECT 'MOVIMENTOS' as tabela, COUNT(*) as total FROM loyalty.movimento_pontos
UNION ALL
SELECT 'SALDOS' as tabela, COUNT(*) as total FROM loyalty.saldo_pontos
UNION ALL
SELECT 'RESGATES' as tabela, COUNT(*) as total FROM loyalty.resgate
UNION ALL
SELECT 'NOTIFICAÇÕES' as tabela, COUNT(*) as total FROM loyalty.notificacao
ORDER BY tabela;

-- Mostrar saldos por usuário
SELECT 
    u.nome as usuario,
    c.numero as cartao,
    sp.saldo as pontos_saldo,
    sp.atualizado_em as ultima_atualizacao
FROM loyalty.saldo_pontos sp
JOIN loyalty.usuario u ON sp.usuario_id = u.id
JOIN loyalty.cartao c ON sp.cartao_id = c.id
ORDER BY sp.saldo DESC;

-- Mostrar resgates por usuário
SELECT 
    u.nome as usuario,
    r.descricao as recompensa,
    rg.pontos_utilizados as pontos,
    rg.status as status_resgate,
    rg.criado_em as data_solicitacao
FROM loyalty.resgate rg
JOIN loyalty.usuario u ON rg.usuario_id = u.id
JOIN loyalty.recompensa r ON rg.recompensa_id = r.id
ORDER BY rg.criado_em DESC;

-- Mostrar transações recentes
SELECT 
    u.nome as usuario,
    t.valor,
    t.categoria,
    t.pontos_gerados,
    t.data_evento
FROM loyalty.transacao t
JOIN loyalty.usuario u ON t.usuario_id = u.id
ORDER BY t.data_evento DESC
LIMIT 10;

-- =====================================================
-- FIM DOS DADOS COMPLETOS
-- =====================================================