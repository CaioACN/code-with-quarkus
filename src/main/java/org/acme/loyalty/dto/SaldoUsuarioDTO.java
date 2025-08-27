package org.acme.loyalty.dto;

import java.util.List;

public class SaldoUsuarioDTO {
    
    public Long usuarioId;
    public List<SaldoPontosDTO> saldos;
    public Long saldoTotal;
    public Long totalPontosExpirando;
    
    // Construtores
    public SaldoUsuarioDTO() {}
    
    public SaldoUsuarioDTO(Long usuarioId, List<SaldoPontosDTO> saldos) {
        this.usuarioId = usuarioId;
        this.saldos = saldos;
        this.saldoTotal = calcularSaldoTotal();
        this.totalPontosExpirando = calcularTotalPontosExpirando();
    }
    
    // Métodos de negócio
    private Long calcularSaldoTotal() {
        if (saldos == null) return 0L;
        return saldos.stream()
                .mapToLong(saldo -> saldo.saldo != null ? saldo.saldo : 0L)
                .sum();
    }
    
    private Long calcularTotalPontosExpirando() {
        if (saldos == null) return 0L;
        return saldos.stream()
                .mapToLong(saldo -> {
                    Long pontos30 = saldo.pontosExpirando30Dias != null ? saldo.pontosExpirando30Dias : 0L;
                    Long pontos60 = saldo.pontosExpirando60Dias != null ? saldo.pontosExpirando60Dias : 0L;
                    Long pontos90 = saldo.pontosExpirando90Dias != null ? saldo.pontosExpirando90Dias : 0L;
                    return pontos30 + pontos60 + pontos90;
                })
                .sum();
    }
}

