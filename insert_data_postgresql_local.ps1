# =====================================================
# INSERINDO DADOS NO POSTGRESQL LOCAL
# Sistema de Pontos do Cartão
# =====================================================

Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "INSERINDO DADOS NO POSTGRESQL LOCAL" -ForegroundColor Cyan
Write-Host "Sistema de Pontos do Cartão" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Verificando se o PostgreSQL está rodando localmente..." -ForegroundColor Yellow
try {
    $psqlVersion = psql --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ PostgreSQL encontrado!" -ForegroundColor Green
        Write-Host "Versão: $psqlVersion" -ForegroundColor Gray
    } else {
        throw "PostgreSQL não encontrado"
    }
} catch {
    Write-Host "❌ Erro: PostgreSQL não está instalado ou não está no PATH." -ForegroundColor Red
    Write-Host "Por favor, instale o PostgreSQL e adicione ao PATH." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Conectando ao banco PostgreSQL local..." -ForegroundColor Yellow
Write-Host "Usuário: postgres" -ForegroundColor Gray
Write-Host "Banco: quarkus-social" -ForegroundColor Gray
Write-Host ""

Write-Host "Executando script SQL..." -ForegroundColor Yellow
try {
    $env:PGPASSWORD = Read-Host "Digite a senha do PostgreSQL (postgres)" -AsSecureString | ConvertFrom-SecureString -AsPlainText
    if ([string]::IsNullOrEmpty($env:PGPASSWORD)) {
        $env:PGPASSWORD = "postgres"
    }
    
    psql -h localhost -U postgres -d quarkus-social -f insert_postgresql_local.sql
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Dados inseridos com sucesso no PostgreSQL local!" -ForegroundColor Green
    } else {
        throw "Erro ao executar script SQL"
    }
} catch {
    Write-Host "❌ Erro ao executar script SQL no PostgreSQL local." -ForegroundColor Red
    Write-Host "Verifique se:" -ForegroundColor Red
    Write-Host "1. PostgreSQL está rodando" -ForegroundColor Red
    Write-Host "2. Banco 'quarkus-social' existe" -ForegroundColor Red
    Write-Host "3. Usuário 'postgres' tem permissões" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Verificando dados inseridos..." -ForegroundColor Yellow
$verificationQuery = @"
SELECT 'USUÁRIOS' as tabela, COUNT(*) as total FROM loyalty.usuario 
UNION ALL SELECT 'CARTÕES' as tabela, COUNT(*) as total FROM loyalty.cartao 
UNION ALL SELECT 'TRANSAÇÕES' as tabela, COUNT(*) as total FROM loyalty.transacao 
UNION ALL SELECT 'MOVIMENTOS' as tabela, COUNT(*) as total FROM loyalty.movimento_pontos 
UNION ALL SELECT 'SALDOS' as tabela, COUNT(*) as total FROM loyalty.saldo_pontos 
UNION ALL SELECT 'RESGATES' as tabela, COUNT(*) as total FROM loyalty.resgate 
UNION ALL SELECT 'NOTIFICAÇÕES' as tabela, COUNT(*) as total FROM loyalty.notificacao 
UNION ALL SELECT 'RECOMPENSAS' as tabela, COUNT(*) as total FROM loyalty.recompensa 
UNION ALL SELECT 'REGRAS CONVERSÃO' as tabela, COUNT(*) as total FROM loyalty.regra_conversao 
UNION ALL SELECT 'CAMPANHAS BÔNUS' as tabela, COUNT(*) as total FROM loyalty.campanha_bonus 
ORDER BY tabela;
"@

psql -h localhost -U postgres -d quarkus-social -c $verificationQuery

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "DADOS INSERIDOS COM SUCESSO NO POSTGRESQL LOCAL!" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para verificar os dados, acesse:" -ForegroundColor Yellow
Write-Host "- Dashboard: http://localhost:8081/admin/dashboard" -ForegroundColor Green
Write-Host "- Swagger UI: http://localhost:8081/q/swagger-ui" -ForegroundColor Green
Write-Host ""

# Limpar senha da memória
$env:PGPASSWORD = $null

Read-Host "Pressione Enter para continuar"
