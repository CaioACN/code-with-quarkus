package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
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
    @Column(name = "data_cadastro", nullable = false)
    public LocalDateTime dataCadastro;
    
    // Relacionamentos
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Cartao> cartoes;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Transacao> transacoes;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<MovimentoPontos> movimentosPontos;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<SaldoPontos> saldosPontos;
    
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<Resgate> resgates;
    
    // Construtores
    public Usuario() {}
    
    public Usuario(String nome, String email) {
        this.nome = nome;
        this.email = email;
        this.dataCadastro = LocalDateTime.now();
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

