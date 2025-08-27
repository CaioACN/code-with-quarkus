package org.acme.loyalty.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CampanhaBonusDTO {
    
    public Long id;
    public String nome;
    public BigDecimal multiplicadorExtra;
    public LocalDateTime vigenciaIni;
    public LocalDateTime vigenciaFim;
    public String segmento;
    public Integer prioridade;
    public Long teto;
    public Boolean ativo;
    public String descricao;
    public LocalDateTime criadoEm;
    public LocalDateTime atualizadoEm;
    public Boolean estaVigente;
    public String statusVigencia;
    
    // Construtores
    public CampanhaBonusDTO() {}
    
    public CampanhaBonusDTO(Long id, String nome, BigDecimal multiplicadorExtra, 
                            LocalDateTime vigenciaIni, LocalDateTime vigenciaFim, 
                            String segmento, Integer prioridade, Long teto, 
                            Boolean ativo, String descricao, LocalDateTime criadoEm, 
                            LocalDateTime atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.multiplicadorExtra = multiplicadorExtra;
        this.vigenciaIni = vigenciaIni;
        this.vigenciaFim = vigenciaFim;
        this.segmento = segmento;
        this.prioridade = prioridade;
        this.teto = teto;
        this.ativo = ativo;
        this.descricao = descricao;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.estaVigente = calcularVigencia();
        this.statusVigencia = calcularStatusVigencia();
    }
    
    // Métodos de negócio
    private Boolean calcularVigencia() {
        if (!ativo) return false;
        
        LocalDateTime agora = LocalDateTime.now();
        return agora.isAfter(vigenciaIni) && 
               (vigenciaFim == null || agora.isBefore(vigenciaFim));
    }
    
    private String calcularStatusVigencia() {
        if (!ativo) return "INATIVA";
        
        if (vigenciaFim != null && LocalDateTime.now().isAfter(vigenciaFim)) {
            return "EXPIRADA";
        }
        
        if (vigenciaFim != null) {
            LocalDateTime proximaExpiracao = vigenciaFim.minusDays(7);
            if (LocalDateTime.now().isAfter(proximaExpiracao)) {
                return "PROXIMA_EXPIRACAO";
            }
        }
        
        return "VIGENTE";
    }
    
    public BigDecimal getMultiplicadorTotal() {
        if (multiplicadorExtra == null) return BigDecimal.ONE;
        return BigDecimal.ONE.add(multiplicadorExtra);
    }
    
    public String getDescricaoAplicacao() {
        StringBuilder descricao = new StringBuilder();
        descricao.append("Multiplicador extra: ").append(multiplicadorExtra);
        
        if (segmento != null && !segmento.trim().isEmpty()) {
            descricao.append(" | Segmento: ").append(segmento);
        }
        
        if (teto != null && teto > 0) {
            descricao.append(" | Teto: ").append(teto);
        }
        
        return descricao.toString();
    }
}

