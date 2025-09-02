package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario")
public class Usuario extends PanacheEntity {
    
    @NotBlank
    @Column(name = "nome", nullable = false, length = 100)
    public String nome;
    
    @Email
    @NotBlank
    @Column(name = "email", nullable = false, unique = true, length = 150)
    public String email;
    
    @NotNull
    @Column(name = "data_cadastro", nullable = false, columnDefinition = "DATE DEFAULT CURRENT_DATE")
    public LocalDate dataCadastro;
    
    // Relacionamentos - efeitos em cascata conforme regra 17.1
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<Cartao> cartoes;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<Transacao> transacoes;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<MovimentoPontos> movimentosPontos;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<SaldoPontos> saldosPontos;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    public List<Resgate> resgates;
    
    // Construtores
    public Usuario() {}
    
    public Usuario(String nome, String email) {
        this.nome = nome;
        this.email = email;
        this.dataCadastro = LocalDate.now(); // Conforme regra 17.1: data_cadastro padrão = CURRENT_DATE
    }
    
    // Métodos de negócio
    public void adicionarCartao(Cartao cartao) {
        if (cartoes == null) {
            cartoes = new ArrayList<>();
        }
        cartoes.add(cartao);
        cartao.usuario = this;
    }
    
    public boolean possuiCartao(Long cartaoId) {
        return cartoes != null && cartoes.stream()
                .anyMatch(c -> c.id.equals(cartaoId));
    }
}

