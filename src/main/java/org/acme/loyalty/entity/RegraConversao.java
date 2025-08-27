package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "regra_conversao")
public class RegraConversao extends PanacheEntity {
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "nome", nullable = false, length = 100)
    public String nome;
    
    @NotNull
    @Positive
    @Column(name = "multiplicador", nullable = false, precision = 8, scale = 4)
    public BigDecimal multiplicador;
    
    @Size(max = 100)
    @Column(name = "mcc_regex", length = 100)
    public String mccRegex;
    
    @Size(max = 100)
    @Column(name = "categoria", length = 100)
    public String categoria;
    
    @Column(name = "parceiro_id")
    public Long parceiroId;
    
    @NotNull
    @Column(name = "vigencia_ini", nullable = false)
    public LocalDateTime vigenciaIni;
    
    @Column(name = "vigencia_fim")
    public LocalDateTime vigenciaFim;
    
    @NotNull
    @Column(name = "prioridade", nullable = false)
    public Integer prioridade;
    
    @Column(name = "teto_mensal")
    public Long tetoMensal;
    
    @NotNull
    @Column(name = "ativo", nullable = false)
    public Boolean ativo;
    
    @NotNull
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm;
    
    @Column(name = "atualizado_em")
    public LocalDateTime atualizadoEm;
    
    // Construtores
    public RegraConversao() {}
    
    public RegraConversao(String nome, BigDecimal multiplicador, String mccRegex, 
                          String categoria, Long parceiroId, LocalDateTime vigenciaIni, 
                          LocalDateTime vigenciaFim, Integer prioridade, Long tetoMensal) {
        this.nome = nome;
        this.multiplicador = multiplicador;
        this.mccRegex = mccRegex;
        this.categoria = categoria;
        this.parceiroId = parceiroId;
        this.vigenciaIni = vigenciaIni;
        this.vigenciaFim = vigenciaFim;
        this.prioridade = prioridade;
        this.tetoMensal = tetoMensal;
        this.ativo = true;
        this.criadoEm = LocalDateTime.now();
    }
    
    // Métodos de negócio
    public boolean estaVigente() {
        LocalDateTime agora = LocalDateTime.now();
        return ativo && 
               agora.isAfter(vigenciaIni) && 
               (vigenciaFim == null || agora.isBefore(vigenciaFim));
    }
    
    public boolean aplicaParaMcc(String mcc) {
        if (mccRegex == null || mccRegex.trim().isEmpty()) {
            return true; // Regra aplica para todos os MCCs
        }
        try {
            return Pattern.matches(mccRegex, mcc);
        } catch (Exception e) {
            return false; // Regex inválido
        }
    }
    
    public boolean aplicaParaCategoria(String categoriaTransacao) {
        if (categoria == null || categoria.trim().isEmpty()) {
            return true; // Regra aplica para todas as categorias
        }
        return categoria.equalsIgnoreCase(categoriaTransacao);
    }
    
    public boolean aplicaParaParceiro(Long parceiroIdTransacao) {
        if (parceiroId == null) {
            return true; // Regra aplica para todos os parceiros
        }
        return parceiroId.equals(parceiroIdTransacao);
    }
    
    public Long calcularPontos(BigDecimal valor) {
        return valor.multiply(multiplicador).longValue();
    }
    
    public void desativar() {
        this.ativo = false;
        this.atualizadoEm = LocalDateTime.now();
    }
    
    public void ativar() {
        this.ativo = true;
        this.atualizadoEm = LocalDateTime.now();
    }
    
    public boolean temTetoMensal() {
        return tetoMensal != null && tetoMensal > 0;
    }
}

