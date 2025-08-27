package org.acme.loyalty.dto.event;

import java.time.LocalDateTime;

public class PointsExpiredEvent {
    
    public Long usuarioId;
    public Long cartaoId;
    public Integer pontos;
    public String jobId;
    public LocalDateTime criadoEm;
    public LocalDateTime timestamp;
    
    // Construtores
    public PointsExpiredEvent() {}
    
    public PointsExpiredEvent(Long usuarioId, Long cartaoId, Integer pontos, 
                              String jobId, LocalDateTime criadoEm) {
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.pontos = pontos;
        this.jobId = jobId;
        this.criadoEm = criadoEm;
        this.timestamp = LocalDateTime.now();
    }
    
    // Métodos de negócio
    public String getEventKey() {
        return "pontos.expiracao." + usuarioId + "." + cartaoId + "." + jobId;
    }
    
    public String getTopic() {
        return "loyalty.points";
    }
    
    public String getEventType() {
        return "PointsExpired";
    }
    
    public Boolean isNegative() {
        return pontos != null && pontos < 0;
    }
    
    public Integer getPontosAbsolutos() {
        return pontos != null ? Math.abs(pontos) : 0;
    }
}

