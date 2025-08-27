package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.acme.loyalty.entity.RegraConversao;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Atualização parcial (PATCH) para {@link RegraConversao}.
 * Todos os campos são opcionais; apenas os não nulos serão aplicados.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RegraConversaoUpdate", description = "Campos opcionais para atualizar uma regra de conversão de pontos")
public class RegraConversaoUpdateDTO {

    // ===== Campos atualizáveis (todos opcionais) =====

    @Size(max = 100)
    @Schema(description = "Novo nome da regra", example = "1 ponto por BRL em Restaurantes")
    public String nome;

    @Positive
    @Digits(integer = 4, fraction = 4) // numeric(8,4)
    @Schema(description = "Novo multiplicador de pontos (numeric(8,4))", example = "1.2500")
    public BigDecimal multiplicador;

    @Size(max = 100)
    @Schema(description = "Novo regex de MCC (vazio = qualquer MCC)", example = "^(5(8(1[0-9]|2[0-9])|9[0-9]))$")
    public String mccRegex;

    @Size(max = 100)
    @Schema(description = "Nova categoria (vazio = qualquer categoria)", example = "RESTAURANTE")
    public String categoria;

    @Schema(description = "Novo identificador de parceiro (null = qualquer)", example = "12345")
    public Long parceiroId;

    @Min(0)
    @Schema(description = "Nova prioridade (maior vence em empate)", example = "10")
    public Integer prioridade;

    @Positive
    @Schema(description = "Novo teto mensal de pontos (null = ilimitado)", example = "50000")
    public Long tetoMensal;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Novo início de vigência", example = "2025-09-01T00:00:00")
    public LocalDateTime vigenciaIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Novo fim de vigência (opcional)", example = "2025-12-31T23:59:59")
    public LocalDateTime vigenciaFim;

    @Schema(description = "Ativar/Desativar a regra", example = "true")
    public Boolean ativo;

    // ===== Validações de regra (apenas do payload) =====

    @AssertTrue(message = "Se fornecidos, vigenciaFim deve ser maior que vigenciaIni")
    public boolean isPeriodoValidoQuandoAmbosFornecidos() {
        if (vigenciaIni == null || vigenciaFim == null) return true;
        return vigenciaFim.isAfter(vigenciaIni);
    }

    @AssertTrue(message = "mccRegex inválido (não compila)")
    public boolean isRegexValido() {
        if (mccRegex == null || mccRegex.isBlank()) return true;
        try {
            Pattern.compile(mccRegex);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    // ===== Conveniências =====

    /**
     * Aplica os campos não nulos desta DTO sobre a entidade alvo.
     * Também valida o período resultante (vigenciaIni/vigenciaFim) após aplicar.
     *
     * @throws IllegalArgumentException se o período final ficar inválido.
     */
    public void applyToEntity(RegraConversao r) {
        if (r == null) return;

        if (this.nome != null)            r.nome = this.nome;
        if (this.multiplicador != null)   r.multiplicador = this.multiplicador;
        if (this.mccRegex != null)        r.mccRegex = this.mccRegex;
        if (this.categoria != null)       r.categoria = this.categoria;
        if (this.parceiroId != null)      r.parceiroId = this.parceiroId;
        if (this.prioridade != null)      r.prioridade = this.prioridade;
        if (this.tetoMensal != null)      r.tetoMensal = this.tetoMensal;
        if (this.vigenciaIni != null)     r.vigenciaIni = this.vigenciaIni;
        if (this.vigenciaFim != null)     r.vigenciaFim = this.vigenciaFim;
        if (this.ativo != null)           r.ativo = this.ativo;

        // valida período final
        if (r.vigenciaFim != null && !r.vigenciaFim.isAfter(r.vigenciaIni)) {
            throw new IllegalArgumentException("vigenciaFim deve ser maior que vigenciaIni");
        }

        r.atualizadoEm = LocalDateTime.now();
    }

    /** Retorna true se ao menos um campo atualizável foi fornecido. */
    @Schema(hidden = true)
    public boolean hasAnyField() {
        return nome != null
            || multiplicador != null
            || mccRegex != null
            || categoria != null
            || parceiroId != null
            || prioridade != null
            || tetoMensal != null
            || vigenciaIni != null
            || vigenciaFim != null
            || ativo != null;
    }
}
