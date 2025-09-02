-- =====================================================
-- SCRIPT DE VERIFICAÇÃO DAS TABELAS CRIADAS
-- Sistema de Pontos do Cartão (Quarkus/Java 17)
-- =====================================================

-- Configurações
SET search_path TO loyalty, public;

-- =====================================================
-- 1. VERIFICAÇÃO GERAL DO SCHEMA
-- =====================================================

SELECT 
    'SCHEMA LOYALTY' as verificacao,
    CASE 
        WHEN EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'loyalty') 
        THEN 'EXISTE' 
        ELSE 'NÃO EXISTE' 
    END as status;

-- =====================================================
-- 2. LISTAGEM DE TODAS AS TABELAS
-- =====================================================

SELECT 
    'TABELAS NO SCHEMA LOYALTY' as verificacao,
    table_name as tabela,
    CASE 
        WHEN table_name IN ('usuario', 'cartao', 'transacao', 'regra_conversao', 
                           'campanha_bonus', 'movimento_pontos', 'saldo_pontos', 
                           'recompensa', 'resgate', 'notificacao') 
        THEN 'OK' 
        ELSE 'EXTRA' 
    END as status
FROM information_schema.tables 
WHERE table_schema = 'loyalty' 
ORDER BY table_name;

-- =====================================================
-- 3. VERIFICAÇÃO DE TABELAS OBRIGATÓRIAS
-- =====================================================

WITH tabelas_obrigatorias AS (
    SELECT unnest(ARRAY[
        'usuario', 'cartao', 'transacao', 'regra_conversao', 
        'campanha_bonus', 'movimento_pontos', 'saldo_pontos', 
        'recompensa', 'resgate', 'notificacao'
    ]) as tabela
),
tabelas_existentes AS (
    SELECT table_name as tabela
    FROM information_schema.tables 
    WHERE table_schema = 'loyalty'
)
SELECT 
    'VERIFICAÇÃO DE TABELAS OBRIGATÓRIAS' as verificacao,
    t.tabela,
    CASE 
        WHEN e.tabela IS NOT NULL THEN 'CRIADA' 
        ELSE 'FALTANDO' 
    END as status
FROM tabelas_obrigatorias t
LEFT JOIN tabelas_existentes e ON t.tabela = e.tabela
ORDER BY t.tabela;

-- =====================================================
-- 4. VERIFICAÇÃO DE CONSTRAINTS E FOREIGN KEYS
-- =====================================================

SELECT 
    'FOREIGN KEYS' as verificacao,
    tc.table_name as tabela,
    tc.constraint_name as constraint_name,
    kcu.column_name as coluna,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND tc.table_schema = 'loyalty'
ORDER BY tc.table_name, tc.constraint_name;

-- =====================================================
-- 5. VERIFICAÇÃO DE ÍNDICES
-- =====================================================

SELECT 
    'ÍNDICES' as verificacao,
    schemaname as schema_name,
    tablename as tabela,
    indexname as indice,
    indexdef as definicao
FROM pg_indexes 
WHERE schemaname = 'loyalty'
ORDER BY tablename, indexname;

-- =====================================================
-- 6. VERIFICAÇÃO DE TRIGGERS
-- =====================================================

SELECT 
    'TRIGGERS' as verificacao,
    trigger_schema as schema_name,
    event_object_table as tabela,
    trigger_name as trigger_name,
    action_timing as timing,
    event_manipulation as evento
FROM information_schema.triggers 
WHERE trigger_schema = 'loyalty'
ORDER BY event_object_table, trigger_name;

-- =====================================================
-- 7. CONTAGEM DE REGISTROS NAS TABELAS
-- =====================================================

SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'usuario' as tabela,
    COUNT(*) as total_registros
FROM loyalty.usuario
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'cartao' as tabela,
    COUNT(*) as total_registros
FROM loyalty.cartao
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'transacao' as tabela,
    COUNT(*) as total_registros
FROM loyalty.transacao
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'regra_conversao' as tabela,
    COUNT(*) as total_registros
FROM loyalty.regra_conversao
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'campanha_bonus' as tabela,
    COUNT(*) as total_registros
