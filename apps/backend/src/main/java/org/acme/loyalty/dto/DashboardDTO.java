package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.*;
import org.acme.loyalty.entity.MovimentoPontos.TipoMovimento;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Snapshot agregador para o dashboard do sistema de pontos.
 * Reúne KPIs de usuários, transações, movimentos e resgates em um período.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Dashboard", description = "Resumo agregado do programa de pontos para exibição em dashboard")
public class DashboardDTO {

    // Janela de análise
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do período considerado")
    public LocalDateTime periodoIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim do período considerado")
    public LocalDateTime periodoFim;

    // KPIs de base
    @Schema(description = "Total de usuários cadastrados")
    public Integer totalUsuarios;

    @Schema(description = "Total de cartões cadastrados")
    public Integer totalCartoes;

    @Schema(description = "Total de transações no período")
    public Integer totalTransacoes;

    @Schema(description = "Somatório de pontos acumulados no período")
    public Long pontosAcumulados;

    @Schema(description = "Somatório de pontos expirados no período")
    public Long pontosExpirados;

    @Schema(description = "Somatório de pontos resgatados no período")
    public Long pontosResgatados;

    @Schema(description = "Saldo total de pontos (todas as carteiras)")
    public Long saldoTotal;

    // Resgates por status
    @Schema(description = "Quantidade de resgates PENDENTE")
    public Integer resgatesPendentes;

    @Schema(description = "Quantidade de resgates APROVADO")
    public Integer resgatesAprovados;

    @Schema(description = "Quantidade de resgates CONCLUIDO")
    public Integer resgatesConcluidos;

    @Schema(description = "Quantidade de resgates NEGADO")
    public Integer resgatesNegados;

    @Schema(description = "Quantidade de resgates CANCELADO")
    public Integer resgatesCancelados;

    // Catálogo
    @Schema(description = "Quantidade de recompensas ativas")
    public Integer recompensasAtivas;

    @Schema(description = "Estoque total disponível (soma)")
    public Long estoqueTotalRecompensas;

    // Séries temporais (para gráficos)
    @Schema(description = "Transações por dia no período")
    public List<SerieDia<Long>> transacoesPorDia;

    @Schema(description = "Pontos acumulados por dia")
    public List<SerieDia<Long>> pontosAcumuladosPorDia;

    @Schema(description = "Resgates por dia")
    public List<SerieDia<Long>> resgatesPorDia;

    // Tops
    @Schema(description = "Top MCC por quantidade de transações (desc)")
    public List<ItemValor> topMcc;

    @Schema(description = "Top categorias por quantidade de transações (desc)")
    public List<ItemValor> topCategorias;

