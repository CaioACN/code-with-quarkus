#!/bin/bash

# =====================================================
# SCRIPT MASTER DE CONFIGURAÇÃO DO BANCO
# Sistema de Pontos do Cartão (Quarkus/Java 17)
# =====================================================

echo ""
echo "====================================================="
echo "CONFIGURAÇÃO COMPLETA DO BANCO DE DADOS"
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

echo "====================================================="
echo "PASSO 1: CRIANDO/ATUALIZANDO TABELAS"
echo "====================================================="
echo ""

# Verificar se o arquivo SQL existe
if [ ! -f "create_all_tables.sql" ]; then
    echo "ERRO: Arquivo create_all_tables.sql não encontrado!"
    echo "Certifique-se de que o arquivo está no mesmo diretório deste script."
    exit 1
fi

# Executar o script de criação
echo "Executando script de criação de tabelas..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f create_all_tables.sql

if [ $? -ne 0 ]; then
    echo ""
    echo "ERRO: Falha na criação das tabelas!"
    echo "Verifique as configurações do banco e tente novamente."
    exit 1
fi

echo ""
echo "====================================================="
echo "PASSO 2: VERIFICANDO TABELAS CRIADAS"
echo "====================================================="
echo ""

# Verificar se o arquivo de verificação existe
if [ ! -f "verify_tables.sql" ]; then
    echo "AVISO: Arquivo verify_tables.sql não encontrado!"
    echo "Pulando verificação..."
    exit 0
fi

# Executar o script de verificação
echo "Executando verificação das tabelas..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f verify_tables.sql

if [ $? -ne 0 ]; then
    echo ""
    echo "AVISO: Falha na verificação das tabelas!"
    echo "As tabelas podem ter sido criadas, mas a verificação falhou."
    echo "Continue com a aplicação para testar."
fi

echo ""
echo "====================================================="
echo "CONFIGURAÇÃO CONCLUÍDA!"
echo "====================================================="
echo ""
echo "Próximos passos:"
echo "1. Verifique os logs acima para confirmar que tudo foi criado corretamente"
echo "2. Execute a aplicação Quarkus para testar as entidades JPA"
echo "3. Insira dados de teste para validar o funcionamento"
echo "4. Configure o Flyway para versionamento futuro"
echo ""
echo "Para verificar as tabelas novamente, execute: ./verify_tables.sh"
echo ""
