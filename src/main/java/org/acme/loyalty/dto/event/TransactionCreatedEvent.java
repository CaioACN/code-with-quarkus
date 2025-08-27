package org.acme.loyalty.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionCreatedEvent {
    
    public Long transacaoId;
    public Long usuarioId;
    public Long cartaoId;
    public BigDecimal valor;
    public String moeda;
    public String mcc;
    public String categoria;
    public LocalDateTime dataEvento;
    public LocalDateTime timestamp;
    
    // Construtores
    public TransactionCreatedEvent() {}
    
    public TransactionCreatedEvent(Long transacaoId, Long usuarioId, Long cartaoId, 
                                  BigDecimal valor, String moeda, String mcc, 
                                  String categoria, LocalDateTime dataEvento) {
        this.transacaoId = transacaoId;
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.valor = valor;
        this.moeda = moeda;
        this.mcc = mcc;
        this.categoria = categoria;
        this.dataEvento = dataEvento;
        this.timestamp = LocalDateTime.now();
    }
    
    // Métodos de negócio
    public String getEventKey() {
        return "transacao." + transacaoId;
    }
    
    public String getTopic() {
        return "loyalty.transactions";
    }
    
    public String getEventType() {
        return "TransactionCreated";
    }
}

