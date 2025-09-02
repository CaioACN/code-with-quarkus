-- =====================================================
-- DADOS DE TESTE PARA O SISTEMA DE PONTOS
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- Configurações
SET search_path TO loyalty, public;

-- =====================================================
-- 1. DADOS DE TESTE - USUÁRIOS
-- =====================================================

INSERT INTO loyalty.usuario (nome, email, data_cadastro) VALUES
    ('João Silva', 'joao.silva@email.com', '2024-01-15'),
    ('Maria Santos', 'maria.santos@email.com', '2024-02-20'),
    ('Pedro Oliveira', 'pedro.oliveira@email.com', '2024-03-10'),
    ('Ana Costa', 'ana.costa@email.com', '2024-04-05'),
    ('Carlos Ferreira', 'carlos.ferreira@email.com', '2024-05-12')
ON CONFLICT (email) DO NOTHING;

-- =====================================================
-- 2. DADOS DE TESTE - CARTÕES
-- =====================================================

INSERT INTO loyalty.cartao (numero, nome_impresso, validade, limite, id_usuario) VALUES
    ('1234567890123456', 'JOAO SILVA', '2026-12-31', 5000.00, 1),
    ('2345678901234567', 'MARIA SANTOS', '2027-06-30', 8000.00, 2),
    ('3456789012345678', 'PEDRO OLIVEIRA', '2026-09-15', 3000.00, 3),
    ('4567890123456789', 'ANA COSTA', '2027-03-20', 10000.00, 4),
    ('5678901234567890', 'CARLOS FERREIRA', '2026-11-10', 6000.00, 5)
ON CONFLICT (numero) DO NOTHING;

-- =====================================================
-- 3. DADOS DE TESTE - TRANSAÇÕES
-- =====================================================

