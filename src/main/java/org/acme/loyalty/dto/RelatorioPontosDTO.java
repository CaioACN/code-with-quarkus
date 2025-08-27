package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DTO genérico para relatórios de PONTOS:
 * - "Pontos Acumulados" (resumo + série temporal + por usuário/cartão)
 * - "Pontos Expirando"  (resumo por horizonte + itens detalhados)
 *
 * Compatível com Quarkus 3 / IDE Cursor. Não depende de entidades.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RelatorioPontos", description = "Relatórios de pontos (acúmulo, resgates, expiração) com sumários e séries")
public class RelatorioPontosDTO {

    // ===================== Cabeçalho / filtros =====================
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do período analisado")
    public LocalDateTime periodoInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim do período analisado")
    public LocalDateTime periodoFim;

    @Schema(description = "Filtro de usuário (opcional)")
    public Long usuarioId;

    @Schema(description = "Filtro de cartão (opcional)")
    public Long cartaoId;

    @Schema(description = "Moeda base (apenas informativa para correlação com transações)")
    public String moedaBase;

    @Schema(description = "Granularidade da série temporal (diario|semanal|mensal)", example = "diario")
    public String granularidade;

    // ===================== Sumário geral =====================
    @Schema(description = "Totais consolidados no período")
    public Totais totais;

    @Schema(description = "Resumo de pontos expirando por horizonte (30/60/90 dias)")
    public ExpiracaoResumo expiracaoResumo;

    // ===================== Série temporal =====================
    @Schema(description = "Série temporal agregada por bucket (ex.: dia)")
    public List<BucketSerie> serieTemporal;

    // ===================== Quebra por usuário/cartão =====================
    @Schema(description = "Detalhes por usuário (e cartões)")
    public List<LinhaUsuario> usuarios;

    // ===================== Lista detalhada de expirações (opcional) =====================
    @Schema(description = "Itens de expiração detalhados (opcional)")
    public List<ItemExpiracao> expiracoes;

    public RelatorioPontosDTO() {
        this.totais = new Totais();
        this.expiracaoResumo = new ExpiracaoResumo();
        this.serieTemporal = new ArrayList<>();
        this.usuarios = new ArrayList<>();
        this.expiracoes = new ArrayList<>();
    }

    // =====================================================================================
    // Tipos auxiliares
    // =====================================================================================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Totais de pontos no período")
    public static class Totais {
        @Schema(description = "Pontos acumulados")
        public long acumulo;

        @Schema(description = "Pontos expirados")
        public long expiracao;

        @Schema(description = "Pontos resgatados")
        public long resgate;

        @Schema(description = "Pontos estornados (voltam para o saldo)")
        public long estorno;

        @Schema(description = "Ajustes (pode ser positivo ou negativo)")
        public long ajuste;

        @Schema(description = "Saldo inicial (se informado pelo serviço)")
        public Long saldoInicial;

        @Schema(description = "Saldo final (saldoInicial + líquidos)")
        public Long saldoFinal;

        @Schema(description = "Pontos líquidos = acumulo + estorno + ajuste - (resgate + expiracao)")
        public long liquidos;

