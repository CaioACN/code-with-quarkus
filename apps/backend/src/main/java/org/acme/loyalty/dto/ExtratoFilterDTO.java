package org.acme.loyalty.dto;

import java.time.LocalDate;

public class ExtratoFilterDTO {
    
    public LocalDate dataInicio;
    public LocalDate dataFim;
    public Long cartaoId;
    public String tipoMovimento;
    public String categoria;
    public Long parceiroId;
    
    // Construtores
    public ExtratoFilterDTO() {}
    
    public ExtratoFilterDTO(LocalDate dataInicio, LocalDate dataFim) {
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }
    
    public ExtratoFilterDTO(LocalDate dataInicio, LocalDate dataFim, Long cartaoId, 
                            String tipoMovimento, String categoria, Long parceiroId) {
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.cartaoId = cartaoId;
        this.tipoMovimento = tipoMovimento;
        this.categoria = categoria;
        this.parceiroId = parceiroId;
    }
    
    // Métodos de negócio
    public Boolean temFiltroData() {
        return dataInicio != null || dataFim != null;
    }
    
    public Boolean temFiltroCartao() {
        return cartaoId != null;
    }
    
    public Boolean temFiltroTipo() {
        return tipoMovimento != null && !tipoMovimento.trim().isEmpty();
    }
    
    public Boolean temFiltroCategoria() {
        return categoria != null && !categoria.trim().isEmpty();
    }
    
    public Boolean temFiltroParceiro() {
        return parceiroId != null;
    }
    
    public Boolean temFiltros() {
        return temFiltroData() || temFiltroCartao() || temFiltroTipo() || 
               temFiltroCategoria() || temFiltroParceiro();
    }
    
    public String getDescricaoFiltros() {
        StringBuilder descricao = new StringBuilder();
        
        if (temFiltroData()) {
            descricao.append("Período: ");
            if (dataInicio != null) descricao.append(dataInicio);
            descricao.append(" até ");
            if (dataFim != null) descricao.append(dataFim);
        }
        
        if (temFiltroCartao()) {
            if (descricao.length() > 0) descricao.append(" | ");
            descricao.append("Cartão: ").append(cartaoId);
        }
        
        if (temFiltroTipo()) {
            if (descricao.length() > 0) descricao.append(" | ");
            descricao.append("Tipo: ").append(tipoMovimento);
        }
        
        if (temFiltroCategoria()) {
            if (descricao.length() > 0) descricao.append(" | ");
            descricao.append("Categoria: ").append(categoria);
        }
        
        if (temFiltroParceiro()) {
            if (descricao.length() > 0) descricao.append(" | ");
            descricao.append("Parceiro: ").append(parceiroId);
        }
        
        return descricao.length() > 0 ? descricao.toString() : "Sem filtros";
    }
}

