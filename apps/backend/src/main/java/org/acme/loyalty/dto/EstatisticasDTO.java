package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.*;
import org.acme.loyalty.entity.MovimentoPontos.TipoMovimento;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Estatísticas consolidadas do programa de pontos para relatórios/monitoramento.
 * Agrega KPIs simples (contagens, somatórios e médias) em uma janela de tempo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Estatisticas", description = "KPIs consolidados do programa de pontos")
public class EstatisticasDTO {

    // Período considerado
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do período")
    public LocalDateTime periodoIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim do período")
    public LocalDateTime periodoFim;

    // Base cadastral
    @Schema(description = "Total de usuários cadastrados")
    public Integer totalUsuarios;

    @Schema(description = "Total de cartões cadastrados")
    public Integer totalCartoes;

    // Transações
    @Schema(description = "Total de transações no período")
    public Integer totalTransacoes;

    @Schema(description = "Valor total transacionado (soma de valores)")
    public BigDecimal valorTransacionadoTotal;

    @Schema(description = "Ticket médio (valor médio por transação)")
    public BigDecimal valorMedioTransacao;

    // Pontos (movimentos)
    @Schema(description = "Pontos acumulados no período")
    public Long pontosAcumulados;

    @Schema(description = "Pontos resgatados no período")
    public Long pontosResgatados;

    @Schema(description = "Pontos expirados no período")
    public Long pontosExpirados;

    // Saldos
    @Schema(description = "Saldo total de pontos (todas as carteiras)")
    public Long saldoTotal;

    @Schema(description = "Saldo médio por usuário")
    public Long saldoMedioPorUsuario;

    // Engajamento
    @Schema(description = "Usuários com pelo menos 1 transação no período")
    public Integer usuariosAtivos;

    @Schema(description = "Usuários que fizeram ao menos 1 resgate no período")
    public Integer usuariosComResgate;

    @Schema(description = "Taxa de conversão de resgate (usuários que resgataram / usuários ativos)")
    public Double taxaConversaoResgate;

    // Resgates por status (no período)
    @Schema(description = "Resgates PENDENTE")
    public Integer resgatesPendentes;

    @Schema(description = "Resgates APROVADO")
    public Integer resgatesAprovados;

    @Schema(description = "Resgates CONCLUIDO")
    public Integer resgatesConcluidos;

    @Schema(description = "Resgates NEGADO")
    public Integer resgatesNegados;

    @Schema(description = "Resgates CANCELADO")
    public Integer resgatesCancelados;

    // Séries simples (contagens por dia)
    @Schema(description = "Transações por dia")
    public List<SerieDia<Long>> transacoesPorDia;

    @Schema(description = "Resgates por dia")
    public List<SerieDia<Long>> resgatesPorDia;

    // ---- Tipos auxiliares ----
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SerieDia<T> {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        public LocalDate dia;
        public T valor;

        public SerieDia() {}
        public SerieDia(LocalDate dia, T valor) { this.dia = dia; this.valor = valor; }
    }

    public EstatisticasDTO() {}

    // ===================== Builder principal =====================

