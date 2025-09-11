package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.acme.loyalty.entity.SaldoPontos;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "SaldoPontos", description = "DTO para saldo de pontos de um cartão")
public class SaldoPontosDTO {
    
    @Schema(description = "ID do cartão", example = "1")
    public Long cartaoId;
    
    @Schema(description = "Saldo atual de pontos", example = "1500")
    public Long saldo;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data da última atualização do saldo", example = "2025-09-09T10:00:00")
    public LocalDateTime atualizadoEm;
    
    @Schema(description = "Pontos expirando em 30 dias", example = "100")
    public Long pontosExpirando30Dias;
    
    @Schema(description = "Pontos expirando em 60 dias", example = "200")
    public Long pontosExpirando60Dias;
    
    @Schema(description = "Pontos expirando em 90 dias", example = "300")
    public Long pontosExpirando90Dias;
    
    @Schema(description = "Status do saldo", example = "ATIVO")
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
    
    // Métodos de negócio
    public Long getTotalPontosExpirando() {
        Long pontos30 = pontosExpirando30Dias != null ? pontosExpirando30Dias : 0L;
        Long pontos60 = pontosExpirando60Dias != null ? pontosExpirando60Dias : 0L;
        Long pontos90 = pontosExpirando90Dias != null ? pontosExpirando90Dias : 0L;
        return pontos30 + pontos60 + pontos90;
    }
    
    public boolean temPontosExpirando() {
        return getTotalPontosExpirando() > 0;
    }
    
    public String getStatusSaldo() {
        if (saldo == null || saldo <= 0) return "SEM_SALDO";
        if (temPontosExpirando()) return "COM_EXPIRACAO";
        return "ATIVO";
    }
    
    // Método estático para criar DTO a partir da entidade
    public static SaldoPontosDTO fromEntity(SaldoPontos entity) {
        return new SaldoPontosDTO(
            entity.cartao.id,
            entity.saldo,
            entity.atualizadoEm,
            entity.pontosExpirando30Dias,
            entity.pontosExpirando60Dias,
            entity.pontosExpirando90Dias,
            null // será calculado pelo getStatusSaldo()
        );
    }
}

