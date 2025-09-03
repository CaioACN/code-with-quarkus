package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.acme.loyalty.entity.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Schema(name = "AuditoriaUsuario", description = "Snapshot de auditoria e métricas consolidadas do usuário")
public class AuditoriaUsuarioDTO {

    // Identificação
    @Schema(description = "ID do usuário", example = "10", required = true)
    public Long usuarioId;

    @Schema(description = "Nome do usuário", example = "Maria Silva", required = true)
    public String nome;

    @Schema(description = "E-mail do usuário (único)", example = "maria.silva@exemplo.com", required = true)
    public String email;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de cadastro do usuário", example = "2025-08-15")
    public java.time.LocalDate dataCadastro;

    // Totais de relacionamento
    @Schema(description = "Quantidade de cartões")
    public Integer totalCartoes;

    @Schema(description = "Quantidade de transações")
    public Integer totalTransacoes;

    @Schema(description = "Quantidade de movimentos de pontos")
    public Integer totalMovimentosPontos;

    @Schema(description = "Quantidade de resgates")
    public Integer totalResgates;

    // Saldos consolidados (somatório de todos os cartões)
    @Schema(description = "Saldo total de pontos (soma de todos os cartões)", example = "12345")
    public Long saldoTotal;

    @Schema(description = "Pontos expirando em 30 dias (total)", example = "300")
    public Long pontosExpirando30Dias;

    @Schema(description = "Pontos expirando em 60 dias (total)", example = "200")
    public Long pontosExpirando60Dias;

    @Schema(description = "Pontos expirando em 90 dias (total)", example = "100")
    public Long pontosExpirando90Dias;

    @Schema(description = "Total de pontos expirando (30+60+90)")
    public Long pontosExpirandoTotal;

    // Últimas atividades
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora da última transação conhecida")
    public LocalDateTime ultimaTransacao;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora do último movimento de pontos")
    public LocalDateTime ultimoMovimento;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora do último resgate (criado/aprovado/concluído/negado, o mais recente)")
    public LocalDateTime ultimoResgate;

    // Derivados/KPIs
    @Schema(description = "Dias desde o cadastro do usuário", example = "42")
    public Long diasDesdeCadastro;

    @Schema(description = "Status agregado de saldo (SEM_PONTOS, BAIXO, MEDIO, ALTO)")
    public String statusSaldo;

    @Schema(description = "Tem pontos próximos de expirar (30/60/90 dias)?", example = "true")
    public Boolean riscoExpiracao;

    public AuditoriaUsuarioDTO() {}
    
    // ---------- Construtores ----------
    
