# Sistema de Fidelidade por Pontos

Sistema completo de fidelidade por pontos para cartões de crédito, permitindo acúmulo, campanhas, expiração e resgate de pontos, com extrato e notificações.

## 📋 Descrição do Sistema

Este projeto implementa um sistema robusto de fidelidade que oferece:

- **Acúmulo de Pontos**: Pontos creditados automaticamente em transações
- **Campanhas e Regras**: Sistema flexível de campanhas de bônus
- **Expiração Automática**: Controle de validade dos pontos
- **Resgate de Recompensas**: Catálogo de produtos e serviços
- **Extrato Detalhado**: Histórico completo de movimentações
- **Notificações**: Sistema de alertas e comunicações
- **Dashboard Administrativo**: Métricas e relatórios em tempo real

## 🏗️ Arquitetura

### Stack Tecnológica

**Backend**
- **Quarkus** (Java 21) - Framework reativo
- **PostgreSQL** - Banco de dados relacional
- **Flyway** - Versionamento do banco
- **OpenAPI/Swagger** - Documentação da API
- **JAX-RS** - APIs REST
- **Hibernate ORM** - Mapeamento objeto-relacional

**Frontend**
- **Angular 18** - Framework SPA
- **TypeScript** - Linguagem tipada
- **SCSS** - Estilização avançada
- **RxJS** - Programação reativa

**Infraestrutura**
- **Docker** - Containerização
- **Docker Compose** - Orquestração
- **Nginx** - Proxy reverso (produção)

## 🗄️ Principais Entidades

### Domínio de Usuários
- **Usuario**: Dados pessoais e identificação
- **Cartao**: Cartões de crédito vinculados
- **SaldoPontos**: Saldo atual de pontos por cartão

### Domínio de Transações
- **Transacao**: Compras e movimentações financeiras
- **MovimentoPontos**: Histórico de pontos (acúmulo, resgate, expiração)

### Domínio de Recompensas
- **Recompensa**: Catálogo de produtos/serviços
- **Resgate**: Solicitações de resgate de pontos

### Domínio de Campanhas
- **CampanhaBonus**: Regras de bonificação
- **Notificacao**: Sistema de comunicação

## 🔄 Fluxo de Negócio

### 1. Acúmulo de Pontos
```
Transação → Validação → Cálculo de Pontos → Crédito → Notificação
```

### 2. Resgate de Pontos
```
Seleção da Recompensa → Validação de Saldo → Débito Imediato → 
Solicitação de Resgate → Aprovação → Conclusão
```

### 3. Campanhas de Bônus
```
Transação → Verificação de Campanhas Ativas → Aplicação de Bônus → 
Pontos Extras → Notificação
```

### 4. Expiração de Pontos
```
Job Automático → Identificação de Pontos Vencidos → 
Movimento de Expiração → Atualização de Saldo → Notificação
```

## 🚀 Como Executar

### Pré-requisitos
- **Docker** e **Docker Compose**
- **Java 21** (para desenvolvimento local)
- **Node.js 18+** (para desenvolvimento local)
- **Maven 3.9+** (para desenvolvimento local)

### 🐳 Execução com Docker (Produção)

#### Usando o script automatizado (Windows)
```bash
.\run-docker.bat
```

#### Manualmente
```bash
# Subir todos os serviços
docker-compose up --build -d

# Verificar logs
docker-compose logs -f

# Parar os serviços
docker-compose down
```

**Portas (Docker)**:
- **Frontend**: http://localhost (porta 80)
- **Backend**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Swagger UI**: http://localhost:8080/q/swagger-ui

### Portas dos Serviços

**Docker (Produção)**:
- Frontend: http://localhost:80
- Backend: http://localhost:8080
- PostgreSQL: localhost:5432
- Swagger UI: http://localhost:8080/q/swagger-ui

