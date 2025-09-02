@echo off
REM =====================================================
REM SCRIPT MASTER DE CONFIGURAÇÃO DO BANCO
REM Sistema de Pontos do Cartão (Quarkus/Java 17)
REM =====================================================

echo.
echo =====================================================
echo CONFIGURACAO COMPLETA DO BANCO DE DADOS
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
    goto :end
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

:end
echo.
echo =====================================================
echo CONFIGURACAO CONCLUIDA!
echo =====================================================
echo.
echo Próximos passos:
echo 1. Verifique os logs acima para confirmar que tudo foi criado corretamente
echo 2. Execute a aplicacao Quarkus para testar as entidades JPA
echo 3. Insira dados de teste para validar o funcionamento
echo 4. Configure o Flyway para versionamento futuro
echo.
echo Para verificar as tabelas novamente, execute: verify_tables.bat
echo.
pause
