package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.acme.loyalty.entity.RegraConversao;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * DTO de criação para {@link RegraConversao}.
 * Compatível com Quarkus 3 / Jakarta Validation e IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RegraConversaoRequest", description = "Parâmetros para cadastrar uma nova regra de conversão de pontos")
public class RegraConversaoRequestDTO {

    // ===== Campos obrigatórios =====

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Nome da regra", example = "1 ponto por BRL em Restaurantes")
    public String nome;

    @NotNull
    @Positive                       // > 0 (ex.: 1.0000 => 1 ponto por unidade monetária)
    @Digits(integer = 4, fraction = 4) // numeric(8,4)
    @Schema(description = "Multiplicador de pontos (numeric(8,4))", example = "1.0000")
    public BigDecimal multiplicador;

    @NotNull
    @Min(0)
    @Schema(description = "Prioridade da regra (maior vence em empates)", example = "10")
    public Integer prioridade;

    @NotNull
    @Schema(description = "Data/hora de início da vigência", example = "2025-08-01T00:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime vigenciaIni;

    // ===== Campos opcionais =====

    @Size(max = 100)
    @Schema(description = "Regex de MCC (opcional). Se vazio, vale para qualquer MCC", example = "^(5(8(1[0-9]|2[0-9])|9[0-9]))$")
    public String mccRegex;

    @Size(max = 100)
    @Schema(description = "Categoria da transação (opcional). Se vazio, vale para qualquer categoria", example = "RESTAURANTE")
    public String categoria;

    @Schema(description = "Identificador do parceiro (opcional). Se vazio, vale para qualquer parceiro", example = "12345")
    public Long parceiroId;

    @Schema(description = "Teto mensal de pontos acumuláveis pela regra (opcional). Se ausente, ilimitado", example = "50000")
    @Positive
    public Long tetoMensal;

    @Schema(description = "Data/hora de fim da vigência (opcional)", example = "2025-12-31T23:59:59")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime vigenciaFim;

    @Schema(description = "Ativa ao criar? (padrão: true)", example = "true", defaultValue = "true")
    public Boolean ativo = Boolean.TRUE;

    // ===== Validações de regra =====

    @AssertTrue(message = "vigenciaFim deve ser maior que vigenciaIni, quando informada")
    public boolean isPeriodoValido() {
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

    /** Garante defaults sem sobrescrever valores já definidos. */
    public void ensureDefaults() {
        if (ativo == null) ativo = Boolean.TRUE;
        if (prioridade == null) prioridade = 0;
    }

    /** Converte este request em entidade {@link RegraConversao}. */
    public RegraConversao toEntity() {
        ensureDefaults();
        RegraConversao r = new RegraConversao(
                this.nome,
                this.multiplicador,
                this.mccRegex,
                this.categoria,
                this.parceiroId,
                this.vigenciaIni,
                this.vigenciaFim,
                this.prioridade,
                this.tetoMensal
        );
        // Sobrescreve ativo se veio explicitamente (o construtor já define true)
        r.ativo = Boolean.TRUE.equals(this.ativo);
        return r;
    }

    /** Descrição humana curta da segmentação desta regra. */
    @Schema(hidden = true)
    public String resumoSegmentacao() {
        String mcc = (mccRegex == null || mccRegex.isBlank()) ? "qualquer MCC" : "MCC regex";
        String cat = (categoria == null || categoria.isBlank()) ? "qualquer categoria" : categoria.toUpperCase(Locale.ROOT);
        String par = (parceiroId == null) ? "qualquer parceiro" : ("parceiro " + parceiroId);
        return String.format("%s · %s · %s", mcc, cat, par);
    }
}
