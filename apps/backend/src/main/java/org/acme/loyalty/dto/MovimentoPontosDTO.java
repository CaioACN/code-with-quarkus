package org.acme.loyalty.dto;

import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.entity.MovimentoPontos.TipoMovimento;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "MovimentoPontos", description = "DTO para movimentação de pontos")
public class MovimentoPontosDTO {
    
    @Schema(description = "ID do movimento", example = "1")
    public Long id;
    
    @Schema(description = "ID do usuário", example = "1")
    public Long usuarioId;
    
    @Schema(description = "ID do cartão", example = "1")
    public Long cartaoId;
    
    @Schema(description = "Tipo do movimento", example = "ACUMULO", 
            enumeration = {"ACUMULO","EXPIRACAO","RESGATE","ESTORNO","AJUSTE"})
    public TipoMovimento tipo;
    
    @Schema(description = "Quantidade de pontos", example = "100")
    public Integer pontos;
    
    @Schema(description = "ID da transação de referência", example = "1")
    public Long refTransacaoId;
    
    @Schema(description = "Observação do movimento", example = "Pontos acumulados por compra")
    public String observacao;
    
    @Schema(description = "Data de criação do movimento", example = "2025-09-09T10:00:00")
    public LocalDateTime criadoEm;
    
    @Schema(description = "ID do job que processou o movimento", example = "job-123")
    public String jobId;
    
    @Schema(description = "Regra aplicada no movimento", example = "Regra Restaurantes")
    public String regraAplicada;
    
    @Schema(description = "Campanha aplicada no movimento", example = "Campanha Setembro")
    public String campanhaAplicada;
    
    @Schema(description = "Descrição amigável do tipo", example = "Acúmulo de pontos")
    public String descricaoTipo;
    
    // Construtores
    public MovimentoPontosDTO() {}
    
    public MovimentoPontosDTO(Long id, Long usuarioId, Long cartaoId, TipoMovimento tipo, 
                              Integer pontos, Long refTransacaoId, String observacao, 
                              LocalDateTime criadoEm, String jobId, String regraAplicada, 
                              String campanhaAplicada) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.tipo = tipo;
        this.pontos = pontos;
        this.refTransacaoId = refTransacaoId;
        this.observacao = observacao;
        this.criadoEm = criadoEm;
        this.jobId = jobId;
        this.regraAplicada = regraAplicada;
        this.campanhaAplicada = campanhaAplicada;
        this.descricaoTipo = gerarDescricaoTipo();
    }
    
    // Métodos de negócio
    private String gerarDescricaoTipo() {
        if (tipo == null) return "Movimento de pontos";
        
        if (tipo == TipoMovimento.ACUMULO) {
            return "Acúmulo de pontos";
        } else if (tipo == TipoMovimento.EXPIRACAO) {
            return "Expiração de pontos";
        } else if (tipo == TipoMovimento.RESGATE) {
            return "Resgate de pontos";
        } else if (tipo == TipoMovimento.ESTORNO) {
            return "Estorno de pontos";
        } else if (tipo == TipoMovimento.AJUSTE) {
            return "Ajuste de pontos";
        } else {
            return "Movimento de pontos";
        }
    }
    
    // Método estático para criar DTO a partir da entidade
    public static MovimentoPontosDTO fromEntity(MovimentoPontos entity) {
        return new MovimentoPontosDTO(
            entity.id,
            entity.usuario.id,
            entity.cartao.id,
            entity.tipo,
            entity.pontos,
            entity.refTransacaoId,
            entity.observacao,
            entity.criadoEm,
            entity.jobId,
            entity.regraAplicada,
            entity.campanhaAplicada
        );
    }
}

