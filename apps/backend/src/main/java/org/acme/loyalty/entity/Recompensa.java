package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recompensa", schema = "loyalty")
@Check(constraints = "custo_pontos > 0 AND estoque >= 0 AND tipo IN ('MILHAS', 'GIFT', 'CASHBACK', 'PRODUTO')")
public class Recompensa extends PanacheEntity {
    
    @NotNull(message = "Tipo é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    public TipoRecompensa tipo;
    
    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 200, message = "Descrição deve ter no máximo 200 caracteres")
    @Column(name = "descricao", nullable = false, length = 200)
    public String descricao;
    
    @NotNull(message = "Custo em pontos é obrigatório")
    @Min(value = 1, message = "Custo em pontos deve ser maior que zero")
    @Column(name = "custo_pontos", nullable = false)
    public Long custoPontos;
    
    @NotNull(message = "Estoque é obrigatório")
    @Min(value = 0, message = "Estoque deve ser maior ou igual a zero")
    @Column(name = "estoque", nullable = false)
    public Long estoque;
    
    @Column(name = "parceiro_id")
    public Long parceiroId;
    
    @NotNull(message = "Status ativo é obrigatório")
    @Column(name = "ativo", nullable = false)
    public Boolean ativo = true;
    
    @Size(max = 500, message = "Detalhes deve ter no máximo 500 caracteres")
    @Column(name = "detalhes", length = 500)
    public String detalhes;
    
    @Size(max = 500, message = "URL da imagem deve ter no máximo 500 caracteres")
    @Column(name = "imagem_url", length = 500)
    public String imagemUrl;
    
    @Column(name = "validade_recompensa")
    public LocalDateTime validadeRecompensa;
    
    @NotNull(message = "Data de criação é obrigatória")
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
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (descricao != null) descricao = descricao.trim();
        if (detalhes != null) detalhes = detalhes.trim();
        if (imagemUrl != null) imagemUrl = imagemUrl.trim();
        
        // Definir data de criação se não foi definida
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        
        // Definir ativo se não foi definido
        if (ativo == null) {
            ativo = true;
        }
    }
    
    // Métodos de negócio
    public boolean estaDisponivel() {
        boolean disponivel = ativo && estoque != null && estoque > 0 && !estaVencida();
        System.out.println("DEBUG: estaDisponivel - ativo: " + ativo + ", estoque: " + estoque + ", !estaVencida(): " + !estaVencida() + ", resultado: " + disponivel);
        return disponivel;
    }
    
    public boolean estaVencida() {
        boolean vencida = validadeRecompensa != null && LocalDateTime.now().isAfter(validadeRecompensa);
        System.out.println("DEBUG: estaVencida - validadeRecompensa: " + validadeRecompensa + ", resultado: " + vencida);
        return vencida;
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
        String status;
        if (!ativo) {
            status = "INATIVA";
        } else if (estoque == null || estoque == 0) {
            status = "SEM_ESTOQUE";
        } else if (estoque < 10) {
            status = "ESTOQUE_BAIXO";
        } else if (estoque < 50) {
            status = "ESTOQUE_MEDIO";
        } else {
            status = "ESTOQUE_ALTO";
        }
        System.out.println("DEBUG: getStatusEstoque - ativo: " + ativo + ", estoque: " + estoque + ", resultado: " + status);
        return status;
    }
    
    public void adicionarResgate(Resgate resgate) {
        if (resgates == null) {
            resgates = new ArrayList<>();
        }
        resgates.add(resgate);
        resgate.recompensa = this;
    }
    
    // Métodos de negócio conforme regra 17.8
    /**
     * Verifica se o custo em pontos é válido conforme DDL: custo_pontos > 0
     */
    public boolean temCustoValido() {
        boolean valido = custoPontos != null && custoPontos > 0;
        System.out.println("DEBUG: temCustoValido - custoPontos: " + custoPontos + ", resultado: " + valido);
        return valido;
    }
    
    /**
     * Verifica se o estoque é válido conforme DDL: estoque >= 0
     */
    public boolean temEstoqueValido() {
        boolean valido = estoque != null && estoque >= 0;
        System.out.println("DEBUG: temEstoqueValido - estoque: " + estoque + ", resultado: " + valido);
        return valido;
    }
    
    /**
     * Verifica se o tipo é válido conforme DDL
     */
    public boolean temTipoValido() {
        return tipo != null;
    }
    
    /**
     * Verifica se a recompensa está disponível conforme regra 17.8:
     * ativo controla visibilidade
     */
    public boolean estaDisponivelParaResgate() {
        boolean disponivel = ativo && temCustoValido() && estaDisponivel();
        System.out.println("DEBUG: estaDisponivelParaResgate - ativo: " + ativo + ", temCustoValido(): " + temCustoValido() + ", estaDisponivel(): " + estaDisponivel() + ", resultado: " + disponivel);
        return disponivel;
    }
    
    /**
     * Decrementa estoque atomicamente conforme regra 17.8:
     * Se estoque não for nulo, controlar decremento atômico para evitar overbooking
     */
    public synchronized boolean decrementarEstoqueAtomicamente(Long quantidade) {
        if (quantidade == null || quantidade <= 0) {
            return false;
        }
        
        if (estoque == null || estoque >= quantidade) {
            estoque -= quantidade;
            atualizadoEm = LocalDateTime.now();
            return true;
        }
        
        return false; // Estoque insuficiente
    }
    
    /**
     * Retorna a descrição do tipo conforme DDL
     */
    public String getDescricaoTipo() {
        if (tipo == null) return "Desconhecido";
        
        switch (this.tipo) {
            case MILHAS: return "Milhas";
            case GIFT: return "Gift Card";
            case CASHBACK: return "Cashback";
            case PRODUTO: return "Produto";
            default: return "Desconhecido";
        }
    }
    
    // Enum para tipos de recompensa (mantido para compatibilidade)
    public enum TipoRecompensa {
        MILHAS("Milhas"),
        GIFT("Gift Card"),
        CASHBACK("Cashback"),
        PRODUTO("Produto");
        
        private final String descricao;
        
        TipoRecompensa(String descricao) {
            this.descricao = descricao;
        }
        
        public String getDescricao() {
            return descricao;
        }
        
        @Override
        public String toString() {
            return name();
        }
    }
}

