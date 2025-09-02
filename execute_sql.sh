#!/bin/bash

# =====================================================
# SCRIPT DE EXECUÇÃO SQL PARA LINUX/MAC
# Sistema de Pontos do Cartão (Quarkus/Java 17)
# =====================================================

echo ""
echo "====================================================="
echo "EXECUTANDO SCRIPT DE CRIAÇÃO/ATUALIZAÇÃO DE TABELAS"
echo "Sistema de Pontos do Cartão (Quarkus/Java 17)"
echo "====================================================="
echo ""

# Configurações do banco (ajuste conforme necessário)
DB_HOST="localhost"
DB_PORT="6543"
DB_NAME="quarkus_social"
DB_USER="postgres"
DB_PASSWORD="postgres"

echo "Configurações do banco:"
echo "- Host: $DB_HOST"
echo "- Porta: $DB_PORT"
echo "- Database: $DB_NAME"
echo "- Usuário: $DB_USER"
echo ""

# Verificar se o arquivo SQL existe
if [ ! -f "create_all_tables.sql" ]; then
    echo "ERRO: Arquivo create_all_tables.sql não encontrado!"
    echo "Certifique-se de que o arquivo está no mesmo diretório deste script."
    exit 1
fi

echo "Executando script SQL..."
echo ""

# Executar o script SQL
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f create_all_tables.sql

# Verificar se a execução foi bem-sucedida
if [ $? -eq 0 ]; then
    echo ""
    echo "====================================================="
    echo "SUCESSO: Script executado com sucesso!"
    echo "Todas as tabelas foram criadas/atualizadas."
    echo "====================================================="
else
    echo ""
    echo "====================================================="
    echo "ERRO: Falha na execução do script SQL!"
    echo "Verifique as configurações do banco e tente novamente."
    echo "====================================================="
    exit 1
fi

echo ""
