package org.acme.loyalty.dto;

import java.time.LocalDateTime;

public class ResgateDTO {
    
    public Long id;
    public Long usuarioId;
    public Long cartaoId;
    public Long recompensaId;
    public String descricaoRecompensa;
    public String tipoRecompensa;
    public Long pontosUtilizados;
    public String status;
    public LocalDateTime criadoEm;
    public LocalDateTime aprovadoEm;
    public LocalDateTime concluidoEm;
    public LocalDateTime negadoEm;
    public String observacao;
    public String motivoNegacao;
    public String codigoRastreio;
    public String parceiroProcessador;
    public String statusDescricao;
    public Long tempoProcessamento;
    public Boolean estaFinalizado;
    
    // Construtores
    public ResgateDTO() {}
    
    public ResgateDTO(Long id, Long usuarioId, Long cartaoId, Long recompensaId, 
                      String descricaoRecompensa, String tipoRecompensa, Long pontosUtilizados, 
                      String status, LocalDateTime criadoEm, LocalDateTime aprovadoEm, 
                      LocalDateTime concluidoEm, LocalDateTime negadoEm, String observacao, 
                      String motivoNegacao, String codigoRastreio, String parceiroProcessador) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.recompensaId = recompensaId;
        this.descricaoRecompensa = descricaoRecompensa;
        this.tipoRecompensa = tipoRecompensa;
        this.pontosUtilizados = pontosUtilizados;
        this.status = status;
        this.criadoEm = criadoEm;
        this.aprovadoEm = aprovadoEm;
        this.concluidoEm = concluidoEm;
        this.negadoEm = negadoEm;
        this.observacao = observacao;
        this.motivoNegacao = motivoNegacao;
        this.codigoRastreio = codigoRastreio;
        this.parceiroProcessador = parceiroProcessador;
        this.statusDescricao = gerarStatusDescricao();
        this.tempoProcessamento = calcularTempoProcessamento();
        this.estaFinalizado = calcularFinalizacao();
    }
    
    // Métodos de negócio
    private String gerarStatusDescricao() {
        if (status == null) return "Status Desconhecido";
        
        if ("PENDENTE".equals(status)) {
            return "Aguardando Aprovação";
        } else if ("APROVADO".equals(status)) {
            return "Aprovado";
        } else if ("CONCLUIDO".equals(status)) {
            return "Concluído";
        } else if ("NEGADO".equals(status)) {
            return "Negado";
        } else if ("CANCELADO".equals(status)) {
            return "Cancelado";
        } else {
            return "Status Desconhecido";
        }
    }
    
    private Long calcularTempoProcessamento() {
        if (criadoEm == null) return null;
        
        LocalDateTime fim = null;
        if (concluidoEm != null) fim = concluidoEm;
        else if (negadoEm != null) fim = negadoEm;
        else if (aprovadoEm != null) fim = aprovadoEm;
        else fim = LocalDateTime.now();
        
        return java.time.Duration.between(criadoEm, fim).toHours();
    }
    
    private Boolean calcularFinalizacao() {
        if (status == null) return false;
        
        return "CONCLUIDO".equals(status) || 
               "NEGADO".equals(status) ||
               "CANCELADO".equals(status);
    }
    
    public String getResumo() {
        StringBuilder resumo = new StringBuilder();
        resumo.append(descricaoRecompensa).append(" - ");
        resumo.append(pontosUtilizados).append(" pontos");
        resumo.append(" | ").append(statusDescricao);
        
        if (tempoProcessamento != null) {
            resumo.append(" | Tempo: ").append(tempoProcessamento).append("h");
        }
        
        return resumo.toString();
    }
    
    public Boolean podeSerAprovado() {
        return "PENDENTE".equals(this.status);
    }
    
    public Boolean podeSerConcluido() {
        return "APROVADO".equals(this.status);
    }
    
    public Boolean podeSerNegado() {
        return "PENDENTE".equals(this.status) || "APROVADO".equals(this.status);
    }
}

