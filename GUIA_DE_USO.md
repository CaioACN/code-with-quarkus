# 🏦 Sistema de Pontos de Cartão de Crédito - Guia de Uso

## 📋 O que é a Aplicação?

O **Sistema de Pontos de Cartão de Crédito** é uma aplicação completa desenvolvida com **Quarkus** (backend Java) e **Angular** (frontend) que simula um programa de fidelidade para cartões de crédito. A aplicação permite:

- 🏦 **Gestão de usuários** e cartões
- 💰 **Acúmulo automático de pontos** por transações
- 🎁 **Sistema de recompensas** e resgates
- 📊 **Dashboard administrativo** com métricas em tempo real
- 🔄 **Campanhas de bônus** e promoções
- 📱 **Interface web** responsiva e intuitiva

## 🏗️ Arquitetura da Aplicação

### Backend (Quarkus)
- **Framework**: Quarkus 3.25.0 com Java 17
- **Banco de Dados**: PostgreSQL (produção e desenvolvimento)
- **ORM**: Hibernate com Panache
- **API**: REST com OpenAPI/Swagger
- **Porta**: 8080 (Docker e local)

### Frontend (Angular)
- **Framework**: Angular 17
- **Interface**: Responsiva e moderna
- **Porta**: 4200

### Banco de Dados
- **PostgreSQL**: Porta 6543 (produção e desenvolvimento)
- **Schema**: `loyalty` (isolado)
- **Migrações**: Flyway (V1, V2, V3)

## 🚀 Como Executar a Aplicação

### Opção 1: Docker (Recomendado)
```bash
# Subir todos os serviços
docker compose up -d

# Inserir dados de teste
.\insert_data_docker.bat

# Verificar dados
.\verify_data.bat
```

### Opção 2: Desenvolvimento Local
```bash
# Backend
cd apps/backend
.\mvnw.cmd clean package -DskipTests -Pdocker
.\mvnw.cmd quarkus:dev -Dquarkus.profile=docker

# Frontend (em outro terminal)
cd apps/frontend
npm install
npm start
```

## 📊 Dados de Teste Incluídos

A aplicação vem com dados de exemplo para demonstração:

### 👥 Usuários (8 usuários)
- **João Silva** - Cartão: 1234567890123456
- **Maria Santos** - Cartão: 2345678901234567
- **Pedro Oliveira** - Cartão: 3456789012345678
- **Ana Costa** - Cartão: 4567890123456789
- **Carlos Ferreira** - Cartão: 5678901234567890
- **Lucia Mendes** - Cartão: 6789012345678901
- **Roberto Alves** - Cartão: 7890123456789012
- **Fernanda Lima** - Cartão: 8901234567890123

### 💳 Transações (29 transações)
- **Restaurantes**: 1 ponto a cada R$ 40,00
- **Supermercados**: 1 ponto a cada R$ 50,00
- **Postos de Gasolina**: 1 ponto a cada R$ 45,00
- **Farmácias**: 1 ponto a cada R$ 50,00

### 🎁 Recompensas Disponíveis
- **Cashback**: R$ 10,00 (1.000 pontos), R$ 25,00 (2.500 pontos), R$ 50,00 (5.000 pontos)
- **Vale Presente**: R$ 25,00 (2.500 pontos), R$ 50,00 (5.000 pontos)
- **Milhas Aéreas**: 1.000 milhas (2.000 pontos), 2.500 milhas (5.000 pontos)
- **Produtos**: Fone Bluetooth (15.000 pontos), Power Bank (8.000 pontos)

## 🌐 URLs de Acesso

### Aplicação Principal
- **Frontend**: http://localhost:4200
- **Backend**: http://localhost:8080 (Docker e local)

### Documentação e Ferramentas
- **Swagger UI**: http://localhost:8080/q/swagger-ui
- **Health Check**: http://localhost:8080/q/health
- **PostgreSQL**: localhost:6543

## 📱 Como Usar as Funcionalidades

### 1. 🏠 Dashboard Principal
**URL**: http://localhost:4200/dashboard

