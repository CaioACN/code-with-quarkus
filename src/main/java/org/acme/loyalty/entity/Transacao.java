package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transacao")
public class Transacao extends PanacheEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false)
    public Cartao cartao;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    public Usuario usuario;
    
    @NotNull
    @Positive
    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    public BigDecimal valor;
    
    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "moeda", nullable = false, length = 3)
    public String moeda;
    
    @Size(min = 4, max = 4)
    @Column(name = "mcc", length = 4)
    public String mcc;
    
    @Size(max = 60)
    @Column(name = "categoria", length = 60)
    public String categoria;
    
    @Column(name = "parceiro_id")
    public Long parceiroId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public StatusTransacao status;
    
    @Column(name = "autorizacao")
    public String autorizacao; // Para idempotência conforme regra 17.3
    
    @NotNull
    @Column(name = "data_evento", nullable = false)
    public LocalDateTime dataEvento;
    
    @Column(name = "processado_em")
    public LocalDateTime processadoEm;
    
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
     * Verifica se o valor é válido conforme regra 17.3: valor ≥ 0
     */
    public boolean temValorValido() {
        return valor != null && valor.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    // Enum para status conforme regra 17.3
    public enum StatusTransacao {
        APROVADA,    // Transação aprovada e processada
        NEGADA,      // Transação negada (não gera pontos)
        ESTORNADA,   // Transação estornada (gera movimento ESTORNO)
        AJUSTE       // Transação de ajuste manual
    }
}

