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
        this.status = StatusTransacao.PENDENTE;
    }
    
    // Métodos de negócio
    public void processar() {
        this.status = StatusTransacao.PROCESSADA;
        this.processadoEm = LocalDateTime.now();
    }
    
    public void marcarComoProcessada(Integer pontos) {
        this.pontosGerados = pontos;
        processar();
    }
    
    public boolean podeSerProcessada() {
        return StatusTransacao.PENDENTE.equals(this.status);
    }
    
    public void adicionarMovimentoPontos(MovimentoPontos movimento) {
        if (movimentosPontos == null) {
            movimentosPontos = new ArrayList<>();
        }
        movimentosPontos.add(movimento);
        movimento.transacao = this;
    }
    
    // Enum para status
    public enum StatusTransacao {
        PENDENTE,
        PROCESSADA,
        REJEITADA,
        ESTORNADA
    }
}