INSERT INTO loyalty.transacao (cartao_id, usuario_id, valor, moeda, mcc, categoria, parceiro_id, status, autorizacao, data_evento, pontos_gerados) VALUES
    -- Transações do João Silva
    (1, 1, 150.75, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH001', '2024-12-01 12:30:00', 2),
    (1, 1, 89.90, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH002', '2024-12-02 09:15:00', 2),
    (1, 1, 45.00, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH003', '2024-12-03 18:45:00', 1),
    
    -- Transações da Maria Santos
    (2, 2, 320.50, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH004', '2024-12-01 19:20:00', 6),
    (2, 2, 125.30, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH005', '2024-12-02 14:10:00', 3),
    (2, 2, 78.90, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH006', '2024-12-03 08:30:00', 1),
    
    -- Transações do Pedro Oliveira
    (3, 3, 95.60, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH007', '2024-12-01 13:45:00', 2),
    (3, 3, 67.80, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH008', '2024-12-02 16:20:00', 1),
    
    -- Transações da Ana Costa
    (4, 4, 450.00, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH009', '2024-12-01 20:00:00', 9),
    (4, 4, 200.00, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH010', '2024-12-02 11:30:00', 4),
    (4, 4, 120.00, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH011', '2024-12-03 07:15:00', 2),
    
    -- Transações do Carlos Ferreira
    (5, 5, 180.25, 'BRL', '5812', 'RESTAURANTE', NULL, 'APROVADA', 'AUTH012', '2024-12-01 18:30:00', 4),
    (5, 5, 95.40, 'BRL', '5411', 'SUPERMERCADO', NULL, 'APROVADA', 'AUTH013', '2024-12-02 15:45:00', 2),
    (5, 5, 65.00, 'BRL', '5541', 'POSTO_GASOLINA', NULL, 'APROVADA', 'AUTH014', '2024-12-03 12:00:00', 1)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 4. DADOS DE TESTE - MOVIMENTOS DE PONTOS
-- =====================================================

INSERT INTO loyalty.movimento_pontos (usuario_id, cartao_id, tipo, pontos, ref_transacao_id, transacao_id, observacao, criado_em) VALUES
    -- Movimentos de acúmulo baseados nas transações
    (1, 1, 'ACUMULO', 2, 1, 1, 'Acúmulo por transação em restaurante', '2024-12-01 12:30:00'),
    (1, 1, 'ACUMULO', 2, 2, 2, 'Acúmulo por transação em supermercado', '2024-12-02 09:15:00'),
    (1, 1, 'ACUMULO', 1, 3, 3, 'Acúmulo por transação em posto de gasolina', '2024-12-03 18:45:00'),
    
    (2, 2, 'ACUMULO', 6, 4, 4, 'Acúmulo por transação em restaurante', '2024-12-01 19:20:00'),
    (2, 2, 'ACUMULO', 3, 5, 5, 'Acúmulo por transação em supermercado', '2024-12-02 14:10:00'),
    (2, 2, 'ACUMULO', 1, 6, 6, 'Acúmulo por transação em posto de gasolina', '2024-12-03 08:30:00'),
    
    (3, 3, 'ACUMULO', 2, 7, 7, 'Acúmulo por transação em restaurante', '2024-12-01 13:45:00'),
    (3, 3, 'ACUMULO', 1, 8, 8, 'Acúmulo por transação em supermercado', '2024-12-02 16:20:00'),
    
    (4, 4, 'ACUMULO', 9, 9, 9, 'Acúmulo por transação em restaurante', '2024-12-01 20:00:00'),
    (4, 4, 'ACUMULO', 4, 10, 10, 'Acúmulo por transação em supermercado', '2024-12-02 11:30:00'),
    (4, 4, 'ACUMULO', 2, 11, 11, 'Acúmulo por transação em posto de gasolina', '2024-12-03 07:15:00'),
    
    (5, 5, 'ACUMULO', 4, 12, 12, 'Acúmulo por transação em restaurante', '2024-12-01 18:30:00'),
    (5, 5, 'ACUMULO', 2, 13, 13, 'Acúmulo por transação em supermercado', '2024-12-02 15:45:00'),
    (5, 5, 'ACUMULO', 1, 14, 14, 'Acúmulo por transação em posto de gasolina', '2024-12-03 12:00:00')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 5. DADOS DE TESTE - SALDOS DE PONTOS
-- =====================================================

INSERT INTO loyalty.saldo_pontos (usuario_id, cartao_id, saldo, atualizado_em, pontos_expirando_30_dias, pontos_expirando_60_dias, pontos_expirando_90_dias) VALUES
    (1, 1, 5, '2024-12-03 18:45:00', 0, 0, 0),
    (2, 2, 10, '2024-12-03 08:30:00', 0, 0, 0),
    (3, 3, 3, '2024-12-02 16:20:00', 0, 0, 0),
    (4, 4, 15, '2024-12-03 07:15:00', 0, 0, 0),
    (5, 5, 7, '2024-12-03 12:00:00', 0, 0, 0)
ON CONFLICT (usuario_id, cartao_id) DO UPDATE SET
    saldo = EXCLUDED.saldo,
    atualizado_em = EXCLUDED.atualizado_em;

-- =====================================================
-- 6. DADOS DE TESTE - RESGATES
-- =====================================================

INSERT INTO loyalty.resgate (usuario_id, cartao_id, recompensa_id, pontos_utilizados, status, criado_em, aprovado_em, concluido_em) VALUES
    (2, 2, 1, 1000, 'CONCLUIDO', '2024-11-15 10:00:00', '2024-11-15 10:30:00', '2024-11-15 11:00:00'),
    (4, 4, 2, 5000, 'APROVADO', '2024-11-20 14:00:00', '2024-11-20 14:15:00', NULL),
    (5, 5, 3, 2500, 'PENDENTE', '2024-12-01 16:00:00', NULL, NULL)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 7. DADOS DE TESTE - NOTIFICAÇÕES
-- =====================================================

INSERT INTO loyalty.notificacao (usuario_id, cartao_id, transacao_id, canal, tipo, status, titulo, mensagem, destino, criado_em, enviado_em) VALUES
    (1, 1, 1, 'EMAIL', 'ACUMULO', 'ENVIADA', 'Pontos Acumulados!', 'Você acumulou 2 pontos na sua compra de R$ 150,75.', 'joao.silva@email.com', '2024-12-01 12:30:00', '2024-12-01 12:31:00'),
    (2, 2, 4, 'EMAIL', 'ACUMULO', 'ENVIADA', 'Pontos Acumulados!', 'Você acumulou 6 pontos na sua compra de R$ 320,50.', 'maria.santos@email.com', '2024-12-01 19:20:00', '2024-12-01 19:21:00'),
    (4, 4, 9, 'EMAIL', 'ACUMULO', 'ENVIADA', 'Pontos Acumulados!', 'Você acumulou 9 pontos na sua compra de R$ 450,00.', 'ana.costa@email.com', '2024-12-01 20:00:00', '2024-12-01 20:01:00'),
    (2, 2, NULL, 'EMAIL', 'RESGATE', 'ENVIADA', 'Resgate Aprovado!', 'Seu resgate de R$ 10,00 foi aprovado e processado.', 'maria.santos@email.com', '2024-11-15 11:00:00', '2024-11-15 11:01:00'),
    (4, 4, NULL, 'EMAIL', 'RESGATE', 'ENVIADA', 'Resgate Aprovado!', 'Seu resgate de R$ 50,00 foi aprovado e está sendo processado.', 'ana.costa@email.com', '2024-11-20 14:15:00', '2024-11-20 14:16:00')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 8. VERIFICAÇÃO DOS DADOS INSERIDOS
-- =====================================================

-- Contar registros inseridos
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
ORDER BY u.nome;

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

-- =====================================================
-- FIM DOS DADOS DE TESTE
-- =====================================================
