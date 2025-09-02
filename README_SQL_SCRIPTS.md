# Scripts de Criação/Atualização de Tabelas

Este diretório contém scripts para criar e atualizar todas as tabelas do sistema de pontos do cartão.

## Arquivos Disponíveis

### 1. `create_all_tables.sql`
Script SQL principal que cria/atualiza todas as tabelas do sistema:
- **Tabelas do Contexto Social** (baseline): `users`, `posts`, `followers`
- **Tabelas do Contexto Loyalty** (existentes): `usuario`, `cartao`
- **Tabelas do Contexto Loyalty** (novas): `transacao`, `regra_conversao`, `campanha_bonus`, `movimento_pontos`, `saldo_pontos`, `recompensa`, `resgate`, `notificacao`

### 2. `verify_tables.sql`
Script SQL de verificação que valida se todas as tabelas foram criadas corretamente e mostra estatísticas.

### 3. `execute_sql.bat` / `execute_sql.sh`
Scripts de execução que executam o `create_all_tables.sql` automaticamente.

### 4. `verify_tables.bat` / `verify_tables.sh`
Scripts de verificação que executam o `verify_tables.sql` automaticamente.

### 5. `setup_database.bat` / `setup_database.sh`
Scripts master que executam criação e verificação em sequência (recomendado).

### 6. `sample_data.sql`
Script SQL com dados de teste para popular o sistema com informações de exemplo.

### 7. `insert_sample_data.bat` / `insert_sample_data.sh`
Scripts para inserir os dados de teste automaticamente.

### 8. `setup_complete.bat` / `setup_complete.sh`
Scripts master completos que executam criação, verificação e inserção de dados de teste em sequência (mais recomendado).

## Como Usar

### Opção 1: Execução Master Completa (Mais Recomendada)

#### Windows:
```cmd
setup_complete.bat
```

#### Linux/Mac:
```bash
# Tornar o script executável (apenas na primeira vez)
chmod +x setup_complete.sh

# Executar o script
./setup_complete.sh
```

### Opção 2: Execução Master Básica

#### Windows:
```cmd
setup_database.bat
```

#### Linux/Mac:
```bash
# Tornar o script executável (apenas na primeira vez)
chmod +x setup_database.sh

# Executar o script
./setup_database.sh
```

### Opção 3: Execução Separada

#### Windows:
```cmd
# Criar/atualizar tabelas
execute_sql.bat

# Verificar tabelas criadas
verify_tables.bat
```

#### Linux/Mac:
```bash
# Tornar os scripts executáveis (apenas na primeira vez)
chmod +x execute_sql.sh verify_tables.sh

# Criar/atualizar tabelas
./execute_sql.sh

# Verificar tabelas criadas
./verify_tables.sh
```

### Opção 4: Execução Manual

#### Windows (PowerShell):
```powershell
psql -h localhost -p 6543 -U postgres -d quarkus_social -f create_all_tables.sql
```

#### Linux/Mac:
```bash
PGPASSWORD=postgres psql -h localhost -p 6543 -U postgres -d quarkus_social -f create_all_tables.sql
```

## Configurações do Banco

Os scripts estão configurados para conectar ao banco com as seguintes configurações padrão:

- **Host**: localhost
- **Porta**: 6543
- **Database**: quarkus_social
- **Usuário**: postgres
- **Senha**: postgres

### Personalizando as Configurações

Para usar configurações diferentes, edite os arquivos de script:

#### Windows (`execute_sql.bat`):
```batch
set DB_HOST=seu_host
set DB_PORT=sua_porta
set DB_NAME=seu_database
set DB_USER=seu_usuario
set DB_PASSWORD=sua_senha
```

#### Linux/Mac (`execute_sql.sh`):
```bash
DB_HOST="seu_host"
DB_PORT="sua_porta"
DB_NAME="seu_database"
DB_USER="seu_usuario"
DB_PASSWORD="sua_senha"
```

## O que o Script Faz

### 1. Criação do Schema
- Cria o schema `loyalty` se não existir

### 2. Criação/Atualização de Tabelas
- **Tabelas Existentes**: Verifica e atualiza `usuario` e `cartao` conforme necessário
- **Tabelas Novas**: Cria todas as tabelas do sistema de pontos

