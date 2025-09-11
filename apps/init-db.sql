-- Inicialização do banco de dados para o ambiente Docker

-- Cria o schema loyalty se não existir
CREATE SCHEMA IF NOT EXISTS loyalty;

-- Define o schema loyalty como padrão para as operações seguintes
SET search_path TO loyalty;

-- Garante que o usuário postgres tenha todos os privilégios no schema loyalty
GRANT ALL PRIVILEGES ON SCHEMA loyalty TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA loyalty TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA loyalty TO postgres;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA loyalty TO postgres;

-- Configura para que novos objetos criados no schema loyalty pertençam ao usuário postgres
ALTER DEFAULT PRIVILEGES IN SCHEMA loyalty GRANT ALL PRIVILEGES ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA loyalty GRANT ALL PRIVILEGES ON SEQUENCES TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA loyalty GRANT ALL PRIVILEGES ON FUNCTIONS TO postgres;