        public void recompute() {
            this.liquidos = acumulo + estorno + ajuste - (resgate + expiracao);
            if (saldoInicial != null) {
                this.saldoFinal = saldoInicial + liquidos;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Resumo de pontos expirando por horizonte")
    public static class ExpiracaoResumo {
        @Schema(description = "Pontos que expiram em até 30 dias")
        public long dias30;

        @Schema(description = "Pontos que expiram em 31–60 dias")
        public long dias60;

        @Schema(description = "Pontos que expiram em 61–90 dias")
        public long dias90;

        @Schema(description = "Total de pontos a expirar (soma)")
        public long total;

        public void recompute() {
            total = nz(dias30) + nz(dias60) + nz(dias90);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Bucket da série temporal")
    public static class BucketSerie {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Início do bucket (ex.: dia)")
        public LocalDate bucket;

        @Schema(description = "Acúmulo no bucket")
        public long acumulo;

        @Schema(description = "Expiração no bucket")
        public long expiracao;

        @Schema(description = "Resgate no bucket")
        public long resgate;

        @Schema(description = "Estorno no bucket")
        public long estorno;

        @Schema(description = "Ajuste no bucket (pode ser negativo)")
        public long ajuste;

        @Schema(description = "Líquidos no bucket")
        public long liquidos;

        public void recompute() {
            liquidos = acumulo + estorno + ajuste - (resgate + expiracao);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Linha agregada por usuário")
    public static class LinhaUsuario {
        @Schema(description = "ID do usuário")
        public Long id;

        @Schema(description = "Nome (se disponível)")
        public String nome;

        @Schema(description = "E-mail (se disponível)")
        public String email;

        @Schema(description = "Totais do usuário no período")
        public Totais totais = new Totais();

        @Schema(description = "Cartões deste usuário")
        public List<ItemCartao> cartoes = new ArrayList<>();

        public ItemCartao ensureCartao(Long cartaoId, String mascara) {
            for (ItemCartao c : cartoes) {
                if (Objects.equals(c.id, cartaoId)) return c;
            }
            ItemCartao novo = new ItemCartao();
            novo.id = cartaoId;
            novo.numeroMascarado = mascara;
            cartoes.add(novo);
            return novo;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Quebra por cartão dentro de um usuário")
    public static class ItemCartao {
        @Schema(description = "ID do cartão")
        public Long id;

        @Schema(description = "Número mascarado (****-****-****-1234)")
        public String numeroMascarado;

        @Schema(description = "Totais do cartão")
        public Totais totais = new Totais();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Item detalhado de ponto a expirar")
    public static class ItemExpiracao {
        @Schema(description = "ID do usuário")
        public Long usuarioId;

        @Schema(description = "ID do cartão")
        public Long cartaoId;

        @Schema(description = "Quantidade de pontos a expirar")
        public long pontos;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Schema(description = "Data de expiração")
        public LocalDate dataExpiracao;

        @Schema(description = "Dias restantes até a expiração")
        public Integer diasRestantes;
    }

    // =====================================================================================
    // Acúmulo incremental (facilita montar o relatório sem mapear tudo no serviço)
    // =====================================================================================

    public void addMovimento(LocalDateTime quando,
                             Long usuarioId,
                             String usuarioNome,
                             String usuarioEmail,
                             Long cartaoId,
                             String cartaoMasc,
                             TipoMovimento tipo,
                             long pontos) {

        // 1) Atualiza série temporal (bucket por dia)
        LocalDate dia = quando.toLocalDate();
        BucketSerie b = ensureBucket(dia);
        switch (tipo) {
            case ACUMULO   -> b.acumulo += Math.abs(pontos);
            case EXPIRACAO -> b.expiracao += Math.abs(pontos);
            case RESGATE   -> b.resgate += Math.abs(pontos);
            case ESTORNO   -> b.estorno += Math.abs(pontos);
            case AJUSTE    -> b.ajuste += pontos; // ajuste mantém sinal
        }
        b.recompute();

        // 2) Atualiza totais globais
        switch (tipo) {
            case ACUMULO   -> totais.acumulo += Math.abs(pontos);
            case EXPIRACAO -> totais.expiracao += Math.abs(pontos);
            case RESGATE   -> totais.resgate += Math.abs(pontos);
            case ESTORNO   -> totais.estorno += Math.abs(pontos);
            case AJUSTE    -> totais.ajuste += pontos; // pode ser negativo
        }

        // 3) Atualiza por usuário/cartão
        LinhaUsuario lu = ensureUsuario(usuarioId, usuarioNome, usuarioEmail);
        ItemCartao ic = lu.ensureCartao(cartaoId, cartaoMasc);

        switch (tipo) {
            case ACUMULO -> {
                lu.totais.acumulo += Math.abs(pontos);
                ic.totais.acumulo += Math.abs(pontos);
            }
            case EXPIRACAO -> {
                lu.totais.expiracao += Math.abs(pontos);
                ic.totais.expiracao += Math.abs(pontos);
            }
            case RESGATE -> {
                lu.totais.resgate += Math.abs(pontos);
                ic.totais.resgate += Math.abs(pontos);
            }
            case ESTORNO -> {
                lu.totais.estorno += Math.abs(pontos);
                ic.totais.estorno += Math.abs(pontos);
            }
            case AJUSTE -> {
                lu.totais.ajuste += pontos;
                ic.totais.ajuste += pontos;
            }
        }
    }

    /** Adiciona um item de expiração detalhado. */
    public void addExpiracao(Long usuarioId, Long cartaoId, long pontos, LocalDate dataExpiracao) {
        ItemExpiracao it = new ItemExpiracao();
        it.usuarioId = usuarioId;
        it.cartaoId = cartaoId;
        it.pontos = Math.max(0L, pontos);
        it.dataExpiracao = dataExpiracao;
        it.diasRestantes = (dataExpiracao == null) ? null :
                (int) (dataExpiracao.toEpochDay() - LocalDate.now().toEpochDay());
        expiracoes.add(it);

        // Atualiza resumo por horizonte
        int d = it.diasRestantes == null ? Integer.MAX_VALUE : it.diasRestantes;
        if (d <= 30) expiracaoResumo.dias30 += it.pontos;
        else if (d <= 60) expiracaoResumo.dias60 += it.pontos;
        else if (d <= 90) expiracaoResumo.dias90 += it.pontos;
    }

    /** Recalcula campos derivados (líquidos/saldos/expiração total) após montagens. */
    public void recompute() {
        // série temporal
        if (serieTemporal != null) for (BucketSerie s : serieTemporal) if (s != null) s.recompute();

        // totais globais
        if (totais != null) totais.recompute();

        // expiracao
        if (expiracaoResumo != null) expiracaoResumo.recompute();

        // por usuário/cartão
        if (usuarios != null) {
            for (LinhaUsuario u : usuarios) {
                if (u != null && u.totais != null) u.totais.recompute();
                if (u != null && u.cartoes != null) {
                    for (ItemCartao c : u.cartoes) {
                        if (c != null && c.totais != null) c.totais.recompute();
                    }
                }
            }
        }
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    public enum TipoMovimento { ACUMULO, EXPIRACAO, RESGATE, ESTORNO, AJUSTE }

    private BucketSerie ensureBucket(LocalDate dia) {
        for (BucketSerie s : serieTemporal) {
            if (s.bucket.equals(dia)) return s;
        }
        BucketSerie s = new BucketSerie();
        s.bucket = dia;
        serieTemporal.add(s);
        return s;
    }

    private LinhaUsuario ensureUsuario(Long id, String nome, String email) {
        for (LinhaUsuario u : usuarios) {
            if (Objects.equals(u.id, id)) return u;
        }
        LinhaUsuario novo = new LinhaUsuario();
        novo.id = id;
        novo.nome = nome;
        novo.email = email;
        usuarios.add(novo);
        return novo;
    }

    // Utilitário para percentuais em BigDecimal (ex.: cobertura)
    public static BigDecimal ratio(long num, long den, int scale) {
        if (den <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(num).divide(BigDecimal.valueOf(den), scale, RoundingMode.HALF_UP);
    }

    private static long nz(Long v) { return v == null ? 0L : v; }
}