**Desenvolvimento Local**:
- Backend: http://localhost:8081 (Quarkus dev mode)
- Frontend: http://localhost:4200 (Angular dev server)
- PostgreSQL: localhost:5432 (mesmo container Docker)
- Swagger UI: http://localhost:8081/q/swagger-ui
- Health Check: http://localhost:8081/q/health

### 💻 Execução Local (Desenvolvimento)

#### 1. Banco de Dados
```bash
# Subir apenas o PostgreSQL
docker-compose up postgres -d

# Verificar se está rodando
docker-compose ps postgres
```

#### 2. Backend (Quarkus)
```bash
cd backend

# Primeira execução (instalar dependências)
mvn clean compile

# Modo desenvolvimento (hot reload)
mvn quarkus:dev

# Ou usando o wrapper
./mvnw quarkus:dev  # Linux/Mac
.\mvnw.cmd quarkus:dev  # Windows

# O backend estará disponível em http://localhost:8081
# Swagger UI: http://localhost:8081/q/swagger-ui
# Health Check: http://localhost:8081/q/health
```

**Portas (Backend Local)**:
- **API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/q/swagger-ui
- **Health Check**: http://localhost:8081/q/health

#### 3. Frontend (Angular)
```bash
cd frontend

# Instalar dependências (primeira execução)
npm install

# Servidor de desenvolvimento
npm start
# ou
ng serve --port 4200

# O frontend estará disponível em http://localhost:4200
```

**Portas (Frontend Local)**:
- **Aplicação**: http://localhost:4200
- **Proxy para Backend**: Configurado automaticamente

#### 4. Verificação dos Serviços
```bash
# Testar backend
curl http://localhost:8081/q/health

# Testar endpoint de exemplo
curl http://localhost:8081/admin/dashboard

# Frontend deve estar acessível no navegador
# http://localhost:4200
```

## 📊 Funcionalidades Principais

### Dashboard Administrativo
- Métricas consolidadas do sistema
- Total de usuários, cartões e transações
- Movimentação de pontos (acúmulo, resgate, expiração)
- Status de resgates por categoria
- Top recompensas e usuários

### Gestão de Recompensas
- Catálogo completo de produtos/serviços
- Controle de estoque automático
- Ativação/desativação por disponibilidade
- Categorização por tipo (PRODUTO, SERVICO, CASHBACK)

### Sistema de Resgates
- Débito imediato de pontos na solicitação
- Fluxo de aprovação administrativo
- Controle de status (PENDENTE → APROVADO → CONCLUIDO)
- Histórico completo de movimentações

### Extrato de Pontos
- Histórico detalhado por usuário
- Filtros por período e tipo de movimento
- Paginação para grandes volumes
- Exportação de dados

### Campanhas de Bônus
- Regras flexíveis de bonificação
- Segmentação por perfil de usuário
- Controle de vigência e prioridade
- Multiplicadores e bônus fixos

## 🔧 Configuração

### Variáveis de Ambiente

**Backend**:
```properties
# Banco de dados
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/postgres
QUARKUS_DATASOURCE_USERNAME=postgres
QUARKUS_DATASOURCE_PASSWORD=postgres

# Porta da aplicação
QUARKUS_HTTP_PORT=8081

# CORS (desenvolvimento)
QUARKUS_HTTP_CORS_ORIGINS=http://localhost:4200
```

**Frontend**:
```typescript
// src/app/config/api.config.ts
export const API_CONFIG = {
  baseUrl: window.location.port === '4200' 
    ? 'http://localhost:8081'  // Desenvolvimento
    : '/api'                   // Produção (proxy nginx)
};
```

## 📚 Documentação da API

### Endpoints Principais

**Usuários e Cartões**
- `GET /usuarios` - Listar usuários
- `POST /usuarios` - Criar usuário
- `GET /cartoes` - Listar cartões
- `POST /cartoes` - Criar cartão

**Transações e Pontos**
- `POST /transacoes` - Registrar transação
- `GET /pontos/saldo/{cartaoId}` - Consultar saldo
- `GET /pontos/extrato/{cartaoId}` - Extrato de pontos

