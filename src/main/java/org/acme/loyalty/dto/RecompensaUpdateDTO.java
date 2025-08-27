package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.acme.loyalty.entity.Recompensa;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Atualização parcial (PATCH) de uma {@link Recompensa}.
 * Todos os campos são opcionais; somente os não nulos serão aplicados.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RecompensaUpdate", description = "Campos opcionais para atualizar uma recompensa do catálogo")
public class RecompensaUpdateDTO {

    // ===== Campos atualizáveis (todos opcionais) =====

    @Schema(
        description = "Novo tipo da recompensa (opcional)",
        enumeration = {"PRODUTO_FISICO","PRODUTO_DIGITAL","DESCONTO","CASHBACK","MILHAS","EXPERIENCIA","OUTRO"}
    )
    public Recompensa.TipoRecompensa tipo;

    @Size(max = 200)
    @Schema(description = "Nova descrição/título (opcional)")
    public String descricao;

    @Positive
    @Schema(description = "Novo custo em pontos (opcional)", example = "3000")
    public Long custoPontos;

    @PositiveOrZero
    @Schema(description = "Ajuste de estoque absoluto (opcional). Valor será definido exatamente para este número.", example = "150")
    public Long estoque;

    @Schema(description = "Novo identificador de parceiro (opcional)", example = "987")
    public Long parceiroId;

    @Size(max = 500)
    @Schema(description = "Novos detalhes/observações (opcional)")
    public String detalhes;

    @Pattern(regexp = "^(https?://).*$", message = "imagemUrl deve iniciar com http:// ou https://")
    @Schema(description = "Nova URL da imagem (opcional)", example = "https://cdn.exemplo.com/imgs/reward-123.png")
    public String imagemUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Nova data de validade (opcional)", example = "2026-01-31T23:59:59")
    public LocalDateTime validadeRecompensa;

    @Schema(description = "Ativar/Desativar recompensa (opcional)", example = "true")
    public Boolean ativo;

    // ===== Regras simples =====

    @AssertTrue(message = "validadeRecompensa (se informada) deve ser no futuro")
    public boolean isValidadeFuturaIfProvided() {
        if (validadeRecompensa == null) return true;
        return validadeRecompensa.isAfter(LocalDateTime.now());
    }

    // ===== Conveniências =====

    /**
     * Aplica os campos não nulos desta DTO sobre a entidade alvo.
     * Não persiste nem faz flush; apenas muta o objeto.
     */
    public void applyToEntity(Recompensa r) {
        if (r == null) return;
        if (this.tipo != null)                r.tipo = this.tipo;
        if (this.descricao != null)           r.descricao = this.descricao;
        if (this.custoPontos != null)         r.custoPontos = this.custoPontos;
        if (this.estoque != null)             r.estoque = this.estoque;
        if (this.parceiroId != null)          r.parceiroId = this.parceiroId;
        if (this.detalhes != null)            r.detalhes = this.detalhes;
        if (this.imagemUrl != null)           r.imagemUrl = this.imagemUrl;
        if (this.validadeRecompensa != null)  r.validadeRecompensa = this.validadeRecompensa;
        if (this.ativo != null)               r.ativo = this.ativo;

        r.atualizadoEm = LocalDateTime.now();
    }

    /**
     * Retorna true se ao menos um campo atualizável foi fornecido.
     * Útil para validação de PATCH sem payload efetivo.
     */
    @Schema(hidden = true)
    public boolean hasAnyField() {
        return tipo != null
            || descricao != null
            || custoPontos != null
            || estoque != null
            || parceiroId != null
            || detalhes != null
            || imagemUrl != null
            || validadeRecompensa != null
            || ativo != null;
    }
}
