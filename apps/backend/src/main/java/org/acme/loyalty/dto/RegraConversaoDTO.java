package org.acme.loyalty.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RegraConversaoDTO {
    
    public Long id;
    public String nome;
    public BigDecimal multiplicador;
    public String mccRegex;
    public String categoria;
    public Long parceiroId;
    public LocalDateTime vigenciaIni;
    public LocalDateTime vigenciaFim;
    public Integer prioridade;
    public Long tetoMensal;
    public Boolean ativo;
    public LocalDateTime criadoEm;
    public LocalDateTime atualizadoEm;
    public Boolean estaVigente;
    
    // Construtores
    public RegraConversaoDTO() {}
    
    public RegraConversaoDTO(Long id, String nome, BigDecimal multiplicador, String mccRegex, 
                             String categoria, Long parceiroId, LocalDateTime vigenciaIni, 
                             LocalDateTime vigenciaFim, Integer prioridade, Long tetoMensal, 
                             Boolean ativo, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.multiplicador = multiplicador;
        this.mccRegex = mccRegex;
        this.categoria = categoria;
        this.parceiroId = parceiroId;
        this.vigenciaIni = vigenciaIni;
        this.vigenciaFim = vigenciaFim;
        this.prioridade = prioridade;
        this.tetoMensal = tetoMensal;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.estaVigente = calcularVigencia();
    }
    
    // Métodos de negócio
    private Boolean calcularVigencia() {
        if (!ativo) return false;
        
        LocalDateTime agora = LocalDateTime.now();
        return agora.isAfter(vigenciaIni) && 
               (vigenciaFim == null || agora.isBefore(vigenciaFim));
    }
    
    public String getDescricaoAplicacao() {
        StringBuilder descricao = new StringBuilder();
        descricao.append("Multiplicador: ").append(multiplicador);
        
        if (mccRegex != null && !mccRegex.trim().isEmpty()) {
            descricao.append(" | MCC: ").append(mccRegex);
        }
        
        if (categoria != null && !categoria.trim().isEmpty()) {
            descricao.append(" | Categoria: ").append(categoria);
        }
        
        if (parceiroId != null) {
            descricao.append(" | Parceiro: ").append(parceiroId);
        }
        
        if (tetoMensal != null && tetoMensal > 0) {
            descricao.append(" | Teto mensal: ").append(tetoMensal);
        }
        
        return descricao.toString();
    }
}