**Recompensas e Resgates**
- `GET /recompensas` - Catálogo de recompensas
- `POST /resgates` - Solicitar resgate
- `GET /resgates` - Listar resgates

**Administração**
- `GET /admin/dashboard` - Dashboard administrativo
- `PUT /admin/resgates/{id}/aprovar` - Aprovar resgate
- `PUT /admin/resgates/{id}/concluir` - Concluir resgate

**Documentação Completa**: 
- Desenvolvimento: http://localhost:8081/q/swagger-ui
- Docker: http://localhost:8080/q/swagger-ui

## 🧪 Testes

### Backend
```bash
cd backend
mvn test
```

### Frontend
```bash
cd frontend
npm test
```

## 📈 Observabilidade

### Health Checks
- **Backend (Dev)**: http://localhost:8081/q/health
- **Backend (Docker)**: http://localhost:8080/q/health
- **Métricas (Dev)**: http://localhost:8081/q/metrics
- **Métricas (Docker)**: http://localhost:8080/q/metrics

### Logs
```bash
# Logs do Docker Compose
docker-compose logs -f [service_name]

# Logs específicos
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

## 🔒 Segurança

### Implementações Atuais
- **CORS** configurado para desenvolvimento
- **Validação de entrada** em todos os endpoints
- **Transações de banco** para consistência
- **Sanitização de dados** de entrada

### Roadmap de Segurança
- **JWT/OIDC** para autenticação
- **Rate limiting** para APIs
- **Auditoria** de operações críticas
- **Criptografia** de dados sensíveis

## 🗃️ Banco de Dados PostgreSQL

### Configuração

**Database**: `quarkus-social` (Docker) / `postgres` (Desenvolvimento)
**Schema**: `loyalty`
**Usuário**: `postgres`
**Senha**: `postgres`
**Porta**: `5432`

### Ambientes

**Docker (Produção)**:
```bash
# Conexão via container
Host: postgres (interno) / localhost:5432 (externo)
Database: quarkus-social
Schema: loyalty
```

**Desenvolvimento Local**:
```bash
# Usar container PostgreSQL do Docker Compose
docker-compose up -d postgres

# Conexão local
Host: localhost:5432
Database: postgres
Schema: loyalty
```

**Testes**:
```bash
# Configuração separada para testes
Host: localhost:6543
Database: quarkus-social
Schema: public
```

### Versionamento e Migrações

**Flyway**: Migrations em `backend/src/main/resources/db/migration/`
- **V1__init.sql**: Schema inicial completo
- **Automático**: Executa na inicialização (`quarkus.flyway.migrate-at-start=true`)
- **Schema**: Cria automaticamente o schema `loyalty`

### Principais Tabelas

**Core Entities**:
- `usuario` - Dados dos usuários do sistema
- `cartao` - Cartões vinculados aos usuários
- `transacao` - Histórico de compras e transações
- `saldo_pontos` - Saldos atuais por usuário/cartão

**Pontos e Movimentações**:
- `movimento_pontos` - Histórico detalhado de movimentações
- `regra_conversao` - Regras de conversão de valor para pontos
- `campanha_bonus` - Campanhas promocionais e multiplicadores

**Recompensas e Resgates**:
- `recompensa` - Catálogo de produtos e recompensas
- `resgate` - Solicitações e histórico de resgates

**Sistema**:
- `notificacao` - Sistema de notificações (email, push, SMS)

### Comandos Úteis

```bash
# Conectar ao PostgreSQL (Docker)
docker exec -it postgres psql -U postgres -d quarkus-social

# Verificar tabelas do schema loyalty
\dt loyalty.*

# Verificar dados de exemplo
SELECT * FROM loyalty.usuario LIMIT 5;
SELECT * FROM loyalty.recompensa LIMIT 5;

# Verificar migrações aplicadas
SELECT * FROM loyalty.flyway_schema_history;
```

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo `LICENSE` para mais detalhes.

---

**Desenvolvido com ❤️ usando Quarkus + Angular**