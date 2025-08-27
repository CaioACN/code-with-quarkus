package org.acme.loyalty.dto;

public class PageRequestDTO {
    
    public Integer pagina;
    public Integer tamanhoPagina;
    public String ordenacao;
    public String direcao;
    
    // Construtores
    public PageRequestDTO() {
        this.pagina = 1;
        this.tamanhoPagina = 20;
        this.ordenacao = "id";
        this.direcao = "DESC";
    }
    
    public PageRequestDTO(Integer pagina, Integer tamanhoPagina) {
        this.pagina = pagina != null ? pagina : 1;
        this.tamanhoPagina = tamanhoPagina != null ? tamanhoPagina : 20;
        this.ordenacao = "id";
        this.direcao = "DESC";
    }
    
    public PageRequestDTO(Integer pagina, Integer tamanhoPagina, String ordenacao, String direcao) {
        this.pagina = pagina != null ? pagina : 1;
        this.tamanhoPagina = tamanhoPagina != null ? tamanhoPagina : 20;
        this.ordenacao = ordenacao != null ? ordenacao : "id";
        this.direcao = direcao != null ? direcao : "DESC";
    }
    
    // Métodos de negócio
    public Integer getOffset() {
        return (pagina - 1) * tamanhoPagina;
    }
    
    public Integer getLimit() {
        return tamanhoPagina;
    }
    
    public String getOrderBy() {
        return ordenacao + " " + direcao;
    }
    
    public Boolean isValid() {
        return pagina > 0 && tamanhoPagina > 0 && tamanhoPagina <= 100;
    }
}