    public AuditoriaUsuarioDTO(Long usuarioId, String nome, String email, LocalDate dataCadastro) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.email = email;
        this.dataCadastro = dataCadastro;
    }

    // ---------- Builders ----------

    /** Constrói o snapshot usando as listas carregadas dentro da entidade (requer que estejam inicializadas). */
    public static AuditoriaUsuarioDTO fromEntity(Usuario u) {
        return fromEntity(
            u,
            u.saldosPontos,
            u.transacoes,
            u.movimentosPontos,
            u.resgates
        );
    }

    /** Constrói o snapshot recebendo listas já carregadas/filtradas (evita LAZY). */
    public static AuditoriaUsuarioDTO fromEntity(Usuario u,
                                                 List<SaldoPontos> saldos,
                                                 List<Transacao> transacoes,
                                                 List<MovimentoPontos> movimentos,
                                                 List<Resgate> resgates) {
        AuditoriaUsuarioDTO dto = new AuditoriaUsuarioDTO();

        // Identidade
        dto.usuarioId = u.id;
        dto.nome = u.nome;
        dto.email = u.email;
        dto.dataCadastro = u.dataCadastro;

        // Totais (null-safe)
        dto.totalCartoes = safeSize(u.cartoes);
        dto.totalTransacoes = safeSize(transacoes);
        dto.totalMovimentosPontos = safeSize(movimentos);
        dto.totalResgates = safeSize(resgates);

        // Saldos consolidados
        long saldo = 0L, exp30 = 0L, exp60 = 0L, exp90 = 0L;
        if (saldos != null) {
            for (SaldoPontos sp : saldos) {
                saldo += nz(sp.saldo);
                exp30 += nz(sp.pontosExpirando30Dias);
                exp60 += nz(sp.pontosExpirando60Dias);
                exp90 += nz(sp.pontosExpirando90Dias);
            }
        }
        dto.saldoTotal = saldo;
        dto.pontosExpirando30Dias = exp30;
        dto.pontosExpirando60Dias = exp60;
        dto.pontosExpirando90Dias = exp90;
        dto.pontosExpirandoTotal = exp30 + exp60 + exp90;

        // Últimas atividades
        dto.ultimaTransacao = maxBy(transacoes, t -> t != null ? t.dataEvento : null);
        dto.ultimoMovimento = maxBy(movimentos, m -> m != null ? m.criadoEm : null);
        dto.ultimoResgate = maxBy(resgates, AuditoriaUsuarioDTO::latestResgateInstant);

        // KPIs
        dto.diasDesdeCadastro = (u.dataCadastro != null)
            ? Duration.between(u.dataCadastro.atStartOfDay(), LocalDate.now().atStartOfDay()).toDays()
            : null;

        dto.statusSaldo = statusSaldo(dto.saldoTotal);
        dto.riscoExpiracao = (dto.pontosExpirandoTotal != null && dto.pontosExpirandoTotal > 0);

        return dto;
    }

    // ---------- Helpers ----------

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static <T> LocalDateTime maxBy(List<T> list, java.util.function.Function<T, LocalDateTime> extractor) {
        if (list == null || list.isEmpty()) return null;
        return list.stream()
                .map(extractor)
                .filter(d -> d != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /** Retorna a data mais recente relevante do resgate (concluído/negado/aprovado/criado). */
    private static LocalDateTime latestResgateInstant(Resgate r) {
        if (r == null) return null;
        LocalDateTime latest = r.criadoEm;
        if (r.aprovadoEm != null && (latest == null || r.aprovadoEm.isAfter(latest))) latest = r.aprovadoEm;
        if (r.concluidoEm != null && (latest == null || r.concluidoEm.isAfter(latest))) latest = r.concluidoEm;
        if (r.negadoEm != null && (latest == null || r.negadoEm.isAfter(latest))) latest = r.negadoEm;
        return latest;
    }

    /** Agregador simples de status com base no saldo total (mesmos limiares de SaldoPontos#getStatusSaldo). */
    private static String statusSaldo(Long saldoTotal) {
        long s = saldoTotal == null ? 0L : saldoTotal;
        if (s == 0) return "SEM_PONTOS";
        if (s < 1_000) return "BAIXO";
        if (s < 10_000) return "MEDIO";
        return "ALTO";
        // Se quiser, dá pra evoluir para considerar também proximidade de expiração.
    }
    
    // ---------- Métodos de Negócio ----------
    
    /**
     * Verifica se o usuário tem atividade recente (últimos 30 dias)
     */
    public boolean temAtividadeRecente() {
        LocalDateTime limite = LocalDateTime.now().minusDays(30);
        return (ultimaTransacao != null && ultimaTransacao.isAfter(limite)) ||
               (ultimoMovimento != null && ultimoMovimento.isAfter(limite)) ||
               (ultimoResgate != null && ultimoResgate.isAfter(limite));
    }
    
    /**
     * Verifica se o usuário é considerado inativo (sem atividade há mais de 90 dias)
     */
    public boolean isInativo() {
        LocalDateTime limite = LocalDateTime.now().minusDays(90);
        return (ultimaTransacao == null || ultimaTransacao.isBefore(limite)) &&
               (ultimoMovimento == null || ultimoMovimento.isBefore(limite)) &&
               (ultimoResgate == null || ultimoResgate.isBefore(limite));
    }
    
    /**
     * Calcula o percentual de pontos expirando em relação ao saldo total
     */
    public Double getPercentualPontosExpirando() {
        if (saldoTotal == null || saldoTotal == 0) {
            return 0.0;
        }
        if (pontosExpirandoTotal == null) {
            return 0.0;
        }
        return (pontosExpirandoTotal.doubleValue() / saldoTotal.doubleValue()) * 100.0;
    }
    
    /**
     * Verifica se o usuário tem risco alto de expiração (> 50% dos pontos)
     */
    public boolean temRiscoAltoExpiracao() {
        return getPercentualPontosExpirando() > 50.0;
    }
    
    /**
     * Retorna o status de atividade do usuário
     */
    public String getStatusAtividade() {
        if (temAtividadeRecente()) {
            return "ATIVO";
        } else if (isInativo()) {
            return "INATIVO";
        } else {
            return "MODERADO";
        }
    }
    
    /**
     * Calcula a idade da conta em dias
     */
    public Long getIdadeContaDias() {
        return diasDesdeCadastro;
    }
    
    /**
     * Verifica se é um usuário novo (menos de 30 dias)
     */
    public boolean isUsuarioNovo() {
        return diasDesdeCadastro != null && diasDesdeCadastro < 30;
    }
    
    /**
     * Verifica se é um usuário antigo (mais de 365 dias)
     */
    public boolean isUsuarioAntigo() {
        return diasDesdeCadastro != null && diasDesdeCadastro > 365;
    }
    
    /**
     * Retorna um resumo textual da situação do usuário
     */
    public String getResumoSituacao() {
        StringBuilder resumo = new StringBuilder();
        
        resumo.append("Usuário ").append(statusSaldo != null ? statusSaldo.toLowerCase() : "sem pontos");
        
        if (riscoExpiracao != null && riscoExpiracao) {
            resumo.append(" com risco de expiração");
        }
        
        resumo.append(" (").append(getStatusAtividade().toLowerCase()).append(")");
        
        if (isUsuarioNovo()) {
            resumo.append(" - novo usuário");
        } else if (isUsuarioAntigo()) {
            resumo.append(" - usuário antigo");
        }
        
        return resumo.toString();
    }
    
    /**
     * Valida se os dados básicos do DTO estão preenchidos
     */
    public boolean isValid() {
        return usuarioId != null && 
               nome != null && !nome.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               dataCadastro != null;
    }
    
    /**
     * Retorna uma versão resumida do DTO para logs ou relatórios
     */
    public String toSummary() {
        return String.format("Usuario[%d] %s (%s) - %s - %d pontos", 
            usuarioId, nome, email, getStatusAtividade(), 
            saldoTotal != null ? saldoTotal : 0L);
    }
}
