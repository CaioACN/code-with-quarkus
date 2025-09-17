@echo off
echo =====================================================
echo INSERINDO DADOS NO BANCO POSTGRESQL (DOCKER)
echo Sistema de Pontos do Cartão
echo =====================================================

echo.
echo Verificando se o container PostgreSQL está rodando...
docker ps | findstr quarkus_postgres >nul
if %errorlevel% neq 0 (
    echo ❌ Container PostgreSQL não está rodando!
    echo Execute: docker-compose up -d
    pause
    exit /b 1
)

echo ✅ Container PostgreSQL está rodando!

echo.
echo Inserindo dados no banco...
docker exec -i quarkus_postgres psql -U postgres -d quarkus-social < sample_data_complete.sql

if %errorlevel% equ 0 (
    echo.
    echo ✅ Dados inseridos com sucesso!
    echo.
    echo Verificando dados inseridos...
    docker exec -i quarkus_postgres psql -U postgres -d quarkus-social -c "
    SELECT 'REGRAS CONVERSÃO' as tabela, COUNT(*) as total FROM loyalty.regra_conversao
    UNION ALL
    SELECT 'RECOMPENSAS' as tabela, COUNT(*) as total FROM loyalty.recompensa
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
    echo 🎉 DADOS INSERIDOS COM SUCESSO!
    echo.
    echo 📊 RESUMO:
    echo ✅ Regras de conversão inseridas
    echo ✅ Recompensas inseridas
    echo ✅ Usuários inseridos
    echo ✅ Cartões inseridos
    echo ✅ Transações inseridas
    echo ✅ Movimentos de pontos inseridos
    echo ✅ Saldos inseridos
    echo ✅ Resgates inseridos
    echo ✅ Notificações inseridas
    echo.
    echo 🌐 Acesse a aplicação:
    echo Backend: http://localhost:8080
    echo Frontend: http://localhost:4200
    echo Swagger UI: http://localhost:8080/q/swagger-ui
    echo.
) else (
    echo ❌ Erro ao inserir dados!
    echo Verifique se o banco está rodando e acessível.
)

pause
