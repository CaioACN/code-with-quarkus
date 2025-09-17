@echo off
echo =====================================================
echo VERIFICANDO DADOS NO BANCO
echo Sistema de Pontos do Cartão
echo =====================================================

echo.
echo Verificando dados no PostgreSQL...
echo Ambiente: Docker ou Local (porta 8080)
echo.

echo.
echo Verificando dados no PostgreSQL...
docker exec -i quarkus_postgres psql -U postgres -d quarkus-social -c "
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
"
echo.
echo Mostrando saldos por usuário:
docker exec -i quarkus_postgres psql -U postgres -d quarkus-social -c "
SELECT 
    u.nome as usuario,
    c.numero as cartao,
    sp.saldo as pontos_saldo,
    sp.atualizado_em as ultima_atualizacao
FROM loyalty.saldo_pontos sp
JOIN loyalty.usuario u ON sp.usuario_id = u.id
JOIN loyalty.cartao c ON sp.cartao_id = c.id
ORDER BY sp.saldo DESC;
"
echo.
echo Testando endpoints da aplicação:
echo.
echo 1. Health Check:
curl -s http://localhost:8080/q/health
echo.
echo.
echo 2. Dashboard:
curl -s http://localhost:8080/admin/dashboard
echo.
echo.
echo 3. Hello World:
curl -s http://localhost:8080/hello
echo.
echo.
echo Para conectar ao PostgreSQL:
echo 1. Host: localhost
echo 2. Porta: 6543
echo 3. Database: quarkus-social
echo 4. Schema: loyalty
echo 5. Username: postgres
echo 6. Password: postgres
echo.

:END
echo.
echo ✅ Verificação concluída!
pause

