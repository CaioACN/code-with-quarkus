package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecompensaSimpleDTO {
    
    @JsonProperty("id")
    public Long id;
    
    @JsonProperty("tipo")
    public String tipo;
    
    @JsonProperty("descricao")
    public String descricao;
    
    @JsonProperty("custoPontos")
    public Long custoPontos;
    
    @JsonProperty("estoque")
    public Long estoque;
    
    @JsonProperty("ativo")
    public Boolean ativo;
    
    public RecompensaSimpleDTO() {}
    
    public RecompensaSimpleDTO(Long id, String tipo, String descricao, Long custoPontos, Long estoque, Boolean ativo) {
        this.id = id;
        this.tipo = tipo;
        this.descricao = descricao;
        this.custoPontos = custoPontos;
        this.estoque = estoque;
        this.ativo = ativo;
    }
}
