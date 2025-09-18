# Configuração do Banco de Dados

## Visão Geral
O sistema utiliza PostgreSQL 15 como banco de dados principal, executado em container Docker.

## Configuração

### Banco de Dados
- **SGBD**: PostgreSQL 15
- **Nome do Banco**: `quarkus-social`
- **Usuário**: `postgres`
- **Senha**: `postgres`
- **Porta**: `5432`
- **Host**: `postgres` (dentro do Docker) / `localhost` (acesso externo)

### Schema Principal
- **Nome**: `loyalty`
- **Descrição**: Schema principal do sistema de pontos de fidelidade

## Estrutura das Tabelas

### Tabela: usuario
- **id**: BIGINT (PK, AUTO_INCREMENT)
- **nome**: VARCHAR(100) NOT NULL
- **email**: VARCHAR(150) UNIQUE NOT NULL
- **telefone**: VARCHAR(20)
- **data_nascimento**: DATE
- **data_cadastro**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- **ativo**: BOOLEAN DEFAULT TRUE

### Tabela: cartao
- **id**: BIGINT (PK, AUTO_INCREMENT)
- **numero**: VARCHAR(16) UNIQUE NOT NULL
- **usuario_id**: BIGINT (FK → usuario.id)
- **saldo_pontos**: INTEGER DEFAULT 0
- **data_emissao**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP
- **ativo**: BOOLEAN DEFAULT TRUE

### Tabela: transacao
- **id**: BIGINT (PK, AUTO_INCREMENT)
- **cartao_id**: BIGINT (FK → cartao.id)
- **tipo**: VARCHAR(20) NOT NULL (CHECK: 'CREDITO' ou 'DEBITO')
- **pontos**: INTEGER NOT NULL
- **descricao**: VARCHAR(255)
- **data_transacao**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP

### Tabela: configuracao_notificacao
- **id**: BIGINT (PK, AUTO_INCREMENT)
- **usuario_id**: BIGINT (FK → usuario.id)
- **email_ativo**: BOOLEAN DEFAULT TRUE
- **sms_ativo**: BOOLEAN DEFAULT FALSE
- **push_ativo**: BOOLEAN DEFAULT TRUE
- **data_atualizacao**: TIMESTAMP DEFAULT CURRENT_TIMESTAMP

## Migrações Flyway

O sistema utiliza Flyway para controle de versão do banco de dados. As migrações estão localizadas em:
`src/main/resources/db/migration/`

### Histórico de Migrações
1. **V1__init.sql**: Criação inicial do schema e tabelas principais
2. **V2__add_missing_sequences.sql**: Adição de sequences faltantes
3. **V3__fix_sequence_increment.sql**: Correção do incremento das sequences
4. **V4__sync_sequences.sql**: Sincronização das sequences com dados existentes
5. **V5__create_configuracao_notificacao.sql**: Criação da tabela de configuração de notificações

## Configuração da Aplicação

### Arquivo: application-docker.properties
```properties
# PostgreSQL Configuration
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://postgres:5432/quarkus-social
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres

# Flyway Configuration
quarkus.flyway.migrate-at-start=true
quarkus.flyway.baseline-on-migrate=true
quarkus.flyway.schemas=loyalty
```

## Inicialização

O banco é inicializado automaticamente via Docker Compose com:
- Criação do banco `quarkus-social`
- Execução do script `init-db.sql`
- Aplicação automática das migrações Flyway

## Comandos Úteis

### Conectar ao banco via Docker
```bash
docker exec -it postgres psql -U postgres -d quarkus-social
```

### Verificar tabelas do schema loyalty
```sql
\dt loyalty.*
```

### Verificar status das migrações Flyway
```sql
SELECT * FROM loyalty.flyway_schema_history;
```

## Backup e Restore

### Backup
```bash
docker exec postgres pg_dump -U postgres quarkus-social > backup.sql
```

### Restore
```bash
docker exec -i postgres psql -U postgres quarkus-social < backup.sql
```