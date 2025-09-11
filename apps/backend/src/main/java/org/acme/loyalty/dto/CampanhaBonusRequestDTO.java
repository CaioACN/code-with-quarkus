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

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
    @Schema(description = "Nome da campanha de bônus", example = "Bônus Restaurantes Setembro")
    public String nome;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    @Schema(description = "Descrição da campanha de bônus", example = "Campanha especial para restaurantes")
    public String descricao;

    @DecimalMin(value = "0.0000", inclusive = true, message = "Multiplicador extra deve ser maior ou igual a 0.0000")
    @DecimalMax(value = "9999.9999", inclusive = true, message = "Multiplicador extra deve ser menor ou igual a 9999.9999")
    @Digits(integer = 4, fraction = 4, message = "Multiplicador extra deve ter no máximo 4 dígitos inteiros e 4 decimais")
    @Schema(description = "Multiplicador extra aplicado aos pontos (0.0000 a 9999.9999)", example = "0.2000", defaultValue = "0.0000")
    public BigDecimal multiplicadorExtra;

    @NotNull(message = "Data de início da vigência é obrigatória")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de início da vigência", example = "2025-09-01")
    public LocalDate vigenciaIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de término da vigência (opcional)", example = "2025-09-30")
    public LocalDate vigenciaFim;

    @Size(max = 60, message = "Segmento deve ter no máximo 60 caracteres")
    @Schema(description = "Segmento ao qual a campanha se aplica (opcional)", example = "RESTAURANTES")
    public String segmento;

    @Min(value = 0, message = "Prioridade deve ser maior ou igual a 0")
    @Schema(description = "Prioridade da campanha (maior valor = mais precedência)", example = "10", defaultValue = "0")
    public Integer prioridade;

    @AssertTrue(message = "Teto deve ser nulo ou maior que zero")
    public boolean isTetoValido() {
        return teto == null || teto > 0;
    }
    
    @Schema(description = "Teto de pontos extras permitido (opcional)", example = "100000")
    public Long teto;

    public CampanhaBonusRequestDTO() {}

    // ---- Validação de período ----
    @AssertTrue(message = "Data de fim deve ser posterior ou igual à data de início")
    public boolean isPeriodoValido() {
        if (vigenciaFim == null || vigenciaIni == null) return true;
        return !vigenciaFim.isBefore(vigenciaIni);
    }

    // ---- Mapeamentos ----
    public CampanhaBonus toEntity() {
        // Aplicar valores padrão conforme DDL
        BigDecimal mult = (multiplicadorExtra != null) ? multiplicadorExtra : BigDecimal.ZERO;
        Integer prio = (prioridade != null) ? prioridade : 0;
        
        CampanhaBonus entity = new CampanhaBonus(
            nome,
            mult,
            vigenciaIni,
            vigenciaFim,
            segmento,
            prio,
            teto
        );
        
        // Armazenar descrição em um campo auxiliar (já que a entidade não tem descrição)
        // Vamos usar o campo segmento para armazenar a descrição temporariamente
        if (descricao != null && !descricao.trim().isEmpty()) {
            entity.segmento = descricao.trim();
        }
        
        return entity;
    }

    public void applyTo(CampanhaBonus entity) {
        if (nome != null) entity.nome = nome.trim();
        if (multiplicadorExtra != null) entity.multiplicadorExtra = multiplicadorExtra;
        if (vigenciaIni != null) entity.vigenciaIni = vigenciaIni;
        entity.vigenciaFim = vigenciaFim; // pode setar null
        if (segmento != null) entity.segmento = segmento.trim();
        if (prioridade != null) entity.prioridade = prioridade;
        entity.teto = teto;
        
        // Armazenar descrição no campo segmento se fornecida
        if (descricao != null && !descricao.trim().isEmpty()) {
            entity.segmento = descricao.trim();
        }
        
        // Aplicar valores padrão se não foram fornecidos
        if (entity.multiplicadorExtra == null) entity.multiplicadorExtra = BigDecimal.ZERO;
        if (entity.prioridade == null) entity.prioridade = 0;
    }

}
