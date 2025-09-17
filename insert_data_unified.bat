@echo off
echo =====================================================
echo INSERÇÃO UNIFICADA DE DADOS
echo Sistema de Pontos do Cartão
echo =====================================================
echo.

echo Escolha o ambiente para inserir dados:
echo 1. Docker (PostgreSQL em container)
echo 2. PostgreSQL Local
echo 3. H2 Local (desenvolvimento)
echo.

set /p choice="Digite sua escolha (1, 2 ou 3): "

if "%choice%"=="1" (
    echo.
    echo Executando inserção no Docker...
    call .\insert_data_docker.bat
) else if "%choice%"=="2" (
    echo.
    echo Executando inserção no PostgreSQL Local...
    call .\insert_data_postgresql_local.bat
) else if "%choice%"=="3" (
    echo.
    echo Executando inserção no H2 Local...
    call .\insert_data_local.bat
) else (
    echo.
    echo Opção inválida. Por favor, escolha 1, 2 ou 3.
    goto :eof
)

echo.
echo =====================================================
echo INSERÇÃO CONCLUÍDA!
echo =====================================================
pause





