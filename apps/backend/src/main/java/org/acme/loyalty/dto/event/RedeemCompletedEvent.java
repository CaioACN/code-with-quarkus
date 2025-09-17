package org.acme.loyalty.dto.event;

import java.time.LocalDateTime;

public class RedeemCompletedEvent {
    
    public Long resgateId;
    public Long usuarioId;
    public Long pontos;
    public String status;
    public LocalDateTime timestamp;
    
    // Construtores
    public RedeemCompletedEvent() {}
    
    public RedeemCompletedEvent(Long resgateId, Long usuarioId, Long pontos, String status) {
        this.resgateId = resgateId;
        this.usuarioId = usuarioId;
        this.pontos = pontos;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
    
    // Construtor para status COMPLETED
    public static RedeemCompletedEvent completed(Long resgateId, Long usuarioId, Long pontos) {
        return new RedeemCompletedEvent(resgateId, usuarioId, pontos, "COMPLETED");
    }
    
    // Construtor para status NEGADO
    public static RedeemCompletedEvent negado(Long resgateId, Long usuarioId, Long pontos) {
        return new RedeemCompletedEvent(resgateId, usuarioId, pontos, "NEGADO");
    }
    
    // Métodos de negócio
    public String getEventKey() {
        return "resgate." + resgateId;
    }
    
    public String getTopic() {
        return "loyalty.redeems";
    }
    
    public String getEventType() {
        return "RedeemCompleted";
    }
    
    public Boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
    
    public Boolean isNegado() {
        return "NEGADO".equals(status);
    }
    
    public String getStatusDescricao() {
        if ("COMPLETED".equals(status)) {
            return "Resgate Concluído";
        } else if ("NEGADO".equals(status)) {
            return "Resgate Negado";
        } else {
            return "Status: " + status;
        }
    }
}