O dashboard mostra:
- **Total de Usuários**: Quantidade de usuários cadastrados
- **Total de Transações**: Número de transações processadas
- **Saldo Total de Pontos**: Soma de todos os pontos acumulados
- **Resgates Pendentes**: Quantidade de resgates aguardando aprovação

### 2. 💳 Gestão de Transações
**URL**: http://localhost:4200/transacoes

Funcionalidades:
- **Visualizar Transações**: Lista todas as transações por usuário
- **Histórico de Pontos**: Mostra como os pontos foram acumulados
- **Filtros**: Por data, categoria, usuário
- **Detalhes**: Valor, categoria, pontos gerados, data

### 3. 🎁 Catálogo de Recompensas
**URL**: http://localhost:4200/recompensas

Funcionalidades:
- **Visualizar Recompensas**: Lista todas as recompensas disponíveis
- **Filtros**: Por tipo (Cashback, Milhas, Produtos, Vale Presente)
- **Detalhes**: Custo em pontos, estoque disponível, descrição
- **Solicitar Resgate**: Interface para solicitar resgates

### 4. 📋 Histórico de Resgates
**URL**: http://localhost:4200/resgates

Funcionalidades:
- **Status dos Resgates**: Pendente, Aprovado, Concluído, Negado
- **Histórico Completo**: Todos os resgates solicitados
- **Filtros**: Por status, data, usuário
- **Detalhes**: Pontos utilizados, recompensa, data de solicitação

## 🔧 API Endpoints Principais

### Endpoints Públicos
```http
GET /hello
GET /q/health
GET /q/swagger-ui
```

### Endpoints Administrativos
```http
GET /admin/dashboard
GET /admin/sistema/health
GET /admin/sistema/metricas
```

### Endpoints de Usuário
```http
GET /usuario/{id}
GET /usuario/{id}/cartoes
GET /usuario/{id}/transacoes
GET /usuario/{id}/saldo
GET /usuario/{id}/resgates
```

### Endpoints de Transação
```http
POST /transacao
GET /transacao/{id}
GET /transacao/usuario/{usuarioId}
```

### Endpoints de Resgate
```http
POST /resgate
GET /resgate/{id}
PUT /resgate/{id}/aprovar
PUT /resgate/{id}/concluir
```

## 💡 Como Funciona o Sistema de Pontos

### 1. Acúmulo de Pontos
- **Regras de Conversão**: Configuradas por categoria de estabelecimento
- **Multiplicadores**: Diferentes para cada tipo de transação
- **Campanhas de Bônus**: Multiplicadores extras em períodos específicos

### 2. Cálculo de Pontos
```
Pontos = (Valor da Transação × Multiplicador) + Bônus de Campanha
```

**Exemplos**:
- Restaurante: R$ 100,00 × 0.025 = 2,5 pontos (arredondado para 3)
- Supermercado: R$ 100,00 × 0.02 = 2 pontos
- Posto: R$ 100,00 × 0.015 = 1,5 pontos (arredondado para 2)

### 3. Resgate de Pontos
- **Validação**: Verifica se o usuário tem pontos suficientes
- **Aprovação**: Processo de aprovação manual ou automática
- **Processamento**: Entrega da recompensa
- **Baixa**: Dedução dos pontos do saldo

## 📊 Monitoramento e Métricas

### Dashboard Administrativo
- **Métricas em Tempo Real**: Usuários, transações, pontos
- **Status do Sistema**: Saúde da aplicação e banco de dados
- **Performance**: Tempo de resposta e disponibilidade

### Logs e Auditoria
- **Transações**: Todas as transações são logadas
- **Movimentos de Pontos**: Histórico completo de acúmulo e resgate
- **Notificações**: Sistema de notificações por email

## 🔍 Exemplos Práticos de Uso

### Exemplo 1: Usuário Faz uma Compra
1. **Transação**: João Silva compra R$ 150,00 em um restaurante
2. **Cálculo**: 150 × 0.025 = 3,75 pontos → 4 pontos
3. **Acúmulo**: Pontos adicionados ao saldo de João
4. **Notificação**: Email enviado confirmando os pontos

