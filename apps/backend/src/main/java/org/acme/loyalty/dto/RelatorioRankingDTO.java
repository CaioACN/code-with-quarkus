package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DTO de ranking de usuários do programa de pontos.
 * Usado por /relatorios/usuarios/ranking.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RelatorioRanking", description = "Ranking de usuários por pontos e outras métricas")
public class RelatorioRankingDTO {

    // ===================== Parâmetros do relatório =====================

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do período analisado")
    public LocalDateTime periodoInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim do período analisado")
    public LocalDateTime periodoFim;

    @Schema(description = "Critério de ordenação do ranking",
            enumeration = {"PONTOS","ACUMULO","RESGATE","SALDO","VALOR","FREQUENCIA"},
            example = "PONTOS")
    public Criterio criterio = Criterio.PONTOS;

    public enum Criterio {
        /** Pontos líquidos = acumulo + estorno + ajuste − (resgate + expiração). */
        PONTOS,
        /** Soma dos pontos acumulados no período. */
        ACUMULO,
        /** Soma dos pontos resgatados no período. */
        RESGATE,
        /** Saldo atual de pontos (se informado). */
        SALDO,
        /** Valor financeiro total transacionado (se informado). */
        VALOR,
        /** Número de transações (frequência). */
        FREQUENCIA
    }

    @Schema(description = "Máximo de itens no ranking (top N)", example = "100")
    public Integer limite;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp de geração do relatório")
    public LocalDateTime geradoEm = LocalDateTime.now();

    // ===================== Sumários agregados =====================

    @Schema(description = "Total de usuários considerados antes do corte")
    public Integer totalUsuariosConsiderados;

    @Schema(description = "Total de usuários retornados (após limite/filtros)")
    public Integer totalUsuariosRetornados;

    @Schema(description = "Totais agregados do período (apenas dos itens retornados)")
    public Totais agregados = new Totais();

    // ===================== Itens do ranking =====================

    @Schema(description = "Lista ordenada dos usuários no ranking")
    public List<ItemRanking> itens = new ArrayList<>();

    // ======================================================================
    // Tipos auxiliares
    // ======================================================================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Totais agregados de pontos e transações")
    public static class Totais {
        public long pontosLiquidos;
        public long acumulo;
        public long resgate;
        public long expiracao;
        public long estorno;
        public long ajuste;

        public long transacoes;                 // frequência
        public BigDecimal valorTotal = BigDecimal.ZERO; // somatório financeiro

