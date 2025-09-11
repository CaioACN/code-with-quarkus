package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Schema(
        description = "Tipo da recompensa",
        required = true,
        example = "PRODUTO",
        enumeration = {
            "MILHAS","GIFT","CASHBACK","PRODUTO"
        }
    )
    @JsonProperty("tipo")
    public String tipo;

    @Schema(description = "Descrição/título da recompensa", example = "Fone Bluetooth XYZ")
    public String descricao;

    @Schema(description = "Custo em pontos para resgatar esta recompensa", example = "2500")
    public Long custoPontos;

    @Schema(description = "Estoque inicial disponível", example = "100")
    public Long estoque;

    // ===== Campos opcionais =====

    @Schema(description = "Identificador do parceiro (se houver integração de fulfillment)", example = "12345")
    public Long parceiroId;

    @Schema(description = "Detalhes adicionais/observações da recompensa")
    public String detalhes;

    @Schema(description = "URL da imagem ilustrativa", example = "https://cdn.exemplo.com/imgs/reward-123.png")
    public String imagemUrl;

    @Schema(description = "Validade da recompensa (após essa data, fica indisponível)", example = "2025-12-31")
    public String validadeRecompensa;

    @Schema(description = "Ativa no catálogo?", example = "true", defaultValue = "true")
    public Boolean ativo = Boolean.TRUE;

    // ===== Conveniências =====

    /** Garante valores padrão sem sobrescrever o que já foi setado. */
    public void ensureDefaults() {
        if (ativo == null) ativo = Boolean.TRUE;
    }

    /** Converte este request para uma entidade {@link Recompensa}. */
    public Recompensa toEntity() {
        ensureDefaults();
        Recompensa.TipoRecompensa tipoEnum = Recompensa.TipoRecompensa.PRODUTO;
        if (this.tipo != null && !this.tipo.trim().isEmpty()) {
            try {
                tipoEnum = Recompensa.TipoRecompensa.valueOf(this.tipo.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Usar valor padrão se inválido
            }
        }
        Recompensa r = new Recompensa(
                tipoEnum,
                this.descricao,
                this.custoPontos,
                this.estoque,
                this.parceiroId,
                this.detalhes
        );
        r.imagemUrl = this.imagemUrl;
        
        // Converter string para LocalDateTime se não for null
        if (this.validadeRecompensa != null && !this.validadeRecompensa.trim().isEmpty()) {
            try {
                r.validadeRecompensa = LocalDateTime.parse(this.validadeRecompensa + "T23:59:59");
            } catch (Exception e) {
                // Se falhar, usar data padrão (1 ano no futuro)
                r.validadeRecompensa = LocalDateTime.now().plusYears(1);
            }
        } else {
            // Data padrão: 1 ano no futuro
            r.validadeRecompensa = LocalDateTime.now().plusYears(1);
        }
        
        r.ativo = this.ativo;
        return r;
    }
}
