-- Dados de teste para o sistema de pontos
INSERT INTO loyalty.usuario (id, nome, email, data_cadastro) VALUES
(1, 'João Silva', 'joao.silva@email.com', CURRENT_DATE),
(2, 'Maria Santos', 'maria.santos@email.com', CURRENT_DATE),
(3, 'Pedro Oliveira', 'pedro.oliveira@email.com', CURRENT_DATE);

INSERT INTO loyalty.cartao (id, numero, nome_impresso, validade, limite, id_usuario) VALUES
(1, '1234567890123456', 'JOAO SILVA', '2027-12-31', 5000.00, 1),
(2, '1111222233334444', 'MARIA SANTOS', '2027-12-31', 8000.00, 2),
(3, '5555666677778888', 'PEDRO OLIVEIRA', '2027-12-31', 10000.00, 3);

INSERT INTO loyalty.saldo_pontos (usuario_id, cartao_id, saldo, atualizado_em, pontos_expirando_30_dias, pontos_expirando_60_dias, pontos_expirando_90_dias) VALUES
(1, 1, 15000, CURRENT_TIMESTAMP, 1000, 2000, 2000),
(2, 2, 25000, CURRENT_TIMESTAMP, 2000, 3000, 5000),
(3, 3, 12000, CURRENT_TIMESTAMP, 800, 1200, 2000);

INSERT INTO loyalty.recompensa (id, tipo, descricao, custo_pontos, estoque, parceiro_id, ativo, detalhes, imagem_url, validade_recompensa, criado_em, atualizado_em) VALUES
(1, 'PRODUTO', 'Fone Bluetooth Premium', 5000, 10, 1, true, 'Fone com cancelamento de ruído', 'https://example.com/fone.jpg', '2025-12-31 23:59:59', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'MILHAS', '10.000 Milhas Aéreas', 10000, 5, 2, true, 'Milhas para qualquer destino nacional', 'https://example.com/milhas.jpg', '2025-12-31 23:59:59', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 'GIFT', 'Vale Presente R$ 50', 2500, 20, 3, true, 'Vale presente para lojas parceiras', 'https://example.com/gift.jpg', '2025-12-31 23:59:59', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);