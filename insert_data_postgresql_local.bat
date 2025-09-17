@echo off
echo =====================================================
echo INSERINDO DADOS NO POSTGRESQL LOCAL
echo Sistema de Pontos do Cartão
echo =====================================================
echo.

echo Verificando se o PostgreSQL está rodando localmente...
psql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Erro: PostgreSQL não está instalado ou não está no PATH.
    echo Por favor, instale o PostgreSQL e adicione ao PATH.
    goto :eof
)
echo ✅ PostgreSQL encontrado!
echo.

echo Conectando ao banco PostgreSQL local...
echo Usuário: postgres
echo Banco: quarkus-social
echo.

echo Executando script SQL...
psql -h localhost -U postgres -d quarkus-social -f insert_postgresql_local.sql
if %errorlevel% neq 0 (
    echo Erro ao executar script SQL no PostgreSQL local.
    echo Verifique se:
    echo 1. PostgreSQL está rodando
    echo 2. Banco 'quarkus-social' existe
    echo 3. Usuário 'postgres' tem permissões
    goto :eof
)
echo ✅ Dados inseridos com sucesso no PostgreSQL local!
echo.

echo Verificando dados inseridos...
psql -h localhost -U postgres -d quarkus-social -c "SELECT 'USUÁRIOS' as tabela, COUNT(*) as total FROM loyalty.usuario UNION ALL SELECT 'CARTÕES' as tabela, COUNT(*) as total FROM loyalty.cartao UNION ALL SELECT 'TRANSAÇÕES' as tabela, COUNT(*) as total FROM loyalty.transacao UNION ALL SELECT 'MOVIMENTOS' as tabela, COUNT(*) as total FROM loyalty.movimento_pontos UNION ALL SELECT 'SALDOS' as tabela, COUNT(*) as total FROM loyalty.saldo_pontos UNION ALL SELECT 'RESGATES' as tabela, COUNT(*) as total FROM loyalty.resgate UNION ALL SELECT 'NOTIFICAÇÕES' as tabela, COUNT(*) as total FROM loyalty.notificacao UNION ALL SELECT 'RECOMPENSAS' as tabela, COUNT(*) as total FROM loyalty.recompensa UNION ALL SELECT 'REGRAS CONVERSÃO' as tabela, COUNT(*) as total FROM loyalty.regra_conversao UNION ALL SELECT 'CAMPANHAS BÔNUS' as tabela, COUNT(*) as total FROM loyalty.campanha_bonus ORDER BY tabela;"

echo.
echo =====================================================
echo DADOS INSERIDOS COM SUCESSO NO POSTGRESQL LOCAL!
echo =====================================================
echo.
echo Para verificar os dados, acesse:
echo - Dashboard: http://localhost:8081/admin/dashboard
echo - Swagger UI: http://localhost:8081/q/swagger-ui
echo.
pause





