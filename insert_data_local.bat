@echo off
echo =====================================================
echo INSERINDO DADOS NO POSTGRESQL (DESENVOLVIMENTO LOCAL)
echo Sistema de Pontos do Cartão
echo =====================================================

echo.
echo Verificando se a aplicação está rodando...
curl -s http://localhost:8080/q/health >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Aplicação não está rodando na porta 8080!
    echo Execute primeiro: cd apps\backend && .\mvnw.cmd quarkus:dev -Dquarkus.profile=docker
    pause
    exit /b 1
)

echo ✅ Aplicação está rodando!

echo.
echo ⚠️ IMPORTANTE: Este script é para referência apenas.
echo Os dados devem ser inseridos usando o script insert_data_docker.bat
echo que funciona tanto para Docker quanto para desenvolvimento local.
echo.
echo 📊 DADOS DISPONÍVEIS:
echo ✅ Regras de conversão
echo ✅ Recompensas
echo ✅ Usuários de teste
echo ✅ Cartões de teste
echo ✅ Transações de teste
echo ✅ Movimentos de pontos
echo ✅ Saldos de pontos
echo ✅ Resgates de teste
echo ✅ Notificações de teste
echo.
echo 🌐 Acesse a aplicação:
echo Backend: http://localhost:8080
echo Swagger UI: http://localhost:8080/q/swagger-ui
echo PostgreSQL: localhost:6543
echo.
echo 🔍 Para verificar os dados, acesse:
echo - Dashboard: http://localhost:8080/admin/dashboard
echo - Health Check: http://localhost:8080/q/health
echo.
echo 💡 Para inserir dados, execute: .\insert_data_docker.bat
echo.

pause
