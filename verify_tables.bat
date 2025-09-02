@echo off
REM =====================================================
REM SCRIPT DE VERIFICAÇÃO DAS TABELAS CRIADAS
REM Sistema de Pontos do Cartão (Quarkus/Java 17)
REM =====================================================

echo.
echo =====================================================
echo VERIFICANDO TABELAS CRIADAS
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
if not exist "verify_tables.sql" (
    echo ERRO: Arquivo verify_tables.sql nao encontrado!
    echo Certifique-se de que o arquivo esta no mesmo diretorio deste script.
    pause
    exit /b 1
)

echo Executando verificacao das tabelas...
echo.

REM Executar o script de verificação
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f verify_tables.sql

REM Verificar se a execução foi bem-sucedida
if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================
    echo VERIFICACAO CONCLUIDA COM SUCESSO!
    echo Verifique os resultados acima para confirmar que todas as tabelas foram criadas corretamente.
    echo =====================================================
) else (
    echo.
    echo =====================================================
    echo ERRO: Falha na verificacao das tabelas!
    echo Verifique as configuracoes do banco e tente novamente.
    echo =====================================================
)

echo.
pause
