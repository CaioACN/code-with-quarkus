package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

import org.acme.loyalty.entity.CampanhaBonus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CampanhaBonusRequest", description = "Payload para criação ou atualização de campanhas de bônus")
public class CampanhaBonusRequestDTO {

    @NotBlank
    @Size(max = 120)
    @Schema(description = "Nome da campanha de bônus", example = "Bônus Restaurantes Setembro")
    public String nome;

    @NotNull
    @DecimalMin("0.0000")
    @Digits(integer = 4, fraction = 4)
    @Schema(description = "Multiplicador extra aplicado aos pontos", example = "0.2000")
    public BigDecimal multiplicadorExtra;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de início da vigência", example = "2025-09-01")
    public LocalDate vigenciaIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de término da vigência (opcional)", example = "2025-09-30")
    public LocalDate vigenciaFim;

    @Size(max = 60)
    @Schema(description = "Segmento ao qual a campanha se aplica (opcional)", example = "RESTAURANTES")
    public String segmento;

    @NotNull
    @Min(0)
    @Schema(description = "Prioridade da campanha (maior valor tem mais precedência)", example = "10", defaultValue = "0")
    public Integer prioridade;

    @Positive
    @Schema(description = "Teto de pontos extras permitido (opcional)", example = "100000")
    public Long teto;

    public CampanhaBonusRequestDTO() {}

    // ---- Validação de regra de negócio ----
    @AssertTrue(message = "vigenciaFim deve ser >= vigenciaIni")
    public boolean isPeriodoValido() {
        if (vigenciaFim == null || vigenciaIni == null) return true;
        return !vigenciaFim.isBefore(vigenciaIni);
    }

    // ---- Mapeamentos ----
    public CampanhaBonus toEntity() {
        return new CampanhaBonus(
            nome,
            (multiplicadorExtra != null ? multiplicadorExtra : BigDecimal.ZERO),
            vigenciaIni,
            vigenciaFim,
            segmento,
            (prioridade != null ? prioridade : 0),
            teto
        );
    }

    public void applyTo(CampanhaBonus entity) {
        if (nome != null) entity.nome = nome;
        if (multiplicadorExtra != null) entity.multiplicadorExtra = multiplicadorExtra;
        if (vigenciaIni != null) entity.vigenciaIni = vigenciaIni;
        entity.vigenciaFim = vigenciaFim; // pode ser null
        if (segmento != null) entity.segmento = segmento;
        if (prioridade != null) entity.prioridade = prioridade;
        entity.teto = teto;
    }
}
