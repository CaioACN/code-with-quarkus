package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Relatório de RESGATES (pedidos de recompensa).
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RelatorioResgates", description = "Métricas e séries sobre resgates (quantidade, pontos, SLA, top-N)")
public class RelatorioResgatesDTO {

    // ===================== Parâmetros / Filtros =====================

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do período analisado", example = "2025-08-01T00:00:00")
    public LocalDateTime periodoInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim do período analisado", example = "2025-08-31T23:59:59")
    public LocalDateTime periodoFim;

    @Schema(description = "Agrupamento da série temporal", enumeration = {"diario","semanal","mensal"}, example = "diario")
    public String agrupamento = "diario";

    @Schema(description = "Filtro opcional por status",
            enumeration = {"PENDENTE","APROVADO","CONCLUIDO","NEGADO","CANCELADO"})
    public Status statusFiltro;

    @Schema(description = "Limite de itens em Top-N (recompensas/usuários/parceiros)", example = "10")
    @Min(1)
    public Integer limiteTopN = 10;

    // ===================== Sumário / Métricas =====================

    @Schema(description = "Totais agregados no período")
    public Totais totais = new Totais();

    @Schema(description = "Métricas de SLA (tempos médios em horas)")
    public SlaMetricas sla = new SlaMetricas();

    // ===================== Séries e Rankings =====================

    @Schema(description = "Série temporal por bucket (ex.: dia)")
    public List<BucketSerie> serieTemporal = new ArrayList<>();

    @Schema(description = "Top recompensas mais resgatadas")
    public List<TopRecompensa> topRecompensas = new ArrayList<>();

    @Schema(description = "Top parceiros processadores por volume")
    public List<TopParceiro> topParceiros = new ArrayList<>();

    @Schema(description = "Top usuários por pontos resgatados")
    public List<TopUsuario> topUsuarios = new ArrayList<>();

    // ===================== Tipos auxiliares =====================

    public enum Status { PENDENTE, APROVADO, CONCLUIDO, NEGADO, CANCELADO }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Totais consolidados")
    public static class Totais {
        @Schema(description = "Quantidade total de pedidos")
        public long total;

        @Schema(description = "Pontos solicitados (soma de pontosUtilizados de todos os pedidos)")
        public long pontosSolicitados;

        @Schema(description = "Pedidos por status")
        public long pendentes, aprovados, concluidos, negados, cancelados;

        @Schema(description = "Pontos efetivamente debitados (normalmente CONCLUIDO)")
        public long pontosDebitados;

        @Schema(description = "Taxa de aprovação = aprovados/(aprovados+negados)")
        public BigDecimal taxaAprovacao;

        @Schema(description = "Taxa de conclusão = concluidos/total")
        public BigDecimal taxaConclusao;