### Exemplo 2: Usuário Solicita Resgate
1. **Solicitação**: Maria Santos solicita cashback de R$ 25,00 (2.500 pontos)
2. **Validação**: Sistema verifica se ela tem pontos suficientes
3. **Aprovação**: Resgate aprovado automaticamente
4. **Processamento**: Cashback creditado na fatura
5. **Baixa**: 2.500 pontos deduzidos do saldo

### Exemplo 3: Campanha de Bônus
1. **Configuração**: Campanha "Black Friday" com 50% de bônus
2. **Aplicação**: Multiplicador 0.025 vira 0.0375 (0.025 + 50%)
3. **Resultado**: Usuários ganham mais pontos durante o período

## 🛠️ Configurações Avançadas

### Regras de Conversão
```sql
-- Exemplo de regra personalizada
INSERT INTO regra_conversao (nome, multiplicador, categoria, vigencia_ini, prioridade, ativo) 
VALUES ('Farmácias Premium', 0.03, 'FARMACIA', '2024-12-01', 1, true);
```

### Campanhas de Bônus
```sql
-- Exemplo de campanha
INSERT INTO campanha_bonus (nome, multiplicador_extra, vigencia_ini, vigencia_fim, segmento, prioridade) 
VALUES ('Natal 2024', 0.05, '2024-12-15', '2024-12-25', 'GERAL', 1);
```

### Novas Recompensas
```sql
-- Exemplo de nova recompensa
INSERT INTO recompensa (tipo, descricao, custo_pontos, estoque, ativo) 
VALUES ('PRODUTO', 'Smartphone Premium', 50000, 10, true);
```

## 🔧 Troubleshooting

### Problemas Comuns

#### 1. Aplicação não inicia
```bash
# Verificar se as portas estão livres
netstat -an | findstr :8080
netstat -an | findstr :4200

# Parar processos Java
Stop-Process -Name "java" -Force
```

#### 2. Banco de dados não conecta
```bash
# Verificar containers Docker
docker ps

# Verificar logs
docker compose logs postgres
```

#### 3. Dados não aparecem
```bash
# Inserir dados novamente
.\insert_data_docker.bat

# Verificar dados
.\verify_data.bat
```

### Logs Importantes
- **Backend**: Logs aparecem no terminal onde executou o Quarkus
- **Docker**: `docker compose logs -f backend`
- **PostgreSQL**: `docker compose logs -f postgres`

## 📚 Recursos Adicionais

### Documentação da API
- **Swagger UI**: http://localhost:8080/q/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/q/openapi

### Scripts Úteis
- **`insert_data_docker.bat`**: Inserir dados no Docker
- **`verify_data.bat`**: Verificar dados inseridos
- **`start-local.bat`**: Menu interativo para iniciar aplicação

### Comandos Maven
```bash
# Executar testes
.\mvnw.cmd test -Pdocker

# Compilar aplicação
.\mvnw.cmd clean package -DskipTests -Pdocker

# Executar em modo dev
.\mvnw.cmd quarkus:dev -Dquarkus.profile=docker

# Build para produção
.\mvnw.cmd package -Pdocker
```

## 🎯 Próximos Passos

### Para Desenvolvedores
1. **Explorar o código**: Analisar as entidades e serviços
2. **Adicionar funcionalidades**: Implementar novos endpoints
3. **Customizar regras**: Modificar regras de conversão
4. **Integrar sistemas**: Conectar com sistemas externos

### Para Usuários Finais
1. **Testar funcionalidades**: Usar o frontend para explorar
2. **Simular transações**: Criar transações de teste
3. **Solicitar resgates**: Testar o fluxo de resgate
4. **Monitorar métricas**: Acompanhar o dashboard

---

## 📞 Suporte

Para dúvidas ou problemas:
1. Verifique os logs da aplicação
2. Consulte a documentação do Swagger
3. Execute os scripts de verificação
4. Verifique se todos os serviços estão rodando

**A aplicação está pronta para uso e demonstração!** 🚀
