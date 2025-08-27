package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.RegraConversao;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Relatório de EFETIVIDADE das regras de conversão.
 * Mostra cobertura (quantas transações a regra atingiu), pontos gerados/estornados,
 * valor financeiro associado, séries temporais e “top-N” por MCC/categoria/parceiro.
 *
 * Usado por /relatorios/regras/efetividade.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RelatorioEfetividade", description = "Métricas de efetividade de uma regra de conversão")
public class RelatorioEfetividadeDTO {

    // ===================== Identificação da regra =====================
    @Schema(description = "ID da regra")
    public Long regraId;

    @Schema(description = "Nome da regra")
    public String regraNome;

    @Schema(description = "Ativa?")
    public Boolean ativa;

    @Schema(description = "Prioridade (maior vence em empate)")
    public Integer prioridade;

    @Schema(description = "Regex de MCC (vazio = qualquer MCC)")
    public String mccRegex;

    @Schema(description = "Categoria (vazia = qualquer categoria)")
    public String categoria;

    @Schema(description = "Parceiro alvo (null = qualquer)")
    public Long parceiroId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início de vigência da regra")
    public LocalDateTime vigenciaIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim de vigência da regra (opcional)")
    public LocalDateTime vigenciaFim;

    // ===================== Janela do relatório =====================
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do período analisado")
    public LocalDateTime periodoInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim do período analisado")
    public LocalDateTime periodoFim;

    @Schema(description = "Granularidade da série temporal (diario|semanal|mensal)", example = "diario")
    public String granularidade;

    // ===================== Métricas principais =====================
    @Schema(description = "Transações elegíveis no período (após filtros globais)")
    public Long transacoesElegiveis;

    @Schema(description = "Transações que a regra atingiu/matcheou")
    public Long transacoesAtingidas;

    @Schema(description = "Valor total das transações atingidas (moeda base, ex.: BRL)")
    public BigDecimal valorTotalTransacoes;

    @Schema(description = "Pontos gerados pela regra")
    public Long pontosGerados;

    @Schema(description = "Pontos estornados relacionados à regra")
    public Long pontosEstornados;

    @Schema(description = "Pontos líquidos (gerados - estornados)")
    public Long pontosLiquidos;

    @Schema(description = "Cobertura da regra (atingidas / elegíveis) em fração [0..1]")
    public BigDecimal taxaCobertura;

    @Schema(description = "Pontos líquidos por unidade monetária (ex.: pontos por BRL)")
    public BigDecimal pontosPorValor;

    @Schema(description = "Multiplicador médio observado (aprox. pontos/valor)")
    public BigDecimal multiplicadorMedio;

    // ===================== Séries e Top-N =====================
    @Schema(description = "Série temporal por bucket")
    public List<BucketSerie> serieTemporal;

    @Schema(description = "Top MCCs atingidos")
    public List<TopItem> topMcc;

    @Schema(description = "Top categorias atingidas")
    public List<TopItem> topCategorias;

    @Schema(description = "Top parceiros atingidos")
    public List<TopItem> topParceiros;

    // ===================== Observações / alertas =====================
    @Schema(description = "Avisos/observações geradas durante o cálculo")
    public List<String> avisos;

    public RelatorioEfetividadeDTO() {}

    // ===================== Auxiliares (tipos internos) =====================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Ponto da série temporal")
    public static class BucketSerie {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Início do bucket (ex.: dia/semana/mês)")
        public LocalDateTime bucket;

        @Schema(description = "Transações atingidas no bucket")
        public Long transacoes;

        @Schema(description = "Valor total no bucket")
        public BigDecimal valor;

        @Schema(description = "Pontos líquidos no bucket")
        public Long pontos;

        public BucketSerie() {}

