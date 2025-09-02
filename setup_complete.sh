#!/bin/bash

# =====================================================
# SCRIPT MASTER COMPLETO DE CONFIGURAÇÃO
# Sistema de Pontos do Cartão (Quarkus/Java 17)
# =====================================================

echo ""
echo "====================================================="
echo "CONFIGURAÇÃO COMPLETA DO SISTEMA"
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
else
    # Executar o script de verificação
    echo "Executando verificação das tabelas..."
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f verify_tables.sql
    
    if [ $? -ne 0 ]; then
        echo ""
        echo "AVISO: Falha na verificação das tabelas!"
        echo "As tabelas podem ter sido criadas, mas a verificação falhou."
        echo "Continue com a aplicação para testar."
    fi
fi

echo ""
echo "====================================================="
echo "PASSO 3: INSERINDO DADOS DE TESTE"
echo "====================================================="
echo ""

# Verificar se o arquivo de dados de teste existe
if [ ! -f "sample_data.sql" ]; then
    echo "AVISO: Arquivo sample_data.sql não encontrado!"
    echo "Pulando inserção de dados de teste..."
else
    # Perguntar se o usuário quer inserir dados de teste
    echo -n "Deseja inserir dados de teste? (S/N): "
    read -r INSERT_DATA
    
    if [ "$INSERT_DATA" = "S" ] || [ "$INSERT_DATA" = "s" ]; then
        # Executar o script de dados de teste
        echo "Executando inserção de dados de teste..."
        PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f sample_data.sql
        
        if [ $? -ne 0 ]; then
            echo ""
            echo "AVISO: Falha na inserção dos dados de teste!"
            echo "As tabelas foram criadas, mas os dados de teste não foram inseridos."
            echo "Continue com a aplicação para testar."
        fi
    else
        echo "Pulando inserção de dados de teste..."
    fi
fi

echo ""
echo "====================================================="
echo "CONFIGURAÇÃO COMPLETA CONCLUÍDA!"
echo "====================================================="
echo ""
echo "Resumo da configuração:"
echo "- Schema loyalty criado"
echo "- 10 tabelas criadas/atualizadas"
echo "- Índices de performance criados"
echo "- Triggers de auditoria configurados"
echo "- Dados iniciais inseridos"
if [ "$INSERT_DATA" = "S" ] || [ "$INSERT_DATA" = "s" ]; then
    echo "- Dados de teste inseridos"
    echo "  * 5 usuários de teste"
    echo "  * 5 cartões de teste"
    echo "  * 14 transações de teste"
    echo "  * 14 movimentos de pontos"
    echo "  * 5 saldos de pontos"
    echo "  * 3 resgates de teste"
    echo "  * 5 notificações de teste"
fi
echo ""
echo "Próximos passos:"
echo "1. Execute a aplicação Quarkus para testar as entidades JPA"
echo "2. Teste as APIs com os dados inseridos"
echo "3. Configure o Flyway para versionamento futuro"
echo "4. Implemente testes unitários"
echo ""
echo "Para verificar as tabelas novamente, execute: ./verify_tables.sh"
echo "Para inserir dados de teste posteriormente, execute: ./insert_sample_data.sh"
echo ""
