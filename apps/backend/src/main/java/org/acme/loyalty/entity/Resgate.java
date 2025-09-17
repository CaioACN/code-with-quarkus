package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;

@Entity
@Table(name = "resgate", schema = "loyalty")
@Check(constraints = "pontos_utilizados > 0 AND status IN ('PENDENTE', 'APROVADO', 'CONCLUIDO', 'NEGADO', 'CANCELADO')")
@SequenceGenerator(name = "resgate_seq", sequenceName = "loyalty.resgate_id_seq", allocationSize = 1)
public class Resgate extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resgate_seq")
    @Column(name = "id")
    public Long id;
    
    @NotNull(message = "Usuário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resgate_usuario"))
    public Usuario usuario;
    
    @NotNull(message = "Cartão é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resgate_cartao"))
    public Cartao cartao;
    
    @NotNull(message = "Recompensa é obrigatória")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recompensa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resgate_recompensa"))
    public Recompensa recompensa;
    
    @NotNull(message = "Pontos utilizados é obrigatório")
    @Min(value = 1, message = "Pontos utilizados deve ser maior que zero")
    @Column(name = "pontos_utilizados", nullable = false)
    public Long pontosUtilizados;
    
    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    public StatusResgate status;
    
    @NotNull(message = "Data de criação é obrigatória")
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;
    
    @Column(name = "aprovado_em")
    public LocalDateTime aprovadoEm;
    
    @Column(name = "concluido_em")
    public LocalDateTime concluidoEm;
    
    @Column(name = "negado_em")
    public LocalDateTime negadoEm;
    
    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    @Column(name = "observacao", length = 500)
    public String observacao;
    
    @Size(max = 100, message = "Motivo da negação deve ter no máximo 100 caracteres")
    @Column(name = "motivo_negacao", length = 100)
    public String motivoNegacao;
    
    @Size(max = 100, message = "Código de rastreio deve ter no máximo 100 caracteres")
    @Column(name = "codigo_rastreio", length = 100)
    public String codigoRastreio;
    
    @Size(max = 100, message = "Parceiro processador deve ter no máximo 100 caracteres")
    @Column(name = "parceiro_processador", length = 100)
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
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (observacao != null) observacao = observacao.trim();
        if (motivoNegacao != null) motivoNegacao = motivoNegacao.trim();
        if (codigoRastreio != null) codigoRastreio = codigoRastreio.trim();
        if (parceiroProcessador != null) parceiroProcessador = parceiroProcessador.trim();
        
        // Definir data de criação se não foi definida
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        
        // Definir status padrão se não foi definido
        if (status == null) {
            status = StatusResgate.PENDENTE;
        }
    }
    
    // Métodos de negócio conforme regra 17.9
    /**
     * Aprova o resgate conforme regra 17.9:
     * Ao APROVAR, registrar movimento_pontos(RESGATE) negativo
     */
    public void aprovar() {
        this.status = StatusResgate.APROVADO;
        this.aprovadoEm = LocalDateTime.now();
    }
    
    /**
     * Conclui o resgate conforme regra 17.9:
     * quando aplicável, decrementar estoque da recompensa
     */
    public void concluir() {
        this.status = StatusResgate.CONCLUIDO;
        this.concluidoEm = LocalDateTime.now();
    }
    
    /**
     * Nega o resgate conforme regra 17.9:
     * Em NEGADO após reserva, lançar ESTORNO dos pontos
     */
    public void negar(String motivo) {
        this.status = StatusResgate.NEGADO;
        this.negadoEm = LocalDateTime.now();
        this.motivoNegacao = motivo;
    }
    
    /**
     * Cancela o resgate conforme regra 17.9:
     * Em CANCELADO após reserva, lançar ESTORNO dos pontos
     */
    public void cancelar() {
        this.status = StatusResgate.CANCELADO;
    }
    
    /**
     * Verifica se o resgate precisa de estorno conforme regra 17.9:
     * Em NEGADO/CANCELADO após reserva, lançar ESTORNO dos pontos
     */
    public boolean precisaEstorno() {
        return (StatusResgate.NEGADO.equals(this.status) || 
                StatusResgate.CANCELADO.equals(this.status)) &&
               aprovadoEm != null; // Foi aprovado antes de ser negado/cancelado
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
        if (status == null) return "Status Desconhecido";
        
        if (status == StatusResgate.PENDENTE) return "Aguardando Aprovação";
        if (status == StatusResgate.APROVADO) return "Aprovado";
        if (status == StatusResgate.CONCLUIDO) return "Concluído";
        if (status == StatusResgate.NEGADO) return "Negado";
        if (status == StatusResgate.CANCELADO) return "Cancelado";
        
        return "Status Desconhecido";
    }
    
    /**
     * Verifica se o status é válido conforme DDL
     */
    public boolean temStatusValido() {
        return status != null;
    }
    
    /**
     * Verifica se os pontos utilizados são válidos conforme DDL: pontos_utilizados > 0
     */
    public boolean temPontosValidos() {
        return pontosUtilizados != null && pontosUtilizados > 0;
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
    
    // Enum para status do resgate conforme DDL
    public enum StatusResgate {
        PENDENTE,
        APROVADO,
        CONCLUIDO,
        NEGADO,
        CANCELADO
    }
}

