-- Dados de teste para o sistema de pontos
-- Este arquivo é executado automaticamente durante os testes

-- Inserir usuário de teste
INSERT INTO loyalty.usuario (id, nome, email, data_cadastro) VALUES 
(1, 'João Silva', 'joao.silva@teste.com', CURRENT_DATE),
(2, 'Maria Santos', 'maria.santos@teste.com', CURRENT_DATE),
(3, 'Pedro Oliveira', 'pedro.oliveira@teste.com', CURRENT_DATE);

-- Inserir cartões de teste
INSERT INTO loyalty.cartao (id, usuario_id, numero, tipo, ativo, data_emissao) VALUES 
(1, 1, '1234567890123456', 'CREDITO', true, CURRENT_DATE),
(2, 2, '2345678901234567', 'DEBITO', true, CURRENT_DATE),
(3, 3, '3456789012345678', 'CREDITO', true, CURRENT_DATE);

-- Inserir saldo de pontos
INSERT INTO loyalty.saldo_pontos (usuario_id, cartao_id, saldo, atualizado_em, pontos_expirando_30_dias, pontos_expirando_60_dias, pontos_expirando_90_dias) VALUES 
(1, 1, 1000, CURRENT_TIMESTAMP, 100, 200, 300),
(2, 2, 500, CURRENT_TIMESTAMP, 50, 100, 150),
(3, 3, 2000, CURRENT_TIMESTAMP, 200, 400, 600);

-- Inserir transações de teste
INSERT INTO loyalty.transacao (id, cartao_id, usuario_id, valor, moeda, mcc, categoria, parceiro_id, status, autorizacao, data_evento, processado_em, pontos_gerados) VALUES 
(1, 1, 1, 100.50, 'BRL', '5411', 'Supermercado', 1, 'APROVADA', 'AUTH001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100),
(2, 2, 2, 250.00, 'BRL', '5812', 'Restaurante', 2, 'APROVADA', 'AUTH002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 250),
(3, 3, 3, 500.00, 'BRL', '5311', 'Loja de Departamentos', 3, 'APROVADA', 'AUTH003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 500);

-- Inserir recompensas de teste
INSERT INTO loyalty.recompensa (id, tipo, descricao, custo_pontos, estoque, parceiro_id, ativo, detalhes, imagem_url, validade_recompensa, criado_em, atualizado_em) VALUES 
(1, 'MILHAS', '1000 Milhas Aéreas', 1000, 50, 1, true, 'Válido para qualquer companhia aérea', 'https://exemplo.com/milhas.jpg', CURRENT_TIMESTAMP + INTERVAL '1 year', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'GIFT', 'Vale Presente R$ 50', 500, 100, 2, true, 'Válido em lojas parceiras', 'https://exemplo.com/gift.jpg', CURRENT_TIMESTAMP + INTERVAL '6 months', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'CASHBACK', 'Cashback R$ 25', 250, 200, 3, true, 'Depositado na conta corrente', 'https://exemplo.com/cashback.jpg', CURRENT_TIMESTAMP + INTERVAL '3 months', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inserir campanhas de bônus de teste
INSERT INTO loyalty.campanha_bonus (id, nome, descricao, multiplicador_bonus, data_inicio, data_fim, ativo, criado_em, atualizado_em) VALUES 
(1, 'Black Friday 2024', 'Campanha especial Black Friday com bônus de 2x pontos', 2.0, CURRENT_DATE, CURRENT_DATE + INTERVAL '7 days', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Natal 2024', 'Campanha de Natal com bônus de 1.5x pontos', 1.5, CURRENT_DATE + INTERVAL '30 days', CURRENT_DATE + INTERVAL '60 days', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inserir regras de conversão de teste
INSERT INTO loyalty.regra_conversao (id, nome, multiplicador, mcc_regex, categoria, parceiro_id, vigencia_ini, vigencia_fim, prioridade, teto_mensal, ativo, criado_em, atualizado_em) VALUES 
(1, 'Supermercados', 1.0, '5411', 'Alimentação', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year', 1, 10000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Restaurantes', 2.0, '5812', 'Alimentação', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year', 2, 5000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'Postos de Combustível', 1.5, '5541', 'Combustível', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year', 3, 8000, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inserir resgates de teste
INSERT INTO loyalty.resgate (id, usuario_id, cartao_id, recompensa_id, pontos_utilizados, status, criado_em, aprovado_em, concluido_em, negado_em, observacao, motivo_negacao, codigo_rastreio, parceiro_processador) VALUES 
(1, 1, 1, 1, 1000, 'PENDENTE', CURRENT_TIMESTAMP, NULL, NULL, NULL, 'Solicitação de milhas', NULL, 'RESG001', 'Parceiro Aéreo'),
(2, 2, 2, 2, 500, 'APROVADO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, NULL, 'Vale presente aprovado', NULL, 'RESG002', 'Parceiro Varejo'),
(3, 3, 3, 3, 250, 'CONCLUIDO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL, 'Cashback processado', NULL, 'RESG003', 'Parceiro Financeiro');

-- Inserir movimentos de pontos de teste
INSERT INTO loyalty.movimento_pontos (id, usuario_id, cartao_id, transacao_id, tipo, pontos, saldo_anterior, saldo_atual, data_movimento, descricao, parceiro_id) VALUES 
(1, 1, 1, 1, 'ACUMULO', 100, 900, 1000, CURRENT_TIMESTAMP, 'Pontos acumulados por compra', 1),
(2, 2, 2, 2, 'ACUMULO', 250, 250, 500, CURRENT_TIMESTAMP, 'Pontos acumulados por compra', 2),
(3, 3, 3, 3, 'ACUMULO', 500, 1500, 2000, CURRENT_TIMESTAMP, 'Pontos acumulados por compra', 3);

-- Inserir notificações de teste
INSERT INTO loyalty.notificacao (id, usuario_id, tipo, titulo, mensagem, canal, status, data_envio, data_leitura, tentativas_envio, max_tentativas, dados_adicionais, criado_em, atualizado_em) VALUES 
(1, 1, 'PONTOS_ACUMULADOS', 'Pontos Acumulados!', 'Você acumulou 100 pontos na sua compra', 'EMAIL', 'ENVIADA', CURRENT_TIMESTAMP, NULL, 1, 3, '{"pontos": 100, "transacao_id": 1}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 2, 'RESGATE_APROVADO', 'Resgate Aprovado!', 'Seu resgate de vale presente foi aprovado', 'SMS', 'ENVIADA', CURRENT_TIMESTAMP, NULL, 1, 3, '{"resgate_id": 2, "valor": 50}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 3, 'PONTOS_EXPIRANDO', 'Pontos Expirando!', 'Você tem 200 pontos expirando em 30 dias', 'PUSH', 'ENVIADA', CURRENT_TIMESTAMP, NULL, 1, 3, '{"pontos_expirando": 200, "dias_restantes": 30}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
