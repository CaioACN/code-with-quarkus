package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "regra_conversao", schema = "loyalty")
@Check(constraints = "multiplicador >= 0 AND prioridade >= 0 AND (teto_mensal IS NULL OR teto_mensal > 0)")
public class RegraConversao extends PanacheEntity {
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Column(name = "nome", nullable = false, length = 100)
    public String nome;
    
    @NotNull(message = "Multiplicador é obrigatório")
    @DecimalMin(value = "0.0000", inclusive = true, message = "Multiplicador deve ser maior ou igual a 0.0000")
    @Digits(integer = 4, fraction = 4, message = "Multiplicador deve ter no máximo 4 dígitos inteiros e 4 decimais")
    @Column(name = "multiplicador", nullable = false, precision = 8, scale = 4)
    public BigDecimal multiplicador;
    
    @Size(max = 100, message = "MCC Regex deve ter no máximo 100 caracteres")
    @Column(name = "mcc_regex", length = 100)
    public String mccRegex;
    
    @Size(max = 100, message = "Categoria deve ter no máximo 100 caracteres")
    @Column(name = "categoria", length = 100)
    public String categoria;
    
    @Column(name = "parceiro_id")
    public Long parceiroId;
    
    @NotNull(message = "Data de início da vigência é obrigatória")
    @Column(name = "vigencia_ini", nullable = false)
    public LocalDateTime vigenciaIni;
    
    @Column(name = "vigencia_fim")
    public LocalDateTime vigenciaFim;
    
    @NotNull(message = "Prioridade é obrigatória")
    @Min(value = 0, message = "Prioridade deve ser maior ou igual a 0")
    @Column(name = "prioridade", nullable = false)
    public Integer prioridade;
    
    @Column(name = "teto_mensal")
    public Long tetoMensal;
    
    @NotNull(message = "Status ativo é obrigatório")
    @Column(name = "ativo", nullable = false)
    public Boolean ativo = true;
    
    @NotNull(message = "Data de criação é obrigatória")
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
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (nome != null) nome = nome.trim();
        if (mccRegex != null) mccRegex = mccRegex.trim();
        if (categoria != null) categoria = categoria.trim();
        
        // Normalizar multiplicador para 4 casas decimais
        if (multiplicador != null) {
            multiplicador = multiplicador.setScale(4, java.math.RoundingMode.HALF_UP);
        }
        
        // Definir data de criação se não foi definida
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        
        // Definir ativo se não foi definido
        if (ativo == null) {
            ativo = true;
        }
        
        // Definir prioridade se não foi definida
        if (prioridade == null) {
            prioridade = 0;
        }
    }
    
    // Métodos de negócio conforme regra 17.4
    public boolean estaVigente() {
        LocalDateTime agora = LocalDateTime.now();
        return ativo && 
               agora.isAfter(vigenciaIni) && 
               (vigenciaFim == null || agora.isBefore(vigenciaFim));
    }
    
    /**
     * Verifica se o multiplicador é válido conforme DDL: multiplicador >= 0
     */
    public boolean temMultiplicadorValido() {
        return multiplicador != null && multiplicador.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Verifica se a prioridade é válida conforme DDL: prioridade >= 0
     */
    public boolean temPrioridadeValida() {
        return prioridade != null && prioridade >= 0;
    }
    
    /**
     * Verifica se o teto mensal é válido conforme DDL: teto_mensal IS NULL OR teto_mensal > 0
     */
    public boolean temTetoMensalValido() {
        return tetoMensal == null || tetoMensal > 0;
    }
    
    /**
     * Verifica se o período de vigência é válido
     */
    public boolean temPeriodoValido() {
        if (vigenciaFim == null || vigenciaIni == null) return true;
        return !vigenciaFim.isBefore(vigenciaIni);
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
    
    /**
     * Calcula pontos base conforme regra 17.4:
     * pontos_base = floor(valor * multiplicador) (moeda BRL)
     */
    public Long calcularPontos(BigDecimal valor) {
        if (valor == null || multiplicador == null) {
            return 0L;
        }
        return valor.multiply(multiplicador).longValue(); // floor automático no longValue()
    }
    
    /**
     * Verifica se a regra tem maior prioridade que outra conforme regra 17.4:
     * maior prioridade primeiro; empate → a mais específica
     */
    public boolean temMaiorPrioridadeQue(RegraConversao outra) {
        if (outra == null) return true;
        
        // Maior prioridade primeiro
        if (!this.prioridade.equals(outra.prioridade)) {
            return this.prioridade > outra.prioridade;
        }
        
        // Empate: mais específica (parceiro_id > categoria > mcc_regex > geral)
        int especificidadeThis = getEspecificidade();
        int especificidadeOutra = outra.getEspecificidade();
        
        return especificidadeThis > especificidadeOutra;
    }
    
    /**
     * Calcula especificidade da regra conforme regra 17.4:
     * parceiro_id (4) > categoria (3) > mcc_regex (2) > geral (1)
     */
    private int getEspecificidade() {
        if (parceiroId != null) return 4;
        if (categoria != null && !categoria.trim().isEmpty()) return 3;
        if (mccRegex != null && !mccRegex.trim().isEmpty()) return 2;
        return 1;
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

