-- V1 de exemplo: apenas cria uma tabela "probe" no schema loyalty
CREATE TABLE IF NOT EXISTS loyalty._flyway_probe (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMPTZ DEFAULT now()
);