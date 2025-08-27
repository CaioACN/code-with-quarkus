package org.acme.loyalty.dto;

import java.time.LocalDateTime;

public class RecompensaDTO {
    
    public Long id;
    public String tipo;
    public String descricao;
    public Long custoPontos;
    public Long estoque;
    public Long parceiroId;
    public Boolean ativo;
    public String detalhes;
    public String imagemUrl;
    public LocalDateTime validadeRecompensa;
    public LocalDateTime criadoEm;
    public LocalDateTime atualizadoEm;
    public String statusEstoque;
    public Boolean estaDisponivel;
    
    // Construtores
    public RecompensaDTO() {}
    
    public RecompensaDTO(Long id, String tipo, String descricao, Long custoPontos, 
                         Long estoque, Long parceiroId, Boolean ativo, String detalhes, 
                         String imagemUrl, LocalDateTime validadeRecompensa, 
                         LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.tipo = tipo;
        this.descricao = descricao;
        this.custoPontos = custoPontos;
        this.estoque = estoque;
        this.parceiroId = parceiroId;
        this.ativo = ativo;
        this.detalhes = detalhes;
        this.imagemUrl = imagemUrl;
        this.validadeRecompensa = validadeRecompensa;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
        this.statusEstoque = calcularStatusEstoque();
        this.estaDisponivel = calcularDisponibilidade();
    }
    
    // Métodos de negócio
    private String calcularStatusEstoque() {
        if (!ativo) return "INATIVA";
        if (estoque == 0) return "SEM_ESTOQUE";
        if (estoque < 10) return "ESTOQUE_BAIXO";
        if (estoque < 50) return "ESTOQUE_MEDIO";
        return "ESTOQUE_ALTO";
    }
    
    private Boolean calcularDisponibilidade() {
        if (!ativo || estoque <= 0) return false;
        
        if (validadeRecompensa != null && LocalDateTime.now().isAfter(validadeRecompensa)) {
            return false;
        }
        
        return true;
    }
    
    public String getDescricaoTipo() {
        if (tipo == null) return "Outro";
        
        switch (tipo) {
            case "PRODUTO_FISICO": return "Produto Físico";
            case "PRODUTO_DIGITAL": return "Produto Digital";
            case "DESCONTO": return "Desconto";
            case "CASHBACK": return "Cashback";
            case "MILHAS": return "Milhas";
            case "EXPERIENCIA": return "Experiência";
            default: return "Outro";
        }
    }
    
    public String getResumo() {
        StringBuilder resumo = new StringBuilder();
        resumo.append(descricao).append(" - ");
        resumo.append(custoPontos).append(" pontos");
        
        if (estoque != null && estoque > 0) {
            resumo.append(" | Estoque: ").append(estoque);
        }
        
        if (validadeRecompensa != null) {
            resumo.append(" | Válido até: ").append(validadeRecompensa.toLocalDate());
        }
        
        return resumo.toString();
    }
}

