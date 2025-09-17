package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimento_pontos", schema = "loyalty")
@SequenceGenerator(name = "movimento_pontos_seq", sequenceName = "loyalty.movimento_pontos_id_seq", allocationSize = 1)
@Check(constraints = "pontos <> 0 AND tipo IN ('ACUMULO', 'EXPIRACAO', 'RESGATE', 'ESTORNO', 'AJUSTE')")
public class MovimentoPontos extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "movimento_pontos_seq")
    @Column(name = "id")
    public Long id;
    
    @NotNull(message = "Usuário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimento_usuario"))
    public Usuario usuario;
    
    @NotNull(message = "Cartão é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimento_cartao"))
    public Cartao cartao;
    
    @NotNull(message = "Tipo de movimento é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    public TipoMovimento tipo;
    
    @NotNull(message = "Pontos é obrigatório")
    @AssertTrue(message = "Pontos deve ser diferente de zero")
    public boolean isPontosValido() {
        return pontos != null && pontos != 0;
    }
    
    @Column(name = "pontos", nullable = false)
    public Integer pontos;
    
    @Column(name = "ref_transacao_id")
    public Long refTransacaoId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id", foreignKey = @ForeignKey(name = "fk_movimento_transacao"))
    public Transacao transacao;
    
    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    @Column(name = "observacao", length = 500)
    public String observacao;
    
    @NotNull(message = "Data de criação é obrigatória")
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;
    
    @Size(max = 100, message = "Job ID deve ter no máximo 100 caracteres")
    @Column(name = "job_id", length = 100)
    public String jobId;
    
    @Size(max = 200, message = "Regra aplicada deve ter no máximo 200 caracteres")
    @Column(name = "regra_aplicada", length = 200)
    public String regraAplicada;
    
    @Size(max = 200, message = "Campanha aplicada deve ter no máximo 200 caracteres")
    @Column(name = "campanha_aplicada", length = 200)
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
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (observacao != null) observacao = observacao.trim();
        if (jobId != null) jobId = jobId.trim();
        if (regraAplicada != null) regraAplicada = regraAplicada.trim();
        if (campanhaAplicada != null) campanhaAplicada = campanhaAplicada.trim();
        
        // Definir data de criação se não foi definida
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
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
        if (tipo == null) return "Movimento de pontos";
        
        if (this.tipo == TipoMovimento.ACUMULO) {
            return "Acúmulo de pontos";
        } else if (this.tipo == TipoMovimento.EXPIRACAO) {
            return "Expiração de pontos";
        } else if (this.tipo == TipoMovimento.RESGATE) {
            return "Resgate de pontos";
        } else if (this.tipo == TipoMovimento.ESTORNO) {
            return "Estorno de pontos";
        } else if (this.tipo == TipoMovimento.AJUSTE) {
            return "Ajuste de pontos";
        } else {
            return "Movimento de pontos";
        }
    }
    
    /**
     * Valida se o tipo é válido conforme DDL
     */
    public boolean temTipoValido() {
        return tipo != null;
    }
    
    // Enum para tipos de movimento (mantido para compatibilidade)
    public enum TipoMovimento {
        ACUMULO,
        EXPIRACAO,
        RESGATE,
        ESTORNO,
        AJUSTE
    }
}

