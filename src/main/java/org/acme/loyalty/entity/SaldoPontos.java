package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "saldo_pontos")
@IdClass(SaldoPontosId.class)
public class SaldoPontos extends PanacheEntityBase {
    
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    public Usuario usuario;
    
    @Id
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false)
    public Cartao cartao;
    
    @NotNull
    @Column(name = "saldo", nullable = false)
    public Long saldo;
    
    @NotNull
    @Column(name = "atualizado_em", nullable = false)
    public LocalDateTime atualizadoEm;
    
    @Column(name = "pontos_expirando_30_dias")
    public Long pontosExpirando30Dias;
    
    @Column(name = "pontos_expirando_60_dias")
    public Long pontosExpirando60Dias;
    
    @Column(name = "pontos_expirando_90_dias")
    public Long pontosExpirando90Dias;
    
    // Construtores
    public SaldoPontos() {}
    
    public SaldoPontos(Usuario usuario, Cartao cartao) {
        this.usuario = usuario;
        this.cartao = cartao;
        this.saldo = 0L;
        this.atualizadoEm = LocalDateTime.now();
        this.pontosExpirando30Dias = 0L;
        this.pontosExpirando60Dias = 0L;
        this.pontosExpirando90Dias = 0L;
    }
    
    // Métodos de negócio conforme regra 17.7
    /**
     * Adiciona pontos ao saldo conforme regra 17.7:
     * Mantido somente por operações de negócio (não alterar manualmente)
     */
    public void adicionarPontos(Long pontos) {
        if (pontos != null && pontos > 0) {
            this.saldo += pontos;
            this.atualizadoEm = LocalDateTime.now();
        }
    }
    
    /**
     * Debita pontos do saldo conforme regra 17.7:
     * Impedir saldo negativo: validação em resgates e UPDATE condicional
     */
    public void debitarPontos(Long pontos) {
        if (pontos != null && pontos > 0) {
            if (this.saldo >= pontos) {
                this.saldo -= pontos;
                this.atualizadoEm = LocalDateTime.now();
            } else {
                throw new IllegalStateException("Saldo insuficiente de pontos. Saldo atual: " + this.saldo + ", pontos solicitados: " + pontos);
            }
        }
    }
    
    /**
     * Atualização atômica do saldo conforme regra 17.7:
     * Consistência via UPSERT atômico junto do movimento_pontos
     */
    public void atualizarSaldoAtomicamente(Long novoSaldo) {
        if (novoSaldo != null && novoSaldo >= 0) {
            this.saldo = novoSaldo;
            this.atualizadoEm = LocalDateTime.now();
        } else {
            throw new IllegalArgumentException("Saldo deve ser não negativo");
        }
    }
    
    public void atualizarPontosExpirando(Long pontos30, Long pontos60, Long pontos90) {
        this.pontosExpirando30Dias = pontos30;
        this.pontosExpirando60Dias = pontos60;
        this.pontosExpirando90Dias = pontos90;
        this.atualizadoEm = LocalDateTime.now();
    }
    
    public boolean temSaldoSuficiente(Long pontos) {
        return this.saldo >= pontos;
    }
    
    public boolean temPontosExpirando() {
        return (pontosExpirando30Dias != null && pontosExpirando30Dias > 0) ||
               (pontosExpirando60Dias != null && pontosExpirando60Dias > 0) ||
               (pontosExpirando90Dias != null && pontosExpirando90Dias > 0);
    }
    
    public Long getTotalPontosExpirando() {
        return (pontosExpirando30Dias != null ? pontosExpirando30Dias : 0L) +
               (pontosExpirando60Dias != null ? pontosExpirando60Dias : 0L) +
               (pontosExpirando90Dias != null ? pontosExpirando90Dias : 0L);
    }
    
    public String getStatusSaldo() {
        if (saldo == 0) return "SEM_PONTOS";
        if (saldo < 1000) return "BAIXO";
        if (saldo < 10000) return "MEDIO";
        return "ALTO";
    }
}

