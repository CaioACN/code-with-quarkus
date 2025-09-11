@echo off
echo Iniciando a aplicacao com Docker Compose...

REM Verifica se o Docker esta em execucao
docker info > nul 2>&1
if %errorlevel% neq 0 (
    echo Erro: Docker nao esta em execucao. Por favor, inicie o Docker Desktop.
    exit /b 1
)

REM Constroi e inicia os containers
echo Construindo e iniciando os containers...
docker-compose up --build -d

if %errorlevel% neq 0 (
    echo Erro ao iniciar os containers. Verifique os logs para mais detalhes.
    exit /b 1
)

echo.
echo Aplicacao iniciada com sucesso!
echo.
echo Frontend: http://localhost
echo Backend: http://localhost:8080
echo Swagger UI: http://localhost:8080/q/swagger-ui
echo.
echo Para parar a aplicacao, execute: docker-compose down
echo Para ver os logs, execute: docker-compose logs -f