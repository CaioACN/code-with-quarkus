package org.acme.loyalty.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "SaldoUsuario", description = "DTO para saldo de pontos de um usuário")
public class SaldoUsuarioDTO {
    
    @Schema(description = "ID do usuário", example = "1")
    public Long usuarioId;
    
    @Schema(description = "Lista de saldos por cartão")
    public List<SaldoPontosDTO> saldos;
    
    @Schema(description = "Saldo total de pontos do usuário", example = "5000")
    public Long saldoTotal;
    
    @Schema(description = "Total de pontos expirando", example = "500")
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

