package org.acme.loyalty.dto;

import org.acme.loyalty.entity.CampanhaBonus;
import org.acme.loyalty.entity.CampanhaBonus.StatusVigencia;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "CampanhaBonusResponse", description = "Representação de uma campanha de bônus retornada pela API")
public class CampanhaBonusResponseDTO {

    @Schema(description = "Identificador único da campanha", example = "1")
    public Long id;

    @Schema(description = "Nome da campanha de bônus", example = "Bônus Restaurantes Setembro")
    public String nome;

    @Schema(description = "Multiplicador extra aplicado aos pontos", example = "0.2000")
    public BigDecimal multiplicadorExtra;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de início da vigência", example = "2025-09-01")
    public LocalDate vigenciaIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "Data de término da vigência (opcional)", example = "2025-09-30")
    public LocalDate vigenciaFim;

    @Schema(description = "Segmento ao qual a campanha se aplica (opcional)", example = "RESTAURANTES")
    public String segmento;

    @Schema(description = "Prioridade da campanha (maior valor = maior precedência)", example = "10")
    public Integer prioridade;

    @Schema(description = "Teto de pontos extras permitido (opcional)", example = "100000")
    public Long teto;

    @Schema(description = "Indica se a campanha está vigente na data atual", example = "true")
    public Boolean estaVigente;

    @Schema(description = "Status da vigência da campanha", 
            example = "VIGENTE", 
            enumeration = {"AGUARDANDO_INICIO", "VIGENTE", "PROXIMA_EXPIRACAO", "EXPIRADA"})
    public StatusVigencia statusVigencia;

    @Schema(description = "Data/hora de criação do registro", example = "2025-08-01T10:15:30")
    public LocalDateTime criadoEm;

    @Schema(description = "Data/hora da última atualização do registro", example = "2025-08-20T18:45:10")
    public LocalDateTime atualizadoEm;

    public CampanhaBonusResponseDTO() {}

    // --- Conversor a partir da entidade ---
    public static CampanhaBonusResponseDTO fromEntity(CampanhaBonus entity) {
        CampanhaBonusResponseDTO dto = new CampanhaBonusResponseDTO();
        dto.id = entity.id;
        dto.nome = entity.nome;
        dto.multiplicadorExtra = entity.multiplicadorExtra;
        dto.vigenciaIni = entity.vigenciaIni;
        dto.vigenciaFim = entity.vigenciaFim;
        dto.segmento = entity.segmento;
        dto.prioridade = entity.prioridade;
        dto.teto = entity.teto;

        // Calculados na entidade
        dto.estaVigente = entity.estaVigente();
        dto.statusVigencia = entity.getStatusVigencia();

        // Caso sua tabela tenha colunas de auditoria, mapeie aqui
        dto.criadoEm = null;       // Mapear se existir
        dto.atualizadoEm = null;   // Mapear se existir

        return dto;
    }
}
