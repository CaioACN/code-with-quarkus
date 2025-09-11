package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario", schema = "loyalty")
public class Usuario extends PanacheEntity {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Column(name = "nome", nullable = false, length = 100)
    public String nome;
    
    @Email(message = "Email deve ter formato válido")
    @NotBlank(message = "Email é obrigatório")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    public String email;
    
    @NotNull(message = "Data de cadastro é obrigatória")
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
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (nome != null) nome = nome.trim();
        if (email != null) email = email.trim().toLowerCase();
        
        // Definir data de cadastro se não foi definida
        if (dataCadastro == null) {
            dataCadastro = LocalDate.now();
        }
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
    
    /**
     * Verifica se o nome é válido conforme DDL: nome varchar(100) NOT NULL
     */
    public boolean temNomeValido() {
        return nome != null && !nome.trim().isEmpty() && nome.length() <= 100;
    }
    
    /**
     * Verifica se o email é válido conforme DDL: email varchar(150) NOT NULL UNIQUE
     */
    public boolean temEmailValido() {
        return email != null && 
               email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$") &&
               email.length() <= 150;
    }
    
    /**
     * Verifica se a data de cadastro é válida conforme DDL: data_cadastro date DEFAULT CURRENT_DATE NOT NULL
     */
    public boolean temDataCadastroValida() {
        return dataCadastro != null && !dataCadastro.isAfter(LocalDate.now());
    }
    
    /**
     * Verifica se todos os campos obrigatórios estão preenchidos conforme DDL
     */
    public boolean temTodosCamposObrigatorios() {
        return temNomeValido() && temEmailValido() && temDataCadastroValida();
    }
}

