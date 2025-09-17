package org.acme.loyalty.dto;

import jakarta.validation.constraints.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ResgateRequest", description = "Dados para solicitar um resgate de recompensa")
public class ResgateRequestDTO {
    
    @NotNull(message = "ID do usuário é obrigatório")
    @Schema(description = "ID do usuário", example = "1", required = true)
    public Long usuarioId;
    
    @NotNull(message = "ID do cartão é obrigatório")
    @Schema(description = "ID do cartão", example = "1", required = true)
    public Long cartaoId;
    
    @NotNull(message = "ID da recompensa é obrigatório")
    @Schema(description = "ID da recompensa", example = "1", required = true)
    public Long recompensaId;
    
    @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres")
    @Schema(description = "Observação do resgate", example = "Resgate solicitado pelo cliente")
    public String observacao;
    
    // Construtores
    public ResgateRequestDTO() {}
    
    public ResgateRequestDTO(Long usuarioId, Long cartaoId, Long recompensaId, String observacao) {
        this.usuarioId = usuarioId;
        this.cartaoId = cartaoId;
        this.recompensaId = recompensaId;
        this.observacao = observacao;
    }
}

