package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cartao")
public class Cartao extends PanacheEntity {
    
    @NotBlank
    @Pattern(regexp = "\\d{16,19}", message = "Número do cartão deve ter entre 16 e 19 dígitos")
    @Column(name = "numero", nullable = false, unique = true, length = 19)
    public String numero;
    
    @NotBlank
    @Column(name = "nome_impresso", nullable = false, length = 100)
    public String nomeImpresso;
    
    @NotNull
    @Column(name = "validade", nullable = false)
    public LocalDate validade;
    
    @NotNull
    @Positive
    @Column(name = "limite", nullable = false, precision = 12, scale = 2)
    public BigDecimal limite;
    
    @NotNull
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
     * Valida se o limite é válido conforme regra 17.2: limite ≥ 0
     */
    public boolean temLimiteValido() {
        return limite != null && limite.compareTo(BigDecimal.ZERO) >= 0;
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

