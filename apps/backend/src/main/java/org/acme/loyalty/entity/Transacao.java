package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transacao", schema = "loyalty")
@Check(constraints = "valor >= 0 AND status IN ('APROVADA', 'NEGADA', 'ESTORNADA', 'AJUSTE') AND (mcc IS NULL OR length(mcc) = 4) AND length(moeda) = 3")
public class Transacao extends PanacheEntity {
    
    @NotNull(message = "Cartão é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transacao_cartao"))
    public Cartao cartao;
    
    @NotNull(message = "Usuário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transacao_usuario"))
    public Usuario usuario;
    
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.00", inclusive = true, message = "Valor deve ser maior ou igual a 0.00")
    @Digits(integer = 10, fraction = 2, message = "Valor deve ter no máximo 10 dígitos inteiros e 2 decimais")
    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    public BigDecimal valor;
    
    @NotBlank(message = "Moeda é obrigatória")
    @Size(min = 3, max = 3, message = "Moeda deve ter exatamente 3 caracteres")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Moeda deve ter exatamente 3 letras maiúsculas")
    @Column(name = "moeda", nullable = false, length = 3)
    public String moeda;
    
    @Size(min = 4, max = 4, message = "MCC deve ter exatamente 4 caracteres")
    @Pattern(regexp = "^\\d{4}$", message = "MCC deve ter exatamente 4 dígitos")
    @Column(name = "mcc", length = 4)
    public String mcc;
    
    @Size(max = 60, message = "Categoria deve ter no máximo 60 caracteres")
    @Column(name = "categoria", length = 60)
    public String categoria;
    
    @Column(name = "parceiro_id")
    public Long parceiroId;
    
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public StatusTransacao status;
    
    @Size(max = 100, message = "Autorização deve ter no máximo 100 caracteres")
    @Column(name = "autorizacao", length = 100)
    public String autorizacao; // Para idempotência conforme regra 17.3
    
    @NotNull(message = "Data do evento é obrigatória")
    @Column(name = "data_evento", nullable = false)
    public LocalDateTime dataEvento;
    
    @Column(name = "processado_em")
    public LocalDateTime processadoEm;
    
    @Min(value = 0, message = "Pontos gerados deve ser maior ou igual a zero")
    @Column(name = "pontos_gerados")
    public Integer pontosGerados;
    
    // Relacionamentos
    @OneToMany(mappedBy = "transacao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<MovimentoPontos> movimentosPontos;
    
    // Construtores
    public Transacao() {}
    
    public Transacao(Cartao cartao, Usuario usuario, BigDecimal valor, String moeda, 
                     String mcc, String categoria, Long parceiroId, LocalDateTime dataEvento) {
        this.cartao = cartao;
        this.usuario = usuario;
        this.valor = valor;
        this.moeda = moeda;
        this.mcc = mcc;
        this.categoria = categoria;
        this.parceiroId = parceiroId;
        this.dataEvento = dataEvento;
        this.status = StatusTransacao.APROVADA;
    }
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (moeda != null) moeda = moeda.trim().toUpperCase();
        if (mcc != null) mcc = mcc.trim();
        if (categoria != null) categoria = categoria.trim();
        if (autorizacao != null) autorizacao = autorizacao.trim();
        
        // Normalizar valor para 2 casas decimais
        if (valor != null) {
            valor = valor.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        
        // Definir status padrão se não foi definido
        if (status == null) {
            status = StatusTransacao.APROVADA;
        }
    }
    
    // Métodos de negócio
    public void processar() {
        this.processadoEm = LocalDateTime.now();
    }
    
    public void marcarComoProcessada(Integer pontos) {
        this.pontosGerados = pontos;
        processar();
    }
    
    public boolean podeSerProcessada() {
        return StatusTransacao.APROVADA.equals(this.status);
    }
    
    public void adicionarMovimentoPontos(MovimentoPontos movimento) {
        if (movimentosPontos == null) {
            movimentosPontos = new ArrayList<>();
        }
        movimentosPontos.add(movimento);
        movimento.transacao = this;
    }
    
    // Métodos de negócio conforme regra 17.3
    /**
     * Verifica se a transação pode gerar pontos conforme regra 17.3:
     * - NEGADA não gera pontos
     */
    public boolean podeGerarPontos() {
        return !StatusTransacao.NEGADA.equals(this.status);
    }
    
    /**
     * Verifica se a transação foi estornada conforme regra 17.3:
     * - ESTORNADA deve produzir movimento_pontos(ESTORNO)
     */
    public boolean foiEstornada() {
        return StatusTransacao.ESTORNADA.equals(this.status);
    }
    
    /**
     * Gera chave natural para idempotência conforme regra 17.3:
     * cartao_id + data_evento + autorizacao (se existir)
     */
    public String getChaveNatural() {
        StringBuilder chave = new StringBuilder();
        chave.append(cartao.id).append("_");
        chave.append(dataEvento.toString()).append("_");
        if (autorizacao != null && !autorizacao.trim().isEmpty()) {
            chave.append(autorizacao);
        }
        return chave.toString();
    }
    
    /**
     * Verifica se o valor é válido conforme DDL: valor >= 0
     */
    public boolean temValorValido() {
        return valor != null && valor.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Verifica se a moeda é válida conforme DDL: length(moeda) = 3
     */
    public boolean temMoedaValida() {
        return moeda != null && moeda.length() == 3;
    }
    
    /**
     * Verifica se o MCC é válido conforme DDL: mcc IS NULL OR length(mcc) = 4
     */
    public boolean temMccValido() {
        return mcc == null || mcc.length() == 4;
    }
    
    /**
     * Verifica se o status é válido conforme DDL
     */
    public boolean temStatusValido() {
        return status != null;
    }
    
    // Enum para status conforme regra 17.3
    public enum StatusTransacao {
        APROVADA,    // Transação aprovada e processada
        NEGADA,      // Transação negada (não gera pontos)
        ESTORNADA,   // Transação estornada (gera movimento ESTORNO)
        AJUSTE       // Transação de ajuste manual
    }
}

