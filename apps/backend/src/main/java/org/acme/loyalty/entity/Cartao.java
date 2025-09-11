package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cartao", schema = "loyalty")
@Check(constraints = "limite >= 0")
public class Cartao extends PanacheEntity {
    
    @NotBlank(message = "Número do cartão é obrigatório")
    @Size(max = 19, message = "Número do cartão deve ter no máximo 19 caracteres")
    @Pattern(regexp = "\\d{16,19}", message = "Número do cartão deve ter entre 16 e 19 dígitos")
    @Column(name = "numero", nullable = false, unique = true, length = 19)
    public String numero;
    
    @NotBlank(message = "Nome impresso é obrigatório")
    @Size(max = 100, message = "Nome impresso deve ter no máximo 100 caracteres")
    @Column(name = "nome_impresso", nullable = false, length = 100)
    public String nomeImpresso;
    
    @NotNull(message = "Data de validade é obrigatória")
    @Column(name = "validade", nullable = false)
    public LocalDate validade;
    
    @NotNull(message = "Limite é obrigatório")
    @DecimalMin(value = "0.00", inclusive = true, message = "Limite deve ser maior ou igual a 0.00")
    @Digits(integer = 10, fraction = 2, message = "Limite deve ter no máximo 10 dígitos inteiros e 2 decimais")
    @Column(name = "limite", nullable = false, precision = 12, scale = 2)
    public BigDecimal limite;
    
    @NotNull(message = "Usuário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, foreignKey = @ForeignKey(name = "fk_cartao_usuario"))
    public Usuario usuario;
    
    // Relacionamentos
    @OneToMany(mappedBy = "cartao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Transacao> transacoes;
    
    @OneToMany(mappedBy = "cartao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<MovimentoPontos> movimentosPontos;
    
    @OneToMany(mappedBy = "cartao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<SaldoPontos> saldosPontos;
    
    @OneToMany(mappedBy = "cartao", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Resgate> resgates;
    
    // Construtores
    public Cartao() {}
    
    public Cartao(String numero, String nomeImpresso, LocalDate validade, BigDecimal limite, Usuario usuario) {
        this.numero = numero;
        this.nomeImpresso = nomeImpresso;
        this.validade = validade;
        this.limite = limite;
        this.usuario = usuario;
    }
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (numero != null) numero = numero.trim();
        if (nomeImpresso != null) nomeImpresso = nomeImpresso.trim();
        
        // Garantir escala correta para limite (2 decimais)
        if (limite != null) {
            limite = limite.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
    
    // Métodos de negócio conforme regra 17.2
    public boolean estaVencido() {
        return LocalDate.now().isAfter(validade);
    }
    
    public boolean estaProximoVencimento() {
        LocalDate hoje = LocalDate.now();
        LocalDate proximoVencimento = validade.minusMonths(1);
        return hoje.isAfter(proximoVencimento) && !estaVencido();
    }
    
    /**
     * Verifica se o cartão pode receber transações conforme regra 17.2:
     * - Cartão ativo e não vencido
     */
    public boolean podeReceberTransacoes() {
        return !estaVencido(); // Assumindo que cartão ativo por padrão
    }
    
    /**
     * Valida se o limite é válido conforme DDL: limite ≥ 0
     */
    public boolean temLimiteValido() {
        return limite != null && limite.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Valida se o número do cartão é válido conforme DDL: 16-19 dígitos
     */
    public boolean temNumeroValido() {
        return numero != null && numero.matches("\\d{16,19}");
    }
    
    /**
     * Valida se o nome impresso é válido conforme DDL: não nulo e não vazio
     */
    public boolean temNomeImpressoValido() {
        return nomeImpresso != null && !nomeImpresso.trim().isEmpty();
    }
    
    public String getNumeroMascarado() {
        if (numero == null || numero.length() < 4) {
            return numero;
        }
        return "****-****-****-" + numero.substring(numero.length() - 4);
    }
    
    public void adicionarTransacao(Transacao transacao) {
        if (transacoes == null) {
            transacoes = new ArrayList<>();
        }
        transacoes.add(transacao);
        transacao.cartao = this;
    }
}

