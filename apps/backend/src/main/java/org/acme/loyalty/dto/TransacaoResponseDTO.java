package org.acme.loyalty.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransacaoResponseDTO {
    
    public Long id;
    public Long cartaoId;
    public Long usuarioId;
    public BigDecimal valor;
    public String moeda;
    public String mcc;
    public String categoria;
    public Long parceiroId;
    public String status;
    public String autorizacao;
    public LocalDateTime dataEvento;
    public LocalDateTime processadoEm;
    public Integer pontosGerados;
    
    // Construtores
    public TransacaoResponseDTO() {}
    
    public TransacaoResponseDTO(Long id) {
        this.id = id;
    }
    
    public TransacaoResponseDTO(Long id, Long cartaoId, Long usuarioId, BigDecimal valor, 
                               String moeda, String mcc, String categoria, Long parceiroId, 
                               String status, String autorizacao, LocalDateTime dataEvento, 
                               LocalDateTime processadoEm, Integer pontosGerados) {
        this.id = id;
        this.cartaoId = cartaoId;
        this.usuarioId = usuarioId;
        this.valor = valor;
        this.moeda = moeda;
        this.mcc = mcc;
        this.categoria = categoria;
        this.parceiroId = parceiroId;
        this.status = status;
        this.autorizacao = autorizacao;
        this.dataEvento = dataEvento;
        this.processadoEm = processadoEm;
        this.pontosGerados = pontosGerados;
    }
    
    // Método estático para criar DTO a partir da entidade
    public static TransacaoResponseDTO fromEntity(org.acme.loyalty.entity.Transacao transacao) {
        return new TransacaoResponseDTO(
            transacao.id,
            transacao.cartao != null ? transacao.cartao.id : null,
            transacao.usuario != null ? transacao.usuario.id : null,
            transacao.valor,
            transacao.moeda,
            transacao.mcc,
            transacao.categoria,
            transacao.parceiroId,
            transacao.status != null ? transacao.status.name() : null,
            transacao.autorizacao,
            transacao.dataEvento,
            transacao.processadoEm,
            transacao.pontosGerados
        );
    }
}