        public BucketSerie(LocalDateTime bucket, Long transacoes, BigDecimal valor, Long pontos) {
            this.bucket = bucket;
            this.transacoes = nz(transacoes);
            this.valor = nz(valor);
            this.pontos = nz(pontos);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Item de ranking (MCC/Categoria/Parceiro)")
    public static class TopItem {
        @Schema(description = "Chave (ex.: MCC 5812, categoria RESTAURANTE, parceiro 123)")
        public String chave;

        @Schema(description = "Transações atingidas")
        public Long transacoes;

        @Schema(description = "Valor total")
        public BigDecimal valor;

        @Schema(description = "Pontos líquidos")
        public Long pontos;

        @Schema(description = "Participação (fração [0..1] do total de pontos)")
        public BigDecimal sharePontos;

        public TopItem() {}

        public TopItem(String chave, Long transacoes, BigDecimal valor, Long pontos) {
            this.chave = chave;
            this.transacoes = nz(transacoes);
            this.valor = nz(valor);
            this.pontos = nz(pontos);
        }
    }

    // ===================== Factories / Builders =====================

    /** Cria o cabeçalho do relatório a partir da entidade de regra. */
    public static RelatorioEfetividadeDTO fromRegra(RegraConversao r,
                                                    LocalDateTime inicio,
                                                    LocalDateTime fim,
                                                    String granularidade) {
        RelatorioEfetividadeDTO dto = new RelatorioEfetividadeDTO();
        if (r != null) {
            dto.regraId = r.id;
            dto.regraNome = r.nome;
            dto.ativa = r.ativo;
            dto.prioridade = r.prioridade;
            dto.mccRegex = r.mccRegex;
            dto.categoria = r.categoria;
            dto.parceiroId = r.parceiroId;
            dto.vigenciaIni = r.vigenciaIni;
            dto.vigenciaFim = r.vigenciaFim;
        }
        dto.periodoInicio = inicio;
        dto.periodoFim = fim;
        dto.granularidade = granularidade;

        // inicia coleções
        dto.serieTemporal = new ArrayList<>();
        dto.topMcc = new ArrayList<>();
        dto.topCategorias = new ArrayList<>();
        dto.topParceiros = new ArrayList<>();
        dto.avisos = new ArrayList<>();

        // zera métricas
        dto.transacoesElegiveis = 0L;
        dto.transacoesAtingidas = 0L;
        dto.valorTotalTransacoes = BigDecimal.ZERO;
        dto.pontosGerados = 0L;
        dto.pontosEstornados = 0L;
        dto.pontosLiquidos = 0L;

        return dto;
    }

    // ===================== Mutators utilitários =====================

    public RelatorioEfetividadeDTO addAviso(String texto) {
        if (texto != null && !texto.isBlank()) {
            if (avisos == null) avisos = new ArrayList<>();
            avisos.add(texto);
        }
        return this;
    }

    public RelatorioEfetividadeDTO addBucket(LocalDateTime bucket, Long qtd, BigDecimal valor, Long pontos) {
        if (serieTemporal == null) serieTemporal = new ArrayList<>();
        serieTemporal.add(new BucketSerie(bucket, qtd, valor, pontos));
        return this;
    }

    public RelatorioEfetividadeDTO addTopMcc(String mcc, Long qtd, BigDecimal valor, Long pontos) {
        if (topMcc == null) topMcc = new ArrayList<>();
        topMcc.add(new TopItem(mcc, qtd, valor, pontos));
        return this;
    }

    public RelatorioEfetividadeDTO addTopCategoria(String cat, Long qtd, BigDecimal valor, Long pontos) {
        if (topCategorias == null) topCategorias = new ArrayList<>();
        topCategorias.add(new TopItem(cat, qtd, valor, pontos));
        return this;
    }

    public RelatorioEfetividadeDTO addTopParceiro(String chave, Long qtd, BigDecimal valor, Long pontos) {
        if (topParceiros == null) topParceiros = new ArrayList<>();
        topParceiros.add(new TopItem(chave, qtd, valor, pontos));
        return this;
    }

    // ===================== Cálculo de derivados =====================

    /** Recalcula métricas derivadas (liquidez, cobertura, pontos/valor, multiplicador, shares). */
    public void recompute() {
        // defaults
        transacoesElegiveis = nz(transacoesElegiveis);
        transacoesAtingidas = nz(transacoesAtingidas);
        valorTotalTransacoes = nz(valorTotalTransacoes);
        pontosGerados = nz(pontosGerados);
        pontosEstornados = nz(pontosEstornados);

        // pontos líquidos
        long liquidos = pontosGerados - Math.max(0L, pontosEstornados);
        pontosLiquidos = Math.max(liquidos, 0L);

        // cobertura
        if (transacoesElegiveis > 0) {
            taxaCobertura = bd(transacoesAtingidas).divide(bd(transacoesElegiveis), 6, RoundingMode.HALF_UP);
        } else {
            taxaCobertura = BigDecimal.ZERO;
        }

        // pontos/valor & multiplicador médio
        if (valorTotalTransacoes.signum() > 0) {
            pontosPorValor = bd(pontosLiquidos).divide(valorTotalTransacoes, 6, RoundingMode.HALF_UP);
            multiplicadorMedio = pontosPorValor; // em regra, pontos ~ valor * multiplicador
        } else {
            pontosPorValor = BigDecimal.ZERO;
            multiplicadorMedio = BigDecimal.ZERO;
        }

        // shares de top-N por pontos
        long totalTopPontos = sumTopPontos(topMcc) + sumTopPontos(topCategorias) + sumTopPontos(topParceiros);
        if (totalTopPontos > 0) {
            applyShare(topMcc, totalTopPontos);
            applyShare(topCategorias, totalTopPontos);
            applyShare(topParceiros, totalTopPontos);
        } else {
            applyZeroShare(topMcc);
            applyZeroShare(topCategorias);
            applyZeroShare(topParceiros);
        }
    }

    // ===================== Helpers internos =====================

    private static long sumTopPontos(List<TopItem> list) {
        if (list == null) return 0L;
        long sum = 0L;
        for (TopItem ti : list) {
            if (ti != null && ti.pontos != null) sum += Math.max(0L, ti.pontos);
        }
        return sum;
    }

    private static void applyShare(List<TopItem> list, long total) {
        if (list == null || total <= 0) return;
        for (TopItem ti : list) {
            long pts = (ti != null && ti.pontos != null) ? Math.max(0L, ti.pontos) : 0L;
            if (ti != null) {
                ti.sharePontos = new BigDecimal(pts).divide(new BigDecimal(total), 6, RoundingMode.HALF_UP);
            }
        }
    }

    private static void applyZeroShare(List<TopItem> list) {
        if (list == null) return;
        for (TopItem ti : list) {
            if (ti != null) ti.sharePontos = BigDecimal.ZERO;
        }
    }

    private static Long nz(Long v) { return v == null ? 0L : v; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal bd(long v) { return BigDecimal.valueOf(v); }

    // ===================== Builder rápido =====================

    public RelatorioEfetividadeDTO withResumo(long elegiveis, long atingidas, BigDecimal valor, long gerados, long estornados) {
        this.transacoesElegiveis = elegiveis;
        this.transacoesAtingidas = atingidas;
        this.valorTotalTransacoes = nz(valor);
        this.pontosGerados = gerados;
        this.pontosEstornados = estornados;
        return this;
    }
}
