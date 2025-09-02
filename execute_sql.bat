@echo off
REM =====================================================
REM SCRIPT DE EXECUÇÃO SQL PARA WINDOWS
REM Sistema de Pontos do Cartão (Quarkus/Java 17)
REM =====================================================

echo.
echo =====================================================
echo EXECUTANDO SCRIPT DE CRIACAO/ATUALIZACAO DE TABELAS
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

REM Verificar se o arquivo SQL existe
if not exist "create_all_tables.sql" (
    echo ERRO: Arquivo create_all_tables.sql nao encontrado!
    echo Certifique-se de que o arquivo esta no mesmo diretorio deste script.
    pause
    exit /b 1
)

echo Executando script SQL...
echo.

REM Executar o script SQL
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f create_all_tables.sql

REM Verificar se a execução foi bem-sucedida
if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================
    echo SUCESSO: Script executado com sucesso!
    echo Todas as tabelas foram criadas/atualizadas.
    echo =====================================================
) else (
    echo.
    echo =====================================================
    echo ERRO: Falha na execucao do script SQL!
    echo Verifique as configuracoes do banco e tente novamente.
    echo =====================================================
)

echo.
pause
