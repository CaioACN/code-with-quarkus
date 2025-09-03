package org.acme.loyalty.dto;

public class ResgateRequestDTO {
    
    public Long usuarioId;
    public Long cartaoId;
    public Long recompensaId;
    public String observacao;
    
    // Construtores
    public ResgateRequestDTO() {}
    
    public ResgateRequestDTO(Long usuarioId, Long cartaoId, Long recompensaId, String observacao) {
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.recompensaId = recompensaId;
        this.observacao = observacao;
    }
}

