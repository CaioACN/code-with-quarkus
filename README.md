# Sistema de Pontos de Cartão de Crédito

## 📋 Descrição

Sistema completo de gerenciamento de pontos de cartão de crédito desenvolvido com **Quarkus** (backend) e **Angular** (frontend). O sistema permite:

- 🏦 **Gestão de usuários** e cartões
- 💰 **Acúmulo de pontos** por transações
- 🎁 **Sistema de recompensas** e resgates
- 📊 **Dashboard administrativo** com métricas
- 🔄 **Campanhas de bônus** e promoções
- 📱 **Interface web** responsiva

## 🏗️ Arquitetura

- **Backend**: Quarkus (Java) com Hibernate/JPA
- **Frontend**: Angular 17
- **Banco de Dados**: PostgreSQL (produção e desenvolvimento)
- **Containerização**: Docker & Docker Compose
- **Migrações**: Flyway para versionamento do schema

## 🚀 Como Executar Localmente

### Pré-requisitos

- Java 17+
- Maven 3.8+
- Node.js 18+
- Docker (opcional)

### Opção 1: Desenvolvimento Local (Recomendado)

#### 1. Backend (Quarkus)

```bash
# Navegar para o diretório do backend
cd apps/backend

# Compilar com perfil Docker (PostgreSQL)
.\mvnw.cmd clean package -DskipTests -Pdocker

# Executar a aplicação
.\mvnw.cmd quarkus:dev -Dquarkus.profile=docker
```

**URLs do Backend:**
- Aplicação: http://localhost:8080
- Swagger UI: http://localhost:8080/q/swagger-ui
- Health Check: http://localhost:8080/q/health

#### 2. Frontend (Angular)

```bash
# Navegar para o diretório do frontend
cd apps/frontend

# Instalar dependências
npm install

# Executar a aplicação
npm start
```

**URLs do Frontend:**
- Aplicação: http://localhost:4200

### Opção 2: Docker (Recomendado para Produção)

```bash
# Na raiz do projeto
docker compose up -d
```

**URLs do Docker:**
- Backend: http://localhost:8080
- Frontend: http://localhost:4200
- Swagger UI: http://localhost:8080/q/swagger-ui
- PostgreSQL: localhost:6543

## 🧪 Executar Testes

```bash
# No diretório do backend
cd apps/backend

# Executar testes com PostgreSQL
.\mvnw.cmd test -Pdocker
```

## 📊 Endpoints Principais

### Backend (Quarkus)

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `/hello` | GET | Endpoint básico de teste |
| `/q/health` | GET | Health check da aplicação |
| `/admin/dashboard` | GET | Dashboard administrativo |
| `/admin/sistema/health` | GET | Saúde do sistema |
| `/admin/sistema/metricas` | GET | Métricas do sistema |
| `/q/swagger-ui` | GET | Documentação da API |

### Frontend (Angular)

| Página | URL | Descrição |
|--------|-----|-----------|
| Dashboard | `/dashboard` | Painel principal |
| Transações | `/transacoes` | Gestão de transações |
| Recompensas | `/recompensas` | Catálogo de recompensas |
| Resgates | `/resgates` | Histórico de resgates |

## 🗄️ Banco de Dados

### Configuração Atual
- **PostgreSQL** (porta 6543) - **Único banco suportado**
- **Configuração**: `application-docker.properties`
- **Migrações**: Flyway (V1, V2, V3) aplicadas automaticamente
- **Schema**: `loyalty` (isolado do schema público)
- **Dados**: Execute `insert_data_docker.bat` para inserir dados de teste

### ⚠️ Importante
- **H2 foi removido** - A aplicação agora usa apenas PostgreSQL
- **Compilação**: Sempre use `-Pdocker` para garantir o uso do PostgreSQL
- **Desenvolvimento**: Use Docker ou configure PostgreSQL localmente

### Conexão com Banco (DBeaver/pgAdmin)
- **Host**: `localhost`
- **Porta**: `6543`
- **Database**: `quarkus-social`
- **Schema**: `loyalty`
- **Username**: `postgres`
- **Password**: `postgres`
- **Nota**: Se usar DBeaver, adicione `extra_float_digits=0` nas propriedades da conexão

## 📊 Dados de Teste

### Dados Incluídos
- ✅ **5 usuários** com cartões
- ✅ **14 transações** de exemplo
- ✅ **13 movimentos** de pontos
- ✅ **5 saldos** de pontos
- ✅ **3 resgates** de exemplo
- ✅ **5 notificações** de exemplo
- ✅ **9 recompensas** disponíveis
- ✅ **5 regras** de conversão
- ✅ **4 campanhas** de bônus

### Inserir Dados

#### Para PostgreSQL (Docker ou Local):
```bash
# Execute o script
.\insert_data_docker.bat
```

#### Verificar Dados:
```bash
# Execute o script de verificação
.\verify_data.bat
```

## 🔧 Configurações

### Profiles Maven

- **docker**: **Único perfil ativo** - PostgreSQL (desenvolvimento e produção)

### Variáveis de Ambiente

```bash
# Para desenvolvimento local
QUARKUS_PROFILE=docker
QUARKUS_HTTP_PORT=8080

# Para Docker
QUARKUS_PROFILE=docker
QUARKUS_HTTP_PORT=8080
```

## 📁 Estrutura do Projeto

```
code-with-quarkus/
├── apps/
│   ├── backend/                 # Backend Quarkus
│   │   ├── src/main/java/       # Código Java
│   │   ├── src/main/resources/  # Configurações e migrações
│   │   ├── src/test/           # Testes
│   │   ├── Dockerfile          # Imagem Docker
│   │   └── pom.xml             # Maven
│   └── frontend/               # Frontend Angular
│       ├── src/app/           # Código Angular
│       ├── src/assets/        # Assets
│       ├── Dockerfile         # Imagem Docker
│       └── package.json       # NPM
├── docker-compose.yml         # Docker Compose
└── README.md                  # Este arquivo
```

## 🛠️ Comandos Úteis

### Backend

```bash
# Compilar com PostgreSQL
.\mvnw.cmd clean package -DskipTests -Pdocker

# Executar testes
.\mvnw.cmd test -Pdocker

# Executar em modo dev
.\mvnw.cmd quarkus:dev -Dquarkus.profile=docker

# Build para produção
.\mvnw.cmd package -Pdocker
```

### Frontend

```bash
# Instalar dependências
npm install

# Executar em modo dev
npm start

# Build para produção
npm run build

# Executar testes
npm test
```

### Docker

```bash
# Subir todos os serviços
docker compose up -d

# Ver logs
docker compose logs -f

# Parar serviços
docker compose down

# Status dos containers
docker compose ps

# Reconstruir apenas o backend
docker compose build --no-cache backend

# Reconstruir apenas o PostgreSQL
docker compose up -d --force-recreate --no-deps postgres
```

## 🐛 Troubleshooting

### Problemas Comuns

1. **Porta já em uso**: Verifique se não há outros serviços rodando nas portas 8080, 4200, 6543
2. **Erro de conexão PostgreSQL**: Verifique se o PostgreSQL está rodando na porta 6543
3. **Driver não encontrado**: Sempre compile com `-Pdocker` para incluir o driver PostgreSQL
4. **Testes falhando**: Execute `.\mvnw.cmd clean test -Pdocker` para limpar e executar novamente

### Logs

```bash
# Backend (local)
# Logs aparecem no terminal onde executou o quarkus:dev

# Docker
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

## 📝 Licença

Este projeto é para fins educacionais e de demonstração.

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature
3. Commit suas mudanças
4. Push para a branch
5. Abra um Pull Request