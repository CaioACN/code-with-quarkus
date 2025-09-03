package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimento_pontos")
public class MovimentoPontos extends PanacheEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    public Usuario usuario;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false)
    public Cartao cartao;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    public TipoMovimento tipo;
    
    @NotNull
    @Column(name = "pontos", nullable = false)
    public Integer pontos;
    
    @Column(name = "ref_transacao_id")
    public Long refTransacaoId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id")
    public Transacao transacao;
    
    @Size(max = 500)
    @Column(name = "observacao", length = 500)
    public String observacao;
    
    @NotNull
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;
    
    @Column(name = "job_id")
    public String jobId;
    
    @Column(name = "regra_aplicada")
    public String regraAplicada;
    
    @Column(name = "campanha_aplicada")
    public String campanhaAplicada;
    
    // Construtores
    public MovimentoPontos() {}
    
    public MovimentoPontos(Usuario usuario, Cartao cartao, TipoMovimento tipo, 
                           Integer pontos, String observacao) {
        this.usuario = usuario;
        this.cartao = cartao;
        this.tipo = tipo;
        this.pontos = pontos;
        this.observacao = observacao;
        this.criadoEm = LocalDateTime.now();
    }
    
    public MovimentoPontos(Usuario usuario, Cartao cartao, TipoMovimento tipo, 
                           Integer pontos, Transacao transacao, String observacao) {
        this(usuario, cartao, tipo, pontos, observacao);
        this.transacao = transacao;
        this.refTransacaoId = transacao.id;
    }
    
    // Métodos de negócio conforme regra 17.6
    public boolean isAcumulo() {
        return TipoMovimento.ACUMULO.equals(this.tipo);
    }
    
    public boolean isExpiração() {
        return TipoMovimento.EXPIRACAO.equals(this.tipo);
    }
    
    public boolean isResgate() {
        return TipoMovimento.RESGATE.equals(this.tipo);
    }
    
    public boolean isEstorno() {
        return TipoMovimento.ESTORNO.equals(this.tipo);
    }
    
    public boolean isAjuste() {
        return TipoMovimento.AJUSTE.equals(this.tipo);
    }
    
    /**
     * Verifica se o movimento é um crédito conforme regra 17.6:
     * positivo para créditos (ACUMULO, AJUSTE+)
     */
    public boolean isCredito() {
        return (isAcumulo() || isAjuste()) && pontos > 0;
    }
    
    /**
     * Verifica se o movimento é um débito conforme regra 17.6:
     * negativo para débitos (RESGATE, EXPIRACAO, ESTORNO)
     */
    public boolean isDebito() {
        return (isResgate() || isExpiração() || isEstorno()) && pontos < 0;
    }
    
    /**
     * Verifica se o movimento está vinculado a uma transação conforme regra 17.6:
     * ref_transacao_id vincula acúmulos/estornos à transacao de origem
     */
    public boolean estaVinculadoATransacao() {
        return refTransacaoId != null || transacao != null;
    }
    
    public String getDescricaoTipo() {
        switch (this.tipo) {
            case ACUMULO: return "Acúmulo de pontos";
            case EXPIRACAO: return "Expiração de pontos";
            case RESGATE: return "Resgate de pontos";
            case ESTORNO: return "Estorno de pontos";
            case AJUSTE: return "Ajuste de pontos";
            default: return "Movimento de pontos";
        }
    }
    
    // Enum para tipos de movimento
    public enum TipoMovimento {
        ACUMULO,
        EXPIRACAO,
        RESGATE,
        ESTORNO,
        AJUSTE
    }
}

