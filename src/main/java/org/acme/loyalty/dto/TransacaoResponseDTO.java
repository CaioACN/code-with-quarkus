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
                               String status, LocalDateTime dataEvento, LocalDateTime processadoEm, 
                               Integer pontosGerados) {
        this.id = id;
        this.cartaoId = cartaoId;
        this.usuarioId = usuarioId;
        this.valor = valor;
        this.moeda = moeda;
        this.mcc = mcc;
        this.categoria = categoria;
        this.parceiroId = parceiroId;
        this.status = status;
        this.dataEvento = dataEvento;
        this.processadoEm = processadoEm;
        this.pontosGerados = pontosGerados;
    }
}

