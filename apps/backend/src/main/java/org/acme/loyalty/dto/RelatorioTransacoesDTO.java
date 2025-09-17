package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Relatório de VOLUME DE TRANSAÇÕES (quantidade, valor, pontos, status) com séries e Top-N.
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 *
 * Usado por: GET /relatorios/transacoes/volume
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RelatorioTransacoes", description = "Métricas e séries sobre transações (volume, valor, status, Top-N)")
public class RelatorioTransacoesDTO {

    // ===================== Parâmetros / Filtros =====================

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do período analisado")
    public LocalDateTime periodoInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim do período analisado")
    public LocalDateTime periodoFim;

    @Schema(description = "Agrupamento da série temporal", enumeration = {"diario","semanal","mensal"}, example = "diario")
    public String agrupamento = "diario";

    @Schema(description = "Moeda base para somatórios (informativo caso não haja conversão)", example = "BRL")
    public String moedaBase = "BRL";

    @Schema(description = "Limite de itens para Top-N", example = "10")
    @Min(1)
    public Integer limiteTopN = 10;

    // ===================== Sumário / Métricas =====================

    @Schema(description = "Totais agregados")
    public Totais totais = new Totais();

    // ===================== Série temporal e Top-N =====================

    @Schema(description = "Série temporal agregada por bucket (ex.: dia)")
    public List<BucketSerie> serieTemporal = new ArrayList<>();

    @Schema(description = "Top MCCs por valor")
    public List<TopItem> topMcc = new ArrayList<>();

    @Schema(description = "Top Categorias por valor")
    public List<TopItem> topCategorias = new ArrayList<>();

    @Schema(description = "Top Parceiros por valor")
    public List<TopItem> topParceiros = new ArrayList<>();

    @Schema(description = "Top Usuários por valor")
    public List<TopUsuario> topUsuarios = new ArrayList<>();

    // ===================== Tipos auxiliares =====================

    /** Espelha os possíveis status da transação para não acoplar ao domínio. */
    public enum StatusTransacao { PENDENTE, PROCESSADA, REJEITADA, ESTORNADA }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Totais gerais do período")
    public static class Totais {
        @Schema(description = "Quantidade total de transações")
        public long quantidade;

        @Schema(description = "Soma de valores (moeda base)")
        public BigDecimal valorTotal = BigDecimal.ZERO;

        @Schema(description = "Ticket médio (valorTotal/quantidade)")
        public BigDecimal ticketMedio = BigDecimal.ZERO;

        @Schema(description = "Pontos gerados (se informados)")
        public long pontosGerados;

        // Por status
        public long pendentes;
        public long processadas;
        public long rejeitadas;
        public long estornadas;

