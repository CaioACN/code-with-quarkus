package org.acme.loyalty.dto;

import java.time.LocalDateTime;

public class SaldoPontosDTO {
    
    public Long cartaoId;
    public Long saldo;
    public LocalDateTime atualizadoEm;
    public Long pontosExpirando30Dias;
    public Long pontosExpirando60Dias;
    public Long pontosExpirando90Dias;
    public String statusSaldo;
    
    // Construtores
    public SaldoPontosDTO() {}
    
    public SaldoPontosDTO(Long cartaoId, Long saldo, LocalDateTime atualizadoEm) {
        this.cartaoId = cartaoId;
        this.saldo = saldo;
        this.atualizadoEm = atualizadoEm;
    }
    
    public SaldoPontosDTO(Long cartaoId, Long saldo, LocalDateTime atualizadoEm,
                          Long pontosExpirando30Dias, Long pontosExpirando60Dias, 
                          Long pontosExpirando90Dias, String statusSaldo) {
        this.cartaoId = cartaoId;
        this.saldo = saldo;
        this.atualizadoEm = atualizadoEm;
        this.pontosExpirando30Dias = pontosExpirando30Dias;
        this.pontosExpirando60Dias = pontosExpirando60Dias;
        this.pontosExpirando90Dias = pontosExpirando90Dias;
        this.statusSaldo = statusSaldo;
    }
}

