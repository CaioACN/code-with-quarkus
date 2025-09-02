#!/bin/bash

# =====================================================
# SCRIPT PARA INSERIR DADOS DE TESTE
# Sistema de Pontos do Cartão (Quarkus/Java 17)
# =====================================================

echo ""
echo "====================================================="
echo "INSERINDO DADOS DE TESTE"
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
if [ ! -f "sample_data.sql" ]; then
    echo "ERRO: Arquivo sample_data.sql não encontrado!"
    echo "Certifique-se de que o arquivo está no mesmo diretório deste script."
    exit 1
fi

echo "Inserindo dados de teste..."
echo ""

# Executar o script de dados de teste
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f sample_data.sql

# Verificar se a execução foi bem-sucedida
if [ $? -eq 0 ]; then
    echo ""
    echo "====================================================="
    echo "SUCESSO: Dados de teste inseridos com sucesso!"
    echo ""
    echo "Dados inseridos:"
    echo "- 5 usuários de teste"
    echo "- 5 cartões de teste"
    echo "- 14 transações de teste"
    echo "- 14 movimentos de pontos"
    echo "- 5 saldos de pontos"
    echo "- 3 resgates de teste"
    echo "- 5 notificações de teste"
    echo ""
    echo "Agora você pode testar a aplicação com dados reais!"
    echo "====================================================="
else
    echo ""
    echo "====================================================="
    echo "ERRO: Falha na inserção dos dados de teste!"
    echo "Verifique as configurações do banco e tente novamente."
    echo "====================================================="
    exit 1
fi

echo ""
