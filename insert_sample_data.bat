@echo off
REM =====================================================
REM SCRIPT PARA INSERIR DADOS DE TESTE
REM Sistema de Pontos do Cartão (Quarkus/Java 17)
REM =====================================================

echo.
echo =====================================================
echo INSERINDO DADOS DE TESTE
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
if not exist "sample_data.sql" (
    echo ERRO: Arquivo sample_data.sql nao encontrado!
    echo Certifique-se de que o arquivo esta no mesmo diretorio deste script.
    pause
    exit /b 1
)

echo Inserindo dados de teste...
echo.

REM Executar o script de dados de teste
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f sample_data.sql

REM Verificar se a execução foi bem-sucedida
if %ERRORLEVEL% EQU 0 (
    echo.
    echo =====================================================
    echo SUCESSO: Dados de teste inseridos com sucesso!
    echo.
    echo Dados inseridos:
    echo - 5 usuarios de teste
    echo - 5 cartoes de teste
    echo - 14 transacoes de teste
    echo - 14 movimentos de pontos
    echo - 5 saldos de pontos
    echo - 3 resgates de teste
    echo - 5 notificacoes de teste
    echo.
    echo Agora voce pode testar a aplicacao com dados reais!
    echo =====================================================
) else (
    echo.
    echo =====================================================
    echo ERRO: Falha na insercao dos dados de teste!
    echo Verifique as configuracoes do banco e tente novamente.
    echo =====================================================
)

echo.
pause
