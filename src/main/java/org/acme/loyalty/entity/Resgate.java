package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "resgate")
public class Resgate extends PanacheEntity {
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    public Usuario usuario;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false)
    public Cartao cartao;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recompensa_id", nullable = false)
    public Recompensa recompensa;
    
    @NotNull
    @Positive
    @Column(name = "pontos_utilizados", nullable = false)
    public Long pontosUtilizados;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public StatusResgate status;
    
    @NotNull
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;
    
    @Column(name = "aprovado_em")
    public LocalDateTime aprovadoEm;
    
    @Column(name = "concluido_em")
    public LocalDateTime concluidoEm;
    
    @Column(name = "negado_em")
    public LocalDateTime negadoEm;
    
    @Size(max = 500)
    @Column(name = "observacao", length = 500)
    public String observacao;
    
    @Size(max = 100)
    @Column(name = "motivo_negacao", length = 100)
    public String motivoNegacao;
    
    @Column(name = "codigo_rastreio")
    public String codigoRastreio;
    
    @Column(name = "parceiro_processador")
    public String parceiroProcessador;
    
    // Construtores
    public Resgate() {}
    
    public Resgate(Usuario usuario, Cartao cartao, Recompensa recompensa, Long pontosUtilizados) {
        this.usuario = usuario;
        this.cartao = cartao;
        this.recompensa = recompensa;
        this.pontosUtilizados = pontosUtilizados;
        this.status = StatusResgate.PENDENTE;
        this.criadoEm = LocalDateTime.now();
    }
    
    // Métodos de negócio
    public void aprovar() {
        this.status = StatusResgate.APROVADO;
        this.aprovadoEm = LocalDateTime.now();
    }
    
    public void concluir() {
        this.status = StatusResgate.CONCLUIDO;
        this.concluidoEm = LocalDateTime.now();
    }
    
    public void negar(String motivo) {
        this.status = StatusResgate.NEGADO;
        this.negadoEm = LocalDateTime.now();
        this.motivoNegacao = motivo;
    }
    
    public void cancelar() {
        this.status = StatusResgate.CANCELADO;
    }
    
    public boolean podeSerAprovado() {
        return StatusResgate.PENDENTE.equals(this.status);
    }
    
    public boolean podeSerConcluido() {
        return StatusResgate.APROVADO.equals(this.status);
    }
    
    public boolean podeSerNegado() {
        return StatusResgate.PENDENTE.equals(this.status) || 
               StatusResgate.APROVADO.equals(this.status);
    }
    
    public boolean estaFinalizado() {
        return StatusResgate.CONCLUIDO.equals(this.status) || 
               StatusResgate.NEGADO.equals(this.status) ||
               StatusResgate.CANCELADO.equals(this.status);
    }
    
    public String getStatusDescricao() {
        switch (this.status) {
            case PENDENTE: return "Aguardando Aprovação";
            case APROVADO: return "Aprovado";
            case CONCLUIDO: return "Concluído";
            case NEGADO: return "Negado";
            case CANCELADO: return "Cancelado";
            default: return "Status Desconhecido";
        }
    }
    
    public Long getTempoProcessamento() {
        if (criadoEm == null) return null;
        
        LocalDateTime fim = null;
        if (concluidoEm != null) fim = concluidoEm;
        else if (negadoEm != null) fim = negadoEm;
        else if (aprovadoEm != null) fim = aprovadoEm;
        else fim = LocalDateTime.now();
        
        return java.time.Duration.between(criadoEm, fim).toHours();
    }
    
    // Enum para status do resgate
    public enum StatusResgate {
        PENDENTE,
        APROVADO,
        CONCLUIDO,
        NEGADO,
        CANCELADO
    }
}

