@echo off
echo ========================================
echo    SISTEMA DE PONTOS - START LOCAL
echo ========================================
echo.

echo Escolha uma opção:
echo 1. Docker (Recomendado - PostgreSQL)
echo 2. Desenvolvimento Local (PostgreSQL)
echo.

set /p choice="Digite sua escolha (1 ou 2): "

if "%choice%"=="1" goto docker
if "%choice%"=="2" goto local
goto invalid

:docker
echo.
echo 🚀 Iniciando com Docker...
docker compose up -d
echo.
echo ✅ Aplicação iniciada com Docker!
echo 🌐 Backend: http://localhost:8080
echo 🌐 Frontend: http://localhost:4200
echo 🌐 Swagger: http://localhost:8080/q/swagger-ui
echo 🗄️ PostgreSQL: localhost:6543
echo.
goto end

:local
echo.
echo 🚀 Iniciando desenvolvimento local...
cd apps\backend
echo Compilando com PostgreSQL...
call mvnw.cmd clean package -DskipTests -Pdocker
echo Iniciando aplicação...
call mvnw.cmd quarkus:dev -Dquarkus.profile=docker
echo.
echo ✅ Aplicação iniciada localmente!
echo 🌐 Backend: http://localhost:8080
echo 🌐 Swagger: http://localhost:8080/q/swagger-ui
echo 🗄️ PostgreSQL: localhost:6543
echo.
goto end

:invalid
echo.
echo ❌ Opção inválida!
goto end

:end
echo.
echo Pressione qualquer tecla para sair...
pause >nul

