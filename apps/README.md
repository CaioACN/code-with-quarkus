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

### 💻 Execução Local (Desenvolvimento)

#### 1. Banco de Dados
```bash
# Subir apenas o PostgreSQL
docker-compose up postgres -d
```

#### 2. Backend (Quarkus)
```bash
cd backend

# Modo desenvolvimento (hot reload)
mvn quarkus:dev

# Ou usando o wrapper
./mvnw quarkus:dev  # Linux/Mac
.\mvnw.cmd quarkus:dev  # Windows
```

**Portas (Backend Local)**:
- **API**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/q/swagger-ui
- **Health Check**: http://localhost:8081/q/health

#### 3. Frontend (Angular)
```bash
cd frontend

# Instalar dependências
npm install

# Servidor de desenvolvimento
npm start
# ou
ng serve --port 4200
```

**Portas (Frontend Local)**:
- **Aplicação**: http://localhost:4200
- **Proxy para Backend**: Configurado automaticamente

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

**Documentação Completa**: http://localhost:8081/q/swagger-ui

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
- **Backend**: http://localhost:8081/q/health
- **Métricas**: http://localhost:8081/q/metrics

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

## 🗃️ Banco de Dados

### Schema: `loyalty`

**Versionamento**: Flyway migrations em `backend/src/main/resources/db/migration/`

**Principais Tabelas**:
- `usuarios` - Dados dos usuários
- `cartoes` - Cartões vinculados
- `transacoes` - Histórico de compras
- `saldo_pontos` - Saldos atuais
- `movimento_pontos` - Histórico de movimentações
- `recompensas` - Catálogo de produtos
- `resgates` - Solicitações de resgate
- `campanhas_bonus` - Regras de campanhas
- `notificacoes` - Sistema de comunicação

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