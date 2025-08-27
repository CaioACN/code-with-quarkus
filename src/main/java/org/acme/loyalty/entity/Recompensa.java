package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recompensa")
public class Recompensa extends PanacheEntity {
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    public TipoRecompensa tipo;
    
    @NotBlank
    @Size(max = 200)
    @Column(name = "descricao", nullable = false, length = 200)
    public String descricao;
    
    @NotNull
    @Positive
    @Column(name = "custo_pontos", nullable = false)
    public Long custoPontos;
    
    @NotNull
    @Column(name = "estoque", nullable = false)
    public Long estoque;
    
    @Column(name = "parceiro_id")
    public Long parceiroId;
    
    @NotNull
    @Column(name = "ativo", nullable = false)
    public Boolean ativo;
    
    @Size(max = 500)
    @Column(name = "detalhes", length = 500)
    public String detalhes;
    
    @Column(name = "imagem_url")
    public String imagemUrl;
    
    @Column(name = "validade_recompensa")
    public LocalDateTime validadeRecompensa;
    
    @NotNull
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;
    
    @Column(name = "atualizado_em")
    public LocalDateTime atualizadoEm;
    
    // Relacionamentos
    @OneToMany(mappedBy = "recompensa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Resgate> resgates;
    
    // Construtores
    public Recompensa() {}
    
    public Recompensa(TipoRecompensa tipo, String descricao, Long custoPontos, 
                      Long estoque, Long parceiroId, String detalhes) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.custoPontos = custoPontos;
        this.estoque = estoque;
        this.parceiroId = parceiroId;
        this.detalhes = detalhes;
        this.ativo = true;
        this.criadoEm = LocalDateTime.now();
    }
    
    // Métodos de negócio
    public boolean estaDisponivel() {
        return ativo && estoque > 0 && !estaVencida();
    }
    
    public boolean estaVencida() {
        return validadeRecompensa != null && LocalDateTime.now().isAfter(validadeRecompensa);
    }
    
    public boolean temEstoqueSuficiente(Long quantidade) {
        return estoque >= quantidade;
    }
    
    public void decrementarEstoque(Long quantidade) {
        if (estoque >= quantidade) {
            estoque -= quantidade;
            atualizadoEm = LocalDateTime.now();
        } else {
            throw new IllegalStateException("Estoque insuficiente");
        }
    }
    
    public void incrementarEstoque(Long quantidade) {
        estoque += quantidade;
        atualizadoEm = LocalDateTime.now();
    }
    
    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = LocalDateTime.now();
    }
    
    public void ativar() {
        this.ativo = true;
        this.atualizadoEm = LocalDateTime.now();
    }
    
    public String getStatusEstoque() {
        if (!ativo) return "INATIVA";
        if (estoque == 0) return "SEM_ESTOQUE";
        if (estoque < 10) return "ESTOQUE_BAIXO";
        if (estoque < 50) return "ESTOQUE_MEDIO";
        return "ESTOQUE_ALTO";
    }
    
    public void adicionarResgate(Resgate resgate) {
        if (resgates == null) {
            resgates = new ArrayList<>();
        }
        resgates.add(resgate);
        resgate.recompensa = this;
    }
    
    // Enum para tipos de recompensa
    public enum TipoRecompensa {
        PRODUTO_FISICO("Produto Físico"),
        PRODUTO_DIGITAL("Produto Digital"),
        DESCONTO("Desconto"),
        CASHBACK("Cashback"),
        MILHAS("Milhas"),
        EXPERIENCIA("Experiência"),
        OUTRO("Outro");
        
        private final String descricao;
        
        TipoRecompensa(String descricao) {
            this.descricao = descricao;
        }
        
        public String getDescricao() {
            return descricao;
        }
    }
}