        public void add(ItemRanking it) {
            pontosLiquidos += it.pontosLiquidos;
            acumulo += it.acumulo;
            resgate += it.resgate;
            expiracao += it.expiracao;
            estorno += it.estorno;
            ajuste += it.ajuste;
            transacoes += nz(it.transacoes);
            valorTotal = valorTotal.add(nz(it.valorTotal));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Entrada do ranking (um usuário)")
    public static class ItemRanking {
        @Schema(description = "Posição no ranking (1 = topo)")
        public Integer posicao;

        @Schema(description = "ID do usuário")
        public Long usuarioId;

        @Schema(description = "Nome do usuário (se disponível)")
        public String nome;

        @Schema(description = "E-mail (se disponível)")
        public String email;

        // --- Pontos (período) ---
        @Schema(description = "Pontos líquidos no período")
        public long pontosLiquidos;

        @Schema(description = "Pontos acumulados")
        public long acumulo;

        @Schema(description = "Pontos resgatados")
        public long resgate;

        @Schema(description = "Pontos expirados")
        public long expiracao;

        @Schema(description = "Pontos estornados")
        public long estorno;

        @Schema(description = "Ajustes (podem ser negativos)")
        public long ajuste;

        // --- Saldos/Transações (opcional) ---
        @Schema(description = "Saldo atual de pontos (se fornecido pelo serviço)")
        public Long saldoAtual;

        @Schema(description = "Quantidade de transações no período")
        public Long transacoes;

        @Schema(description = "Valor financeiro total transacionado (moeda base)")
        public BigDecimal valorTotal;

        @Schema(description = "Ticket médio (valorTotal / transacoes)")
        public BigDecimal ticketMedio;

        // --- Cartões (opcional) ---
        @Schema(description = "Quantidade de cartões do usuário")
        public Integer quantidadeCartoes;

        @Schema(description = "Cartão destaque (máscara)")
        public String cartaoTopMascarado;

        // --- Derivados ---
        @Schema(description = "Participação do usuário segundo o critério (fração [0..1])")
        public BigDecimal share;

        /** Recalcula derivados deste item (ex.: ticket médio). */
        public void recompute() {
            if (valorTotal != null && nz(transacoes) > 0) {
                ticketMedio = valorTotal.divide(BigDecimal.valueOf(transacoes), 6, RoundingMode.HALF_UP);
            } else {
                ticketMedio = BigDecimal.ZERO;
            }
        }
    }

    // ======================================================================
    // API de montagem
    // ======================================================================

    public void addItem(ItemRanking item) {
        if (item == null) return;
        item.recompute();
        itens.add(item);
    }

    public ItemRanking newItem(Long usuarioId, String nome, String email) {
        ItemRanking it = new ItemRanking();
        it.usuarioId = usuarioId;
        it.nome = nome;
        it.email = email;
        return it;
    }

    // ======================================================================
    // Ordenação, corte e shares
    // ======================================================================

    /** Ordena itens pelo critério, aplica limite, recalcula posições, somatórios e shares. */
    public void finalizeRanking() {
        totalUsuariosConsiderados = itens.size();

        // Ordena desc conforme critério
        Comparator<ItemRanking> cmp;
        if (criterio == Criterio.PONTOS) {
            cmp = Comparator.comparingLong((ItemRanking i) -> i.pontosLiquidos);
        } else if (criterio == Criterio.ACUMULO) {
            cmp = Comparator.comparingLong((ItemRanking i) -> i.acumulo);
        } else if (criterio == Criterio.RESGATE) {
            cmp = Comparator.comparingLong((ItemRanking i) -> i.resgate);
        } else if (criterio == Criterio.SALDO) {
            cmp = Comparator.comparingLong((ItemRanking i) -> nz(i.saldoAtual));
        } else if (criterio == Criterio.VALOR) {
            cmp = Comparator.comparing((ItemRanking i) -> nz(i.valorTotal));
        } else if (criterio == Criterio.FREQUENCIA) {
            cmp = Comparator.comparingLong((ItemRanking i) -> nz(i.transacoes));
        } else {
            cmp = Comparator.comparingLong((ItemRanking i) -> i.pontosLiquidos);
        }
        // descending
        cmp = cmp.reversed().thenComparing(i -> i.nome, Comparator.nullsLast(String::compareToIgnoreCase));
        itens.sort(cmp);

        // aplica limite (se houver)
        if (limite != null && limite > 0 && itens.size() > limite) {
            itens = new ArrayList<>(itens.subList(0, limite));
        }
        totalUsuariosRetornados = itens.size();

        // posições e agregados
        agregados = new Totais();
        int pos = 1;
        for (ItemRanking it : itens) {
            it.posicao = pos++;
            agregados.add(it);
        }

        // base para share
        BigDecimal baseShare;
        if (criterio == Criterio.PONTOS) {
            baseShare = bd(agregados.pontosLiquidos);
        } else if (criterio == Criterio.ACUMULO) {
            baseShare = bd(agregados.acumulo);
        } else if (criterio == Criterio.RESGATE) {
            baseShare = bd(agregados.resgate);
        } else if (criterio == Criterio.SALDO) {
            baseShare = bd(sum(itens, i -> nz(i.saldoAtual)));
        } else if (criterio == Criterio.VALOR) {
            baseShare = nz(agregados.valorTotal);
        } else if (criterio == Criterio.FREQUENCIA) {
            baseShare = bd(agregados.transacoes);
        } else {
            baseShare = bd(agregados.pontosLiquidos);
        }
        if (baseShare == null || baseShare.signum() == 0) {
            for (ItemRanking it : itens) it.share = BigDecimal.ZERO;
        } else {
            for (ItemRanking it : itens) {
                BigDecimal num;
                if (criterio == Criterio.PONTOS) {
                    num = bd(it.pontosLiquidos);
                } else if (criterio == Criterio.ACUMULO) {
                    num = bd(it.acumulo);
                } else if (criterio == Criterio.RESGATE) {
                    num = bd(it.resgate);
                } else if (criterio == Criterio.SALDO) {
                    num = bd(nz(it.saldoAtual));
                } else if (criterio == Criterio.VALOR) {
                    num = nz(it.valorTotal);
                } else if (criterio == Criterio.FREQUENCIA) {
                    num = bd(nz(it.transacoes));
                } else {
                    num = bd(it.pontosLiquidos);
                }
                it.share = num.divide(baseShare, 6, RoundingMode.HALF_UP);
            }
        }
    }

    // ======================================================================
    // Helpers estáticos
    // ======================================================================

    private static long sum(List<ItemRanking> list, java.util.function.ToLongFunction<ItemRanking> f) {
        long s = 0L;
        if (list != null) for (ItemRanking i : list) if (i != null) s += f.applyAsLong(i);
        return s;
    }

    private static long nz(Long v) { return v == null ? 0L : v; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal bd(long v) { return BigDecimal.valueOf(v); }
}