    // ---- Tipos auxiliares ----
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SerieDia<T> {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        public LocalDate dia;
        public T valor;

        public SerieDia() {}
        public SerieDia(LocalDate dia, T valor) { this.dia = dia; this.valor = valor; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItemValor {
        public String chave;
        public Long valor;

        public ItemValor() {}
        public ItemValor(String chave, Long valor) { this.chave = chave; this.valor = valor; }
    }

    public DashboardDTO() {}

    // ------------------ Builder a partir do domínio ------------------

    /**
     * Constrói o dashboard a partir de coleções do domínio já carregadas.
     * Qualquer parâmetro pode ser null; será tratado como vazio.
     */
    public static DashboardDTO of(LocalDateTime ini,
                                  LocalDateTime fim,
                                  List<Usuario> usuarios,
                                  List<Cartao> cartoes,
                                  List<Transacao> transacoes,
                                  List<MovimentoPontos> movimentos,
                                  List<Resgate> resgates,
                                  List<SaldoPontos> saldos,
                                  List<Recompensa> recompensas) {

        DashboardDTO dto = new DashboardDTO();
        dto.periodoIni = ini;
        dto.periodoFim = fim;

        // Tamanhos básicos
        dto.totalUsuarios = size(usuarios);
        dto.totalCartoes = size(cartoes);

        // Filtro por período (quando aplicável)
        List<Transacao> txPeriodo = filterBy(transacoes, t -> t != null ? t.dataEvento : null, ini, fim);
        dto.totalTransacoes = size(txPeriodo);

        List<MovimentoPontos> movPeriodo = filterBy(movimentos, m -> m != null ? m.criadoEm : null, ini, fim);
        dto.pontosAcumulados = sumMov(movPeriodo, TipoMovimento.ACUMULO);
        dto.pontosExpirados  = sumMov(movPeriodo, TipoMovimento.EXPIRACAO);
        dto.pontosResgatados = sumMov(movPeriodo, TipoMovimento.RESGATE);

        // Saldos consolidados
        dto.saldoTotal = (saldos == null ? 0L : saldos.stream().map(sp -> nz(sp.saldo)).reduce(0L, Long::sum));

        // Resgates (contagem por status, no período se tiver data de criação)
        List<Resgate> resgPeriodo = filterBy(resgates, r -> r != null ? r.criadoEm : null, ini, fim);
        dto.resgatesPendentes  = countResg(resgPeriodo, Resgate.StatusResgate.PENDENTE);
        dto.resgatesAprovados  = countResg(resgPeriodo, Resgate.StatusResgate.APROVADO);
        dto.resgatesConcluidos = countResg(resgPeriodo, Resgate.StatusResgate.CONCLUIDO);
        dto.resgatesNegados    = countResg(resgPeriodo, Resgate.StatusResgate.NEGADO);
        dto.resgatesCancelados = countResg(resgPeriodo, Resgate.StatusResgate.CANCELADO);

        // Recompensas
        if (recompensas != null) {
            dto.recompensasAtivas = (int) recompensas.stream().filter(r -> Boolean.TRUE.equals(r.ativo)).count();
            dto.estoqueTotalRecompensas = recompensas.stream()
                    .map(r -> nz(r.estoque))
                    .reduce(0L, Long::sum);
        } else {
            dto.recompensasAtivas = 0;
            dto.estoqueTotalRecompensas = 0L;
        }

        // Séries temporais
        dto.transacoesPorDia = toSerieDia(countByDate(txPeriodo, t -> t.dataEvento));
        dto.pontosAcumuladosPorDia = toSerieDia(sumByDate(movPeriodo, m -> m.criadoEm,
                m -> m.tipo == TipoMovimento.ACUMULO && m.pontos != null ? Math.abs(m.pontos.longValue()) : 0L));
        dto.resgatesPorDia = toSerieDia(countByDate(resgPeriodo, r -> r.criadoEm));

        // Tops (MCC e categoria) por quantidade
        dto.topMcc = topK(countByKey(txPeriodo, t -> safeUpper(t.mcc)), 5);
        dto.topCategorias = topK(countByKey(txPeriodo, t -> safeUpper(t.categoria)), 5);

        return dto;
    }

    // ------------------ Helpers de agregação ------------------

    private static int size(Collection<?> c) { return c == null ? 0 : c.size(); }

    private static Long nz(Long v) { return v == null ? 0L : v; }

    private static String safeUpper(String s) {
        return (s == null || s.isBlank()) ? "N/A" : s.trim().toUpperCase(Locale.ROOT);
    }

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

    /** Soma absoluta dos pontos para um tipo de movimento (Integer -> long), null-safe. */
    private static Long sumMov(List<MovimentoPontos> lista, TipoMovimento tipo) {
        if (lista == null || lista.isEmpty()) return 0L;
        long total = lista.stream()
                .filter(m -> m != null && m.tipo == tipo && m.pontos != null)
                .mapToLong(m -> Math.abs(m.pontos.longValue()))
                .sum();
        return total;
    }

    private static int countResg(List<Resgate> lista, Resgate.StatusResgate status) {
        if (lista == null) return 0;
        return (int) lista.stream().filter(r -> r != null && r.status == status).count();
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

    private static <T> Map<LocalDate, Long> sumByDate(List<T> list,
                                                      Function<T, LocalDateTime> tsExtractor,
                                                      Function<T, Long> valueExtractor) {
        if (list == null) return Collections.emptyMap();
        Map<LocalDate, Long> acc = new HashMap<>();
        for (T t : list) {
            if (t == null) continue;
            LocalDateTime ts = tsExtractor.apply(t);
            if (ts == null) continue;
            LocalDate d = ts.toLocalDate();
            acc.merge(d, Optional.ofNullable(valueExtractor.apply(t)).orElse(0L), Long::sum);
        }
        return acc;
    }

    private static <T> Map<String, Long> countByKey(List<T> list, Function<T, String> keyExtractor) {
        if (list == null) return Collections.emptyMap();
        return list.stream()
                .filter(Objects::nonNull)
                .map(keyExtractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private static List<SerieDia<Long>> toSerieDia(Map<LocalDate, Long> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SerieDia<>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private static List<ItemValor> topK(Map<String, Long> map, int k) {
        return map.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(k)
                .map(e -> new ItemValor(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