FROM loyalty.campanha_bonus
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'movimento_pontos' as tabela,
    COUNT(*) as total_registros
FROM loyalty.movimento_pontos
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'saldo_pontos' as tabela,
    COUNT(*) as total_registros
FROM loyalty.saldo_pontos
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'recompensa' as tabela,
    COUNT(*) as total_registros
FROM loyalty.recompensa
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'resgate' as tabela,
    COUNT(*) as total_registros
FROM loyalty.resgate
UNION ALL
SELECT 
    'CONTAGEM DE REGISTROS' as verificacao,
    'notificacao' as tabela,
    COUNT(*) as total_registros
FROM loyalty.notificacao
ORDER BY tabela;

-- =====================================================
-- 8. VERIFICAÇÃO DE DADOS INICIAIS
-- =====================================================

SELECT 
    'DADOS INICIAIS - REGRAS DE CONVERSÃO' as verificacao,
    COUNT(*) as total_regras,
    COUNT(CASE WHEN ativo = true THEN 1 END) as regras_ativas
FROM loyalty.regra_conversao;

SELECT 
    'DADOS INICIAIS - CAMPANHAS DE BÔNUS' as verificacao,
    COUNT(*) as total_campanhas,
    COUNT(CASE WHEN vigencia_fim IS NULL OR vigencia_fim > CURRENT_DATE THEN 1 END) as campanhas_vigentes
FROM loyalty.campanha_bonus;

SELECT 
    'DADOS INICIAIS - RECOMPENSAS' as verificacao,
    COUNT(*) as total_recompensas,
    COUNT(CASE WHEN ativo = true THEN 1 END) as recompensas_ativas
FROM loyalty.recompensa;

-- =====================================================
-- 9. RESUMO FINAL
-- =====================================================

WITH resumo AS (
    SELECT 
        COUNT(*) as total_tabelas,
        COUNT(CASE WHEN table_name IN ('usuario', 'cartao', 'transacao', 'regra_conversao', 
                                      'campanha_bonus', 'movimento_pontos', 'saldo_pontos', 
                                      'recompensa', 'resgate', 'notificacao') THEN 1 END) as tabelas_obrigatorias
    FROM information_schema.tables 
    WHERE table_schema = 'loyalty'
)
SELECT 
    'RESUMO FINAL' as verificacao,
    total_tabelas as total_tabelas_criadas,
    tabelas_obrigatorias as tabelas_obrigatorias_criadas,
    CASE 
        WHEN tabelas_obrigatorias = 10 THEN 'TODAS AS TABELAS OBRIGATÓRIAS FORAM CRIADAS'
        ELSE 'ALGUMAS TABELAS OBRIGATÓRIAS ESTÃO FALTANDO'
    END as status_final
FROM resumo;

-- =====================================================
-- 10. TESTE DE INTEGRIDADE BÁSICA
-- =====================================================

-- Teste 1: Verificar se as foreign keys estão funcionando
SELECT 
    'TESTE DE INTEGRIDADE' as verificacao,
    'Foreign Keys' as teste,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.table_constraints 
            WHERE table_schema = 'loyalty' 
            AND constraint_type = 'FOREIGN KEY'
        ) THEN 'FOREIGN KEYS CONFIGURADAS'
        ELSE 'FOREIGN KEYS NÃO ENCONTRADAS'
    END as resultado;

-- Teste 2: Verificar se os índices foram criados
SELECT 
    'TESTE DE INTEGRIDADE' as verificacao,
    'Índices' as teste,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM pg_indexes 
            WHERE schemaname = 'loyalty'
        ) THEN 'ÍNDICES CRIADOS'
        ELSE 'ÍNDICES NÃO ENCONTRADOS'
    END as resultado;

-- Teste 3: Verificar se os triggers foram criados
SELECT 
    'TESTE DE INTEGRIDADE' as verificacao,
    'Triggers' as teste,
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.triggers 
            WHERE trigger_schema = 'loyalty'
        ) THEN 'TRIGGERS CRIADOS'
        ELSE 'TRIGGERS NÃO ENCONTRADOS'
    END as resultado;

-- =====================================================
-- FIM DO SCRIPT DE VERIFICAÇÃO
-- =====================================================