### 3. Constraints e Validações
- Adiciona todas as foreign keys necessárias
- Cria constraints de validação (valores positivos, enums, etc.)
- Configura cascatas de exclusão apropriadas

### 4. Índices de Performance
- Cria índices otimizados para consultas frequentes
- Índices compostos para melhor performance

### 5. Triggers de Auditoria
- Triggers para atualizar campos `atualizado_em` automaticamente

### 6. Dados Iniciais
- Insere regras de conversão básicas
- Insere campanhas de bônus de exemplo
- Insere recompensas básicas

### 7. Verificação Final
- Verifica se todas as tabelas foram criadas com sucesso
- Exibe relatório de status

## Estrutura das Tabelas

### Tabelas do Contexto Social (Baseline)
- `users`: Usuários do contexto social
- `posts`: Posts dos usuários
- `followers`: Relacionamentos de seguidores

### Tabelas do Contexto Loyalty

#### Existentes:
- `usuario`: Usuários do sistema de fidelidade
- `cartao`: Cartões de crédito dos usuários

#### Novas:
- `transacao`: Transações financeiras que geram pontos
- `regra_conversao`: Regras para conversão de transações em pontos
- `campanha_bonus`: Campanhas de bônus temporárias
- `movimento_pontos`: Movimentações de pontos (acúmulo, expiração, resgate, estorno)
- `saldo_pontos`: Saldo atual de pontos por usuário/cartão
- `recompensa`: Catálogo de recompensas disponíveis para resgate
- `resgate`: Solicitações de resgate de pontos por recompensas
- `notificacao`: Notificações enviadas aos usuários

## Tratamento de Erros

O script é projetado para ser **idempotente**, ou seja:
- Pode ser executado múltiplas vezes sem causar erros
- Tabelas existentes são atualizadas conforme necessário
- Novas tabelas são criadas apenas se não existirem
- Constraints são adicionadas apenas se não existirem

## Logs e Verificação

O script fornece logs detalhados durante a execução:
- Confirmação de criação de cada tabela
- Verificação de constraints e índices
- Relatório final de status
- Contagem de tabelas criadas/atualizadas

## Troubleshooting

### Erro de Conexão
- Verifique se o PostgreSQL está rodando
- Confirme as configurações de host, porta, usuário e senha
- Teste a conexão manualmente com `psql`

### Erro de Permissões
- Certifique-se de que o usuário tem permissões para criar tabelas
- Verifique se o usuário tem acesso ao schema `loyalty`

### Erro de Schema
- O script cria o schema `loyalty` automaticamente
- Se houver conflitos, verifique se não há objetos existentes com nomes similares

## Dados de Teste

Para testar o sistema com dados reais, você pode inserir dados de exemplo:

### Inserir Dados de Teste

#### Windows:
```cmd
insert_sample_data.bat
```

#### Linux/Mac:
```bash
# Tornar o script executável (apenas na primeira vez)
chmod +x insert_sample_data.sh

# Executar o script
./insert_sample_data.sh
```

### Dados Incluídos

O script de dados de teste inclui:
- **5 usuários** com informações completas
- **5 cartões** vinculados aos usuários
- **14 transações** de diferentes categorias (restaurantes, supermercados, postos)
- **14 movimentos de pontos** baseados nas transações
- **5 saldos de pontos** calculados
- **3 resgates** em diferentes status
- **5 notificações** de exemplo

## Próximos Passos

Após executar o script com sucesso:

1. **Verificar as Tabelas**: Conecte ao banco e verifique se todas as tabelas foram criadas
2. **Inserir Dados de Teste**: Execute o script de dados de teste para popular o sistema
3. **Testar a Aplicação**: Execute a aplicação Quarkus para verificar se as entidades JPA funcionam corretamente
4. **Validar Funcionamento**: Teste as APIs com os dados de exemplo inseridos
5. **Configurar Flyway**: Configure o Flyway para versionamento futuro do banco

## Suporte

Para dúvidas ou problemas:
1. Verifique os logs de execução do script
2. Consulte a documentação do PostgreSQL
3. Verifique as configurações da aplicação Quarkus
4. Teste a conectividade com o banco de dados