        public void recompute() {
            total = pendentes + aprovados + concluidos + negados + cancelados;
            taxaAprovacao = ratio(aprovados, (aprovados + negados), 6);
            taxaConclusao = ratio(concluidos, total, 6);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Métricas de SLA (médias em horas)")
    public static class SlaMetricas {
        @Schema(description = "Tempo médio até aprovação (criado→aprovado)")
        public BigDecimal horasAteAprovacao;

        @Schema(description = "Tempo médio até conclusão (criado→concluído)")
        public BigDecimal horasAteConclusao;

        @Schema(description = "Tempo médio do ciclo total entre os concluidos (aprovado→concluído)")
        public BigDecimal horasCicloConclusao;

        // acumuladores internos
        public long _countAprovados;
        public long _countConcluidos;
        public BigDecimal _sumHorasCriadoAprovado = BigDecimal.ZERO;
        public BigDecimal _sumHorasCriadoConcluido = BigDecimal.ZERO;
        public BigDecimal _sumHorasAprovadoConcluido = BigDecimal.ZERO;

        public void addCriadoAprovado(Duration d) {
            if (d == null) return;
            _countAprovados++;
            _sumHorasCriadoAprovado = _sumHorasCriadoAprovado.add(toHours(d));
        }
        public void addCriadoConcluido(Duration d) {
            if (d == null) return;
            _countConcluidos++;
            _sumHorasCriadoConcluido = _sumHorasCriadoConcluido.add(toHours(d));
        }
        public void addAprovadoConcluido(Duration d) {
            if (d == null) return;
            _sumHorasAprovadoConcluido = _sumHorasAprovadoConcluido.add(toHours(d));
        }
        public void recompute() {
            horasAteAprovacao   = (_countAprovados  > 0) ? _sumHorasCriadoAprovado.divide(BigDecimal.valueOf(_countAprovados), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            horasAteConclusao   = (_countConcluidos > 0) ? _sumHorasCriadoConcluido.divide(BigDecimal.valueOf(_countConcluidos), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            horasCicloConclusao = (_countConcluidos > 0) ? _sumHorasAprovadoConcluido.divide(BigDecimal.valueOf(_countConcluidos), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Bucket agregado por período")
    public static class BucketSerie {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Início do bucket")
        public LocalDate bucket;

        @Schema(description = "Pedidos criados no bucket")
        public long criados;

        @Schema(description = "Pedidos concluídos no bucket")
        public long concluidos;

        @Schema(description = "Pedidos aprovados no bucket")
        public long aprovados;

        @Schema(description = "Pedidos negados no bucket")
        public long negados;

        @Schema(description = "Pedidos cancelados no bucket")
        public long cancelados;

        @Schema(description = "Pontos solicitados no bucket")
        public long pontosSolicitados;

        @Schema(description = "Pontos debitados no bucket (concluídos)")
        public long pontosDebitados;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Item de ranking por recompensa")
    public static class TopRecompensa {
        public Long recompensaId;
        public String descricao;
        public String tipo;      // ex.: "PRODUTO_FISICO"
        public Long parceiroId;  // se aplicável

        public long quantidade;
        public long pontos;
        public BigDecimal share; // fração [0..1] por pontos
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Item de ranking por parceiro")
    public static class TopParceiro {
        public Long parceiroId;
        public long quantidade;
        public long pontos;
        public BigDecimal share;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Item de ranking por usuário")
    public static class TopUsuario {
        public Long usuarioId;
        public String nome;
        public long quantidade;
        public long pontos;
        public BigDecimal share;
    }

    // ===================== API de montagem incremental =====================

    public void addRegistro(LocalDateTime criadoEm,
                            LocalDateTime aprovadoEm,
                            LocalDateTime concluidoEm,
                            Status statusFinal,
                            long pontos,
                            Long recompensaId,
                            String recompensaDesc,
                            String recompensaTipo,
                            Long parceiroId,
                            Long usuarioId,
                            String usuarioNome) {

        // Série temporal (bucket por dia da criação)
        LocalDate dia = (criadoEm != null ? criadoEm.toLocalDate() : LocalDate.now());
        BucketSerie b = ensureBucket(dia);
        b.criados++;
        b.pontosSolicitados += Math.max(0L, pontos);

        // Contadores por status
        if (statusFinal == Status.PENDENTE) {
            totais.pendentes++;
        } else if (statusFinal == Status.APROVADO) {
            totais.aprovados++;
            b.aprovados++;
        } else if (statusFinal == Status.CONCLUIDO) {
            totais.concluidos++;
            b.concluidos++;
            b.pontosDebitados += Math.max(0L, pontos);
            totais.pontosDebitados += Math.max(0L, pontos);
        } else if (statusFinal == Status.NEGADO) {
            totais.negados++;
            b.negados++;
        } else if (statusFinal == Status.CANCELADO) {
            totais.cancelados++;
            b.cancelados++;
        }

        // Pontos agregados
        totais.pontosSolicitados += Math.max(0L, pontos);

        // SLA
        if (aprovadoEm != null && criadoEm != null) {
            sla.addCriadoAprovado(Duration.between(criadoEm, aprovadoEm));
        }
        if (concluidoEm != null && criadoEm != null) {
            sla.addCriadoConcluido(Duration.between(criadoEm, concluidoEm));
        }
        if (concluidoEm != null && aprovadoEm != null) {
            sla.addAprovadoConcluido(Duration.between(aprovadoEm, concluidoEm));
        }

        // Top Recompensas
        if (recompensaId != null) {
            TopRecompensa tr = ensureTopRecompensa(recompensaId, recompensaDesc, recompensaTipo, parceiroId);
            tr.quantidade++;
            tr.pontos += Math.max(0L, pontos);
        }

        // Top Parceiros
        if (parceiroId != null) {
            TopParceiro tp = ensureTopParceiro(parceiroId);
            tp.quantidade++;
            tp.pontos += Math.max(0L, pontos);
        }

        // Top Usuários
        if (usuarioId != null) {
            TopUsuario tu = ensureTopUsuario(usuarioId, usuarioNome);
            tu.quantidade++;
            tu.pontos += Math.max(0L, pontos);
        }
    }

    /** Finaliza o relatório: recomputa totais, SLAs e Top-N (ordenados e limitados). */
    public void recomputeAndTrim() {
        // Totais e SLA
        totais.recompute();
        sla.recompute();

        // Ordena série temporal
        if (serieTemporal != null) {
            serieTemporal.sort(Comparator.comparing(s -> s.bucket));
        }

        // Ordena e limita Top-N (por pontos, depois quantidade)
        Comparator<TopRecompensa> cr = Comparator
                .comparingLong((TopRecompensa t) -> t.pontos).reversed()
                .thenComparingLong(t -> t.quantidade).reversed()
                .thenComparing(t -> t.descricao, Comparator.nullsLast(String::compareToIgnoreCase));
        Comparator<TopParceiro> cp = Comparator
                .comparingLong((TopParceiro t) -> t.pontos).reversed()
                .thenComparingLong(t -> t.quantidade).reversed()
                .thenComparingLong(t -> (t.parceiroId == null ? -1L : t.parceiroId));
        Comparator<TopUsuario> cu = Comparator
                .comparingLong((TopUsuario t) -> t.pontos).reversed()
                .thenComparingLong(t -> t.quantidade).reversed()
                .thenComparing(t -> t.nome, Comparator.nullsLast(String::compareToIgnoreCase));

        topRecompensas.sort(cr);
        topParceiros.sort(cp);
        topUsuarios.sort(cu);

        if (limiteTopN != null && limiteTopN > 0) {
            if (topRecompensas.size() > limiteTopN) topRecompensas = new ArrayList<>(topRecompensas.subList(0, limiteTopN));
            if (topParceiros.size()   > limiteTopN) topParceiros   = new ArrayList<>(topParceiros.subList(0, limiteTopN));
            if (topUsuarios.size()    > limiteTopN) topUsuarios    = new ArrayList<>(topUsuarios.subList(0, limiteTopN));
        }

        // Shares por pontos (sem "switch pattern" para Java 17)
        long totalPtsRec = sumPts(topRecompensas);
        long totalPtsPar = sumPts(topParceiros);
        long totalPtsUsu = sumPts(topUsuarios);

        applyShare(topRecompensas, totalPtsRec);
        applyShare(topParceiros, totalPtsPar);
        applyShare(topUsuarios, totalPtsUsu);
    }

    // ===================== Helpers de construção =====================

    private BucketSerie ensureBucket(LocalDate dia) {
        for (BucketSerie b : serieTemporal) {
            if (b.bucket.equals(dia)) return b;
        }
        BucketSerie b = new BucketSerie();
        b.bucket = dia;
        serieTemporal.add(b);
        return b;
    }

    private TopRecompensa ensureTopRecompensa(Long id, String desc, String tipo, Long parceiroId) {
        for (TopRecompensa t : topRecompensas) {
            if (Objects.equals(t.recompensaId, id)) return t;
        }
        TopRecompensa t = new TopRecompensa();
        t.recompensaId = id;
        t.descricao = desc;
        t.tipo = tipo;
        t.parceiroId = parceiroId;
        topRecompensas.add(t);
        return t;
    }

    private TopParceiro ensureTopParceiro(Long parceiroId) {
        for (TopParceiro t : topParceiros) {
            if (Objects.equals(t.parceiroId, parceiroId)) return t;
        }
        TopParceiro t = new TopParceiro();
        t.parceiroId = parceiroId;
        topParceiros.add(t);
        return t;
    }

    private TopUsuario ensureTopUsuario(Long usuarioId, String nome) {
        for (TopUsuario t : topUsuarios) {
            if (Objects.equals(t.usuarioId, usuarioId)) return t;
        }
        TopUsuario t = new TopUsuario();
        t.usuarioId = usuarioId;
        t.nome = nome;
        topUsuarios.add(t);
        return t;
    }

    // ===================== Helpers numéricos =====================

    private static BigDecimal toHours(Duration d) {
        BigDecimal seconds = BigDecimal.valueOf(d.toMillis())
                .divide(BigDecimal.valueOf(1000), 9, RoundingMode.HALF_UP);
        return seconds.divide(BigDecimal.valueOf(3600), 6, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(long num, long den, int scale) {
        if (den <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(num).divide(BigDecimal.valueOf(den), scale, RoundingMode.HALF_UP);
    }

    private static long sumPts(List<?> list) {
        long s = 0;
        if (list == null) return 0;
        for (Object o : list) {
            if (o instanceof TopRecompensa) {
                s += ((TopRecompensa) o).pontos;
            } else if (o instanceof TopParceiro) {
                s += ((TopParceiro) o).pontos;
            } else if (o instanceof TopUsuario) {
                s += ((TopUsuario) o).pontos;
            }
        }
        return s;
    }

    private static void applyShare(List<?> list, long totalPts) {
        if (list == null) return;
        for (Object o : list) {
            BigDecimal share = BigDecimal.ZERO;
            if (totalPts > 0) {
                long pts = 0;
                if (o instanceof TopRecompensa) {
                    pts = ((TopRecompensa) o).pontos;
                } else if (o instanceof TopParceiro) {
                    pts = ((TopParceiro) o).pontos;
                } else if (o instanceof TopUsuario) {
                    pts = ((TopUsuario) o).pontos;
                }
                share = BigDecimal.valueOf(pts).divide(BigDecimal.valueOf(totalPts), 6, RoundingMode.HALF_UP);
            }
            if (o instanceof TopRecompensa) {
                ((TopRecompensa) o).share = share;
            } else if (o instanceof TopParceiro) {
                ((TopParceiro) o).share = share;
            } else if (o instanceof TopUsuario) {
                ((TopUsuario) o).share = share;
            }
        }
    }
}