        public void recompute() {
            if (quantidade > 0) {
                ticketMedio = valorTotal.divide(BigDecimal.valueOf(quantidade), 6, RoundingMode.HALF_UP);
            } else {
                ticketMedio = BigDecimal.ZERO;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Bucket agregado da série temporal")
    public static class BucketSerie {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Início do bucket (ex.: dia)")
        public LocalDate bucket;

        @Schema(description = "Quantidade de transações")
        public long quantidade;

        @Schema(description = "Valor total no bucket")
        public BigDecimal valor = BigDecimal.ZERO;

        @Schema(description = "Ticket médio no bucket")
        public BigDecimal ticketMedio = BigDecimal.ZERO;

        @Schema(description = "Pontos gerados no bucket")
        public long pontosGerados;

        // Por status
        public long pendentes, processadas, rejeitadas, estornadas;

        public void recompute() {
            if (quantidade > 0) {
                ticketMedio = valor.divide(BigDecimal.valueOf(quantidade), 6, RoundingMode.HALF_UP);
            } else {
                ticketMedio = BigDecimal.ZERO;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Entrada de ranking por chave (MCC/Categoria/Parceiro)")
    public static class TopItem {
        @Schema(description = "Chave (ex.: MCC 5812 ou categoria RESTAURANTE ou parceiro 123)")
        public String chave;

        @Schema(description = "Quantidade")
        public long quantidade;

        @Schema(description = "Valor total")
        public BigDecimal valor = BigDecimal.ZERO;

        @Schema(description = "Pontos gerados")
        public long pontos;

        @Schema(description = "Participação pelo valor (fração [0..1])")
        public BigDecimal share = BigDecimal.ZERO;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Entrada de ranking por usuário")
    public static class TopUsuario {
        public Long usuarioId;
        public String nome;
        public long quantidade;
        public BigDecimal valor = BigDecimal.ZERO;
        public long pontos;
        public BigDecimal share = BigDecimal.ZERO;
    }

    // ===================== API de montagem incremental =====================

    /**
     * Registra uma transação para agregação.
     *
     * @param dataEvento   quando ocorreu
     * @param moeda        moeda da transação (informativa)
     * @param valor        valor da transação (assumido já na moeda base se não houver FX)
     * @param pontos       pontos gerados (opcional; use null se não aplicável)
     * @param mcc          MCC (opcional)
     * @param categoria    categoria (opcional)
     * @param parceiroId   parceiro (opcional)
     * @param usuarioId    usuário (opcional)
     * @param usuarioNome  nome do usuário (opcional)
     * @param status       status da transação
     */
    public void addRegistro(LocalDateTime dataEvento,
                            String moeda,
                            BigDecimal valor,
                            Integer pontos,
                            String mcc,
                            String categoria,
                            Long parceiroId,
                            Long usuarioId,
                            String usuarioNome,
                            StatusTransacao status) {

        BigDecimal v = nz(valor);
        int pts = pontos == null ? 0 : Math.max(0, pontos);

        // Série temporal (bucket por dia)
        LocalDate dia = (dataEvento != null ? dataEvento.toLocalDate() : LocalDate.now());
        BucketSerie b = ensureBucket(dia);
        b.quantidade++;
        b.valor = b.valor.add(v);
        b.pontosGerados += pts;
        if (status == StatusTransacao.PENDENTE) {
            b.pendentes++;
        } else if (status == StatusTransacao.PROCESSADA) {
            b.processadas++;
        } else if (status == StatusTransacao.REJEITADA) {
            b.rejeitadas++;
        } else if (status == StatusTransacao.ESTORNADA) {
            b.estornadas++;
        }

        // Totais
        totais.quantidade++;
        totais.valorTotal = totais.valorTotal.add(v);
        totais.pontosGerados += pts;
        if (status == StatusTransacao.PENDENTE) {
            totais.pendentes++;
        } else if (status == StatusTransacao.PROCESSADA) {
            totais.processadas++;
        } else if (status == StatusTransacao.REJEITADA) {
            totais.rejeitadas++;
        } else if (status == StatusTransacao.ESTORNADA) {
            totais.estornadas++;
        }

        // Top-N MCC
        if (mcc != null && !mcc.isBlank()) {
            TopItem t = ensureByKey(topMcc, "MCC " + mcc.trim());
            t.quantidade++;
            t.valor = t.valor.add(v);
            t.pontos += pts;
        }

        // Top-N Categoria
        if (categoria != null && !categoria.isBlank()) {
            TopItem t = ensureByKey(topCategorias, categoria.trim().toUpperCase(Locale.ROOT));
            t.quantidade++;
            t.valor = t.valor.add(v);
            t.pontos += pts;
        }

        // Top-N Parceiro
        if (parceiroId != null) {
            TopItem t = ensureByKey(topParceiros, "PARCEIRO " + parceiroId);
            t.quantidade++;
            t.valor = t.valor.add(v);
            t.pontos += pts;
        }

        // Top-N Usuário
        if (usuarioId != null) {
            TopUsuario u = ensureTopUsuario(usuarioId, usuarioNome);
            u.quantidade++;
            u.valor = u.valor.add(v);
            u.pontos += pts;
        }
    }

    /** Finaliza: calcula derivados, ordena e aplica limite aos Top-N, e ordena série temporal. */
    public void recomputeAndTrim() {
        // Série temporal
        for (BucketSerie s : serieTemporal) if (s != null) s.recompute();
        serieTemporal.sort(Comparator.comparing(s -> s.bucket));

        // Totais
        totais.recompute();

        // Top-N ordenações
        Comparator<TopItem> ct = Comparator
                .comparing((TopItem t) -> t.valor, Comparator.nullsFirst(BigDecimal::compareTo)).reversed()
                .thenComparingLong(t -> t.quantidade).reversed()
                .thenComparing(t -> t.chave, Comparator.nullsLast(String::compareToIgnoreCase));

        topMcc.sort(ct);
        topCategorias.sort(ct);
        topParceiros.sort(ct);

        Comparator<TopUsuario> cu = Comparator
                .comparing((TopUsuario t) -> t.valor, Comparator.nullsFirst(BigDecimal::compareTo)).reversed()
                .thenComparingLong(t -> t.quantidade).reversed()
                .thenComparing(t -> t.nome, Comparator.nullsLast(String::compareToIgnoreCase));
        topUsuarios.sort(cu);

        // Limites
        if (limiteTopN != null && limiteTopN > 0) {
            if (topMcc.size() > limiteTopN) topMcc = new ArrayList<>(topMcc.subList(0, limiteTopN));
            if (topCategorias.size() > limiteTopN) topCategorias = new ArrayList<>(topCategorias.subList(0, limiteTopN));
            if (topParceiros.size() > limiteTopN) topParceiros = new ArrayList<>(topParceiros.subList(0, limiteTopN));
            if (topUsuarios.size() > limiteTopN) topUsuarios = new ArrayList<>(topUsuarios.subList(0, limiteTopN));
        }

        // Shares por valor
        applyShare(topMcc, sumValor(topMcc));
        applyShare(topCategorias, sumValor(topCategorias));
        applyShare(topParceiros, sumValor(topParceiros));
        applyShareUsuarios(topUsuarios, sumValorUsuarios(topUsuarios));
    }

    // ===================== Helpers de construção =====================

    private BucketSerie ensureBucket(LocalDate dia) {
        for (BucketSerie s : serieTemporal) if (s.bucket.equals(dia)) return s;
        BucketSerie s = new BucketSerie();
        s.bucket = dia;
        serieTemporal.add(s);
        return s;
    }

    private TopItem ensureByKey(List<TopItem> lista, String chave) {
        for (TopItem t : lista) if (Objects.equals(t.chave, chave)) return t;
        TopItem t = new TopItem();
        t.chave = chave;
        lista.add(t);
        return t;
    }

    private TopUsuario ensureTopUsuario(Long id, String nome) {
        for (TopUsuario t : topUsuarios) if (Objects.equals(t.usuarioId, id)) return t;
        TopUsuario t = new TopUsuario();
        t.usuarioId = id;
        t.nome = nome;
        topUsuarios.add(t);
        return t;
    }

    // ===================== Helpers numéricos =====================

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static BigDecimal sumValor(List<TopItem> list) {
        BigDecimal s = BigDecimal.ZERO;
        if (list == null) return s;
        for (TopItem t : list) if (t != null && t.valor != null) s = s.add(t.valor);
        return s;
    }

    private static BigDecimal sumValorUsuarios(List<TopUsuario> list) {
        BigDecimal s = BigDecimal.ZERO;
        if (list == null) return s;
        for (TopUsuario t : list) if (t != null && t.valor != null) s = s.add(t.valor);
        return s;
    }

    private static void applyShare(List<TopItem> list, BigDecimal total) {
        if (list == null) return;
        boolean zero = (total == null || total.signum() == 0);
        for (TopItem t : list) {
            if (zero) {
                t.share = BigDecimal.ZERO;
            } else {
                BigDecimal v = (t.valor == null ? BigDecimal.ZERO : t.valor);
                t.share = v.divide(total, 6, RoundingMode.HALF_UP);
            }
        }
    }

    private static void applyShareUsuarios(List<TopUsuario> list, BigDecimal total) {
        if (list == null) return;
        boolean zero = (total == null || total.signum() == 0);
        for (TopUsuario t : list) {
            if (zero) {
                t.share = BigDecimal.ZERO;
            } else {
                BigDecimal v = (t.valor == null ? BigDecimal.ZERO : t.valor);
                t.share = v.divide(total, 6, RoundingMode.HALF_UP);
            }
        }
    }
}
