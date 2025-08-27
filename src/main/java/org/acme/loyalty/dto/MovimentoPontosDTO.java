package org.acme.loyalty.dto;

import java.time.LocalDateTime;

public class MovimentoPontosDTO {
    
    public Long id;
    public Long usuarioId;
    public Long cartaoId;
    public String tipo;
    public Integer pontos;
    public Long refTransacaoId;
    public String observacao;
    public LocalDateTime criadoEm;
    public String jobId;
    public String regraAplicada;
    public String campanhaAplicada;
    public String descricaoTipo;
    
    // Construtores
    public MovimentoPontosDTO() {}
    
    public MovimentoPontosDTO(Long id, Long usuarioId, Long cartaoId, String tipo, 
                              Integer pontos, Long refTransacaoId, String observacao, 
                              LocalDateTime criadoEm, String jobId, String regraAplicada, 
                              String campanhaAplicada) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.tipo = tipo;
        this.pontos = pontos;
        this.refTransacaoId = refTransacaoId;
        this.observacao = observacao;
        this.criadoEm = criadoEm;
        this.jobId = jobId;
        this.regraAplicada = regraAplicada;
        this.campanhaAplicada = campanhaAplicada;
        this.descricaoTipo = gerarDescricaoTipo();
    }
    
    // Métodos de negócio
    private String gerarDescricaoTipo() {
        if (tipo == null) return "Movimento de pontos";
        
        switch (tipo) {
            case "ACUMULO": return "Acúmulo de pontos";
            case "EXPIRACAO": return "Expiração de pontos";
            case "RESGATE": return "Resgate de pontos";
            case "ESTORNO": return "Estorno de pontos";
            case "AJUSTE": return "Ajuste de pontos";
            default: return "Movimento de pontos";
        }
    }
}