    /**
     * Constrói as estatísticas agregando as coleções do domínio já carregadas/filtradas.
     * Qualquer lista pode ser null; será tratada como vazia.
     */
    public static EstatisticasDTO of(LocalDateTime ini,
                                     LocalDateTime fim,
                                     List<Usuario> usuarios,
                                     List<Cartao> cartoes,
                                     List<Transacao> transacoes,
                                     List<MovimentoPontos> movimentos,
                                     List<Resgate> resgates,
                                     List<SaldoPontos> saldos) {

        EstatisticasDTO dto = new EstatisticasDTO();
        dto.periodoIni = ini;
        dto.periodoFim = fim;

        // Base cadastral
        dto.totalUsuarios = size(usuarios);
        dto.totalCartoes  = size(cartoes);

        // Filtro período
        List<Transacao> txPeriodo = filterBy(transacoes, t -> t != null ? t.dataEvento : null, ini, fim);
        List<MovimentoPontos> movPeriodo = filterBy(movimentos, m -> m != null ? m.criadoEm : null, ini, fim);
        List<Resgate> resgPeriodo = filterBy(resgates, r -> r != null ? r.criadoEm : null, ini, fim);

        // Transações
        dto.totalTransacoes = size(txPeriodo);
        dto.valorTransacionadoTotal = sumValores(txPeriodo);
        dto.valorMedioTransacao = avgValores(txPeriodo, dto.valorTransacionadoTotal);

        // Pontos
        dto.pontosAcumulados = sumMov(movPeriodo, TipoMovimento.ACUMULO);
        dto.pontosResgatados = sumMov(movPeriodo, TipoMovimento.RESGATE);
        dto.pontosExpirados  = sumMov(movPeriodo, TipoMovimento.EXPIRACAO);

        // Saldos
        dto.saldoTotal = sumSaldos(saldos);
        dto.saldoMedioPorUsuario = dto.totalUsuarios != null && dto.totalUsuarios > 0
                ? Math.round(dto.saldoTotal / (double) dto.totalUsuarios)
                : 0L;

        // Engajamento
        dto.usuariosAtivos = countDistinctIds(txPeriodo, t -> t.usuario != null ? t.usuario.id : null);
        dto.usuariosComResgate = countDistinctIds(resgPeriodo, r -> r.usuario != null ? r.usuario.id : null);
        dto.taxaConversaoResgate = calcRate(dto.usuariosComResgate, dto.usuariosAtivos);

        // Resgates por status
        dto.resgatesPendentes  = countResg(resgPeriodo, Resgate.StatusResgate.PENDENTE);
        dto.resgatesAprovados  = countResg(resgPeriodo, Resgate.StatusResgate.APROVADO);
        dto.resgatesConcluidos = countResg(resgPeriodo, Resgate.StatusResgate.CONCLUIDO);
        dto.resgatesNegados    = countResg(resgPeriodo, Resgate.StatusResgate.NEGADO);
        dto.resgatesCancelados = countResg(resgPeriodo, Resgate.StatusResgate.CANCELADO);

        // Séries diárias
        dto.transacoesPorDia = toSerieDia(countByDate(txPeriodo, t -> t.dataEvento));
        dto.resgatesPorDia   = toSerieDia(countByDate(resgPeriodo, r -> r.criadoEm));

        return dto;
    }

    // ===================== Helpers =====================

    private static int size(Collection<?> c) { return c == null ? 0 : c.size(); }

    private static <T> List<T> filterBy(List<T> list,
                                        Function<T, LocalDateTime> extractor,
                                        LocalDateTime ini,
                                        LocalDateTime fim) {
        if (list == null) return Collections.emptyList();
        return list.stream()
                .filter(Objects::nonNull)
                .filter(it -> {
                    LocalDateTime ts = extractor.apply(it);
                    if (ts == null) return false;
                    boolean geIni = (ini == null) || !ts.isBefore(ini);
                    boolean leFim = (fim == null) || !ts.isAfter(fim);
                    return geIni && leFim;
                })
                .collect(Collectors.toList());
    }

    private static BigDecimal sumValores(List<Transacao> txs) {
        if (txs == null || txs.isEmpty()) return BigDecimal.ZERO;
        return txs.stream()
                .filter(t -> t != null && t.valor != null)
                .map(t -> t.valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal avgValores(List<Transacao> txs, BigDecimal total) {
        int n = txs == null ? 0 : (int) txs.stream().filter(t -> t != null && t.valor != null).count();
        if (n == 0 || total == null) return BigDecimal.ZERO;
        return total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
    }

    /** Soma absoluta dos pontos por tipo. */
    private static Long sumMov(List<MovimentoPontos> lista, TipoMovimento tipo) {
        if (lista == null || lista.isEmpty()) return 0L;
        long total = lista.stream()
                .filter(m -> m != null && m.tipo == tipo && m.pontos != null)
                .mapToLong(m -> Math.abs(m.pontos.longValue()))
                .sum();
        return total;
    }

    private static Long sumSaldos(List<SaldoPontos> saldos) {
        if (saldos == null || saldos.isEmpty()) return 0L;
        long total = saldos.stream()
                .filter(Objects::nonNull)
                .map(sp -> sp.saldo)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        return total;
    }

    private static int countResg(List<Resgate> lista, Resgate.StatusResgate status) {
        if (lista == null) return 0;
        return (int) lista.stream().filter(r -> r != null && r.status == status).count();
    }

    private static <T> int countDistinctIds(List<T> lista, Function<T, Long> idExtractor) {
        if (lista == null) return 0;
        return (int) lista.stream()
                .map(idExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .size();
    }

    private static Double calcRate(Integer num, Integer den) {
        if (num == null || den == null || den == 0) return 0.0;
        return (double) num / den;
    }

    private static <T> Map<LocalDate, Long> countByDate(List<T> list, Function<T, LocalDateTime> tsExtractor) {
        if (list == null) return Collections.emptyMap();
        return list.stream()
                .filter(Objects::nonNull)
                .map(tsExtractor)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static List<SerieDia<Long>> toSerieDia(Map<LocalDate, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SerieDia<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
