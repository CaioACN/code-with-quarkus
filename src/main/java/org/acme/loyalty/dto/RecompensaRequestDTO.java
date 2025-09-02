package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.acme.loyalty.entity.Recompensa;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Pedido de criação de Recompensa (catálogo) no programa de pontos.
 * Compatível com Quarkus 3 / Jakarta Validation e IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RecompensaRequest", description = "Dados para cadastrar uma nova recompensa no catálogo")
public class RecompensaRequestDTO {

    // ===== Campos obrigatórios =====

    @NotNull
    @Schema(
        description = "Tipo da recompensa",
        required = true,
        example = "PRODUTO",
        enumeration = {
            "MILHAS","GIFT","CASHBACK","PRODUTO"
        }
    )
    public Recompensa.TipoRecompensa tipo;

    @NotBlank
    @Size(max = 200)
    @Schema(description = "Descrição/título da recompensa", example = "Fone Bluetooth XYZ")
    public String descricao;

    @NotNull
    @Positive
    @Schema(description = "Custo em pontos para resgatar esta recompensa", example = "2500")
    public Long custoPontos;

    @NotNull
    @PositiveOrZero
    @Schema(description = "Estoque inicial disponível", example = "100")
    public Long estoque;

    // ===== Campos opcionais =====

    @Schema(description = "Identificador do parceiro (se houver integração de fulfillment)", example = "12345")
    public Long parceiroId;

    @Size(max = 500)
    @Schema(description = "Detalhes adicionais/observações da recompensa")
    public String detalhes;

    @Pattern(regexp = "^(https?://).*$", message = "imagemUrl deve iniciar com http:// ou https://")
    @Schema(description = "URL da imagem ilustrativa", example = "https://cdn.exemplo.com/imgs/reward-123.png")
    public String imagemUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Validade da recompensa (após essa data, fica indisponível)", example = "2025-12-31T23:59:59")
    public LocalDateTime validadeRecompensa;

    @Schema(description = "Ativa no catálogo?", example = "true", defaultValue = "true")
    public Boolean ativo = Boolean.TRUE;

    // ===== Regras simples de validação =====

    @AssertTrue(message = "validadeRecompensa deve ser no futuro (ou vazio)")
    public boolean isValidadeFutura() {
        if (validadeRecompensa == null) return true;
        return validadeRecompensa.isAfter(LocalDateTime.now());
    }

    // ===== Conveniências =====

    /** Garante valores padrão sem sobrescrever o que já foi setado. */
    public void ensureDefaults() {
        if (ativo == null) ativo = Boolean.TRUE;
    }

    /** Converte este request para uma entidade {@link Recompensa}. */
    public Recompensa toEntity() {
        ensureDefaults();
        Recompensa r = new Recompensa(
                this.tipo,
                this.descricao,
                this.custoPontos,
                this.estoque,
                this.parceiroId,
                this.detalhes
        );
        r.imagemUrl = this.imagemUrl;
        r.validadeRecompensa = this.validadeRecompensa;
        r.ativo = this.ativo;
        return r;
    }
}
