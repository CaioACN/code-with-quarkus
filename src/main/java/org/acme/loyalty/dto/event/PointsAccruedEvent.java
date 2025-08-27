package org.acme.loyalty.dto.event;

import java.time.LocalDateTime;

public class PointsAccruedEvent {
    
    public Long usuarioId;
    public Long cartaoId;
    public Integer pontos;
    public Long refTransacaoId;
    public LocalDateTime criadoEm;
    public LocalDateTime timestamp;
    
    // Construtores
    public PointsAccruedEvent() {}
    
    public PointsAccruedEvent(Long usuarioId, Long cartaoId, Integer pontos, 
                              Long refTransacaoId, LocalDateTime criadoEm) {
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.pontos = pontos;
        this.refTransacaoId = refTransacaoId;
        this.criadoEm = criadoEm;
        this.timestamp = LocalDateTime.now();
    }
    
    // Métodos de negócio
    public String getEventKey() {
        return "pontos.acumulo." + usuarioId + "." + cartaoId + "." + refTransacaoId;
    }
    
    public String getTopic() {
        return "loyalty.points";
    }
    
    public String getEventType() {
        return "PointsAccrued";
    }
    
    public Boolean isPositive() {
        return pontos != null && pontos > 0;
    }
}

