package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.acme.loyalty.entity.CampanhaBonus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "CampanhaBonusUpdate", description = "Payload para atualização parcial de uma campanha de bônus")
public class CampanhaBonusUpdateDTO {

    @Size(max = 120)
    @Schema(description = "Nome da campanha de bônus", example = "Bônus Restaurantes Outubro")
    public String nome;

    @DecimalMin("0.0000")
    @Digits(integer = 4, fraction = 4)
    @Schema(description = "Multiplicador extra aplicado aos pontos", example = "0.1500")
    public BigDecimal multiplicadorExtra;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de início da vigência", example = "2025-10-01")
    public LocalDate vigenciaIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de término da vigência", example = "2025-10-31")
    public LocalDate vigenciaFim;

    @Size(max = 60)
    @Schema(description = "Segmento ao qual a campanha se aplica", example = "SUPERMERCADOS")
    public String segmento;

    @Min(0)
    @Schema(description = "Prioridade da campanha (maior valor = maior precedência)", example = "5")
    public Integer prioridade;

    @Schema(description = "Teto de pontos extras permitido", example = "50000")
    public Long teto;

    public CampanhaBonusUpdateDTO() {}

    // --- Aplicação dos valores recebidos sobre a entidade existente ---
    public void applyTo(CampanhaBonus entity) {
        if (nome != null) entity.nome = nome;
        if (multiplicadorExtra != null) entity.multiplicadorExtra = multiplicadorExtra;
        if (vigenciaIni != null) entity.vigenciaIni = vigenciaIni;
        // se null, não atualiza (mantém o valor atual)
        if (vigenciaFim != null || (vigenciaFim == null && thisHasField("vigenciaFim")))
            entity.vigenciaFim = vigenciaFim;
        if (segmento != null) entity.segmento = segmento;
        if (prioridade != null) entity.prioridade = prioridade;
        if (teto != null || (teto == null && thisHasField("teto")))
            entity.teto = teto;
    }

    /**
     * Método auxiliar: em alguns frameworks, precisamos diferenciar entre
     * "campo não enviado" e "campo enviado como null". 
     * Se quiser tratar isso, dá pra usar reflection ou uma lib de mapeamento (ex.: MapStruct).
     * Aqui deixei o stub para você expandir depois.
     */
    private boolean thisHasField(String fieldName) {
        // Pode ser implementado com reflection ou frameworks JSON (Jackson)
        return true; // por enquanto assume que se está null foi enviado como null
    }
}
