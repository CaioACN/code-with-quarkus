@echo off
REM =====================================================
REM SCRIPT MASTER COMPLETO DE CONFIGURAÇÃO
REM Sistema de Pontos do Cartão (Quarkus/Java 17)
REM =====================================================

echo.
echo =====================================================
echo CONFIGURACAO COMPLETA DO SISTEMA
echo Sistema de Pontos do Cartao (Quarkus/Java 17)
echo =====================================================
echo.

REM Configurações do banco (ajuste conforme necessário)
set DB_HOST=localhost
set DB_PORT=6543
set DB_NAME=quarkus_social
set DB_USER=postgres
set DB_PASSWORD=postgres

echo Configuracoes do banco:
echo - Host: %DB_HOST%
echo - Porta: %DB_PORT%
echo - Database: %DB_NAME%
echo - Usuario: %DB_USER%
echo.

echo =====================================================
echo PASSO 1: CRIANDO/ATUALIZANDO TABELAS
echo =====================================================
echo.

REM Verificar se o arquivo SQL existe
if not exist "create_all_tables.sql" (
    echo ERRO: Arquivo create_all_tables.sql nao encontrado!
    echo Certifique-se de que o arquivo esta no mesmo diretorio deste script.
    pause
    exit /b 1
)

REM Executar o script de criação
echo Executando script de criacao de tabelas...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f create_all_tables.sql

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERRO: Falha na criacao das tabelas!
    echo Verifique as configuracoes do banco e tente novamente.
    pause
    exit /b 1
)

echo.
echo =====================================================
echo PASSO 2: VERIFICANDO TABELAS CRIADAS
echo =====================================================
echo.

REM Verificar se o arquivo de verificação existe
if not exist "verify_tables.sql" (
    echo AVISO: Arquivo verify_tables.sql nao encontrado!
    echo Pulando verificacao...
    goto :insert_data
)

REM Executar o script de verificação
echo Executando verificacao das tabelas...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f verify_tables.sql

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo AVISO: Falha na verificacao das tabelas!
    echo As tabelas podem ter sido criadas, mas a verificacao falhou.
    echo Continue com a aplicacao para testar.
)

:insert_data
echo.
echo =====================================================
echo PASSO 3: INSERINDO DADOS DE TESTE
echo =====================================================
echo.

REM Verificar se o arquivo de dados de teste existe
if not exist "sample_data.sql" (
    echo AVISO: Arquivo sample_data.sql nao encontrado!
    echo Pulando insercao de dados de teste...
    goto :end
)

REM Perguntar se o usuário quer inserir dados de teste
set /p INSERT_DATA="Deseja inserir dados de teste? (S/N): "
if /i "%INSERT_DATA%" NEQ "S" (
    echo Pulando insercao de dados de teste...
    goto :end
)

REM Executar o script de dados de teste
echo Executando insercao de dados de teste...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f sample_data.sql

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo AVISO: Falha na insercao dos dados de teste!
    echo As tabelas foram criadas, mas os dados de teste nao foram inseridos.
    echo Continue com a aplicacao para testar.
)

:end
echo.
echo =====================================================
echo CONFIGURACAO COMPLETA CONCLUIDA!
echo =====================================================
echo.
echo Resumo da configuracao:
echo - Schema loyalty criado
echo - 10 tabelas criadas/atualizadas
echo - Indices de performance criados
echo - Triggers de auditoria configurados
echo - Dados iniciais inseridos
if "%INSERT_DATA%"=="S" (
    echo - Dados de teste inseridos
    echo   * 5 usuarios de teste
    echo   * 5 cartoes de teste
    echo   * 14 transacoes de teste
    echo   * 14 movimentos de pontos
    echo   * 5 saldos de pontos
    echo   * 3 resgates de teste
    echo   * 5 notificacoes de teste
)
echo.
echo Proximos passos:
echo 1. Execute a aplicacao Quarkus para testar as entidades JPA
echo 2. Teste as APIs com os dados inseridos
echo 3. Configure o Flyway para versionamento futuro
echo 4. Implemente testes unitarios
echo.
echo Para verificar as tabelas novamente, execute: verify_tables.bat
echo Para inserir dados de teste posteriormente, execute: insert_sample_data.bat
echo.
pause
