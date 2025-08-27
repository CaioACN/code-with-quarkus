package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payload de criação de Transação.
 * Usado por POST /transacoes.
 *
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "TransacaoRequest", description = "Dados para registrar uma nova transação de cartão")
public class TransacaoRequestDTO {

    // Identificação
    @NotNull
    @Schema(description = "ID do cartão", example = "1", required = true)
    public Long cartaoId;

    @NotNull
    @Schema(description = "ID do usuário", example = "10", required = true)
    public Long usuarioId;

    // Financeiro
    @NotNull
    @DecimalMin(value = "0.01", message = "valor deve ser >= 0.01")
    @Digits(integer = 10, fraction = 2) // numeric(12,2) -> 10 inteiros + 2 decimais
    @Schema(description = "Valor da transação", example = "150.75", required = true)
    public BigDecimal valor;

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$", message = "moeda deve ser ISO-4217 em 3 letras maiúsculas (ex.: BRL)")
    @Schema(description = "Moeda ISO-4217 (3 letras)", example = "BRL", required = true)
    public String moeda;

    // Classificações
    @Pattern(regexp = "^\\d{4}$", message = "mcc deve conter 4 dígitos")
    @Schema(description = "Merchant Category Code (4 dígitos)", example = "5812")
    public String mcc;

    @Size(max = 60)
    @Schema(description = "Categoria livre da transação (máx. 60)", example = "RESTAURANTE")
    public String categoria;

    @Positive
    @Schema(description = "Identificador do parceiro (opcional)", example = "12345")
    public Long parceiroId;

    // Temporal
    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora do evento (UTC ou timezone do sistema)", example = "2025-08-15T12:34:56", required = true)
    public LocalDateTime dataEvento;

    // ===================== Helpers =====================

    /**
     * Normaliza campos textuais (trim/upper) antes do uso.
     * Chame no serviço antes de persistir, se desejar.
     */
    public void normalize() {
        if (moeda != null) moeda = moeda.trim().toUpperCase();
        if (categoria != null) categoria = categoria.trim();
        if (mcc != null) mcc = mcc.trim();
    }

    /**
     * Constrói a entidade Transacao a partir deste DTO.
     * Requer as entidades carregadas de Cartao e Usuario.
     */
    public org.acme.loyalty.entity.Transacao toEntity(
            org.acme.loyalty.entity.Cartao cartao,
            org.acme.loyalty.entity.Usuario usuario) {

        normalize();
        return new org.acme.loyalty.entity.Transacao(
                cartao,
                usuario,
                this.valor,
                this.moeda,
                this.mcc,
                this.categoria,
                this.parceiroId,
                this.dataEvento
        );
    }
}
