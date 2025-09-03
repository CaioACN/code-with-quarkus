package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.RegraConversao;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resposta pública para {@link RegraConversao}.
 * Inclui campos derivados como status de vigência e resumo de segmentação.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RegraConversaoResponse", description = "Regra de conversão e metadados derivados")
public class RegraConversaoResponseDTO {

    // ==== Identidade e campos básicos ====
    @Schema(description = "ID da regra", example = "7")
    public Long id;

    @Schema(description = "Nome da regra", example = "1 ponto por BRL em Restaurantes")
    public String nome;

    @Schema(description = "Multiplicador (numeric(8,4))", example = "1.0000")
    public BigDecimal multiplicador;

    @Schema(description = "Regex de MCC (vazio = qualquer MCC)")
    public String mccRegex;

    @Schema(description = "Categoria da transação (vazio = qualquer categoria)", example = "RESTAURANTE")
    public String categoria;

    @Schema(description = "Identificador do parceiro (null = qualquer parceiro)", example = "12345")
    public Long parceiroId;

    @Schema(description = "Prioridade (maior vence em empate)", example = "10")
    public Integer prioridade;

    @Schema(description = "Teto mensal de pontos (null = ilimitado)", example = "50000")
    public Long tetoMensal;

    @Schema(description = "Ativa?")
    public Boolean ativo;

    // ==== Datas ====
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início da vigência")
    public LocalDateTime vigenciaIni;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Fim da vigência (opcional)")
    public LocalDateTime vigenciaFim;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Criada em")
    public LocalDateTime criadoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Atualizada em")
    public LocalDateTime atualizadoEm;

    // ==== Derivados úteis ao front ====
    @Schema(description = "Está vigente agora?")
    public Boolean vigente;

    @Schema(description = "Está expirada?")
    public Boolean expirada;

    @Schema(description = "Status de vigência (INATIVA | AGUARDANDO_INICIO | VIGENTE | PROXIMA_EXPIRACAO | EXPIRADA)")
    public String statusVigencia;

    @Schema(description = "Dias restantes até o fim (se houver vigência fim)")
    public Integer diasParaFim;

    @Schema(description = "Possui teto mensal de pontos?")
    public Boolean temTetoMensal;

    @Schema(description = "Resumo da segmentação (MCC/Categoria/Parceiro)")
    public String resumoSegmentacao;

    public RegraConversaoResponseDTO() {}

    // ====================== Factories ======================

    public static RegraConversaoResponseDTO fromEntity(RegraConversao r) {
        if (r == null) return null;

        RegraConversaoResponseDTO dto = new RegraConversaoResponseDTO();
        dto.id = r.id;
        dto.nome = r.nome;
        dto.multiplicador = r.multiplicador;
        dto.mccRegex = r.mccRegex;
        dto.categoria = r.categoria;
        dto.parceiroId = r.parceiroId;
        dto.prioridade = r.prioridade;
        dto.tetoMensal = r.tetoMensal;
        dto.ativo = r.ativo;

        dto.vigenciaIni = r.vigenciaIni;
        dto.vigenciaFim = r.vigenciaFim;
        dto.criadoEm = r.criadoEm;
        dto.atualizadoEm = r.atualizadoEm;

        // Derivados
        LocalDateTime now = LocalDateTime.now();
        boolean started = dto.vigenciaIni != null && (now.isAfter(dto.vigenciaIni) || now.isEqual(dto.vigenciaIni));
        boolean ended = dto.vigenciaFim != null && now.isAfter(dto.vigenciaFim);

        dto.expirada = ended;
        dto.vigente = Boolean.TRUE.equals(dto.ativo) && started && !ended;

        // status
        if (!Boolean.TRUE.equals(dto.ativo)) {
            dto.statusVigencia = "INATIVA";
        } else if (!started) {
            dto.statusVigencia = "AGUARDANDO_INICIO";
        } else if (ended) {
            dto.statusVigencia = "EXPIRADA";
        } else {
            boolean proxima = dto.vigenciaFim != null &&
                    (now.isAfter(dto.vigenciaFim.minusDays(7)) || now.isEqual(dto.vigenciaFim.minusDays(7)));
            dto.statusVigencia = proxima ? "PROXIMA_EXPIRACAO" : "VIGENTE";
        }

        // diasParaFim
        if (dto.vigenciaFim != null) {
            long dias = Duration.between(now, dto.vigenciaFim).toDays();
            dto.diasParaFim = (int) dias;
        }

        dto.temTetoMensal = r.temTetoMensal();
        dto.resumoSegmentacao = buildResumo(dto.mccRegex, dto.categoria, dto.parceiroId);

        return dto;
    }

    public static List<RegraConversaoResponseDTO> fromEntityList(List<RegraConversao> list) {
        List<RegraConversaoResponseDTO> out = new ArrayList<>();
        if (list == null || list.isEmpty()) return out;
        for (RegraConversao r : list) out.add(fromEntity(r));
        return out;
    }

    // ====================== Helpers ======================

    private static String buildResumo(String mccRegex, String categoria, Long parceiroId) {
        String mcc = (mccRegex == null || mccRegex.isBlank()) ? "qualquer MCC" : "MCC regex";
        String cat = (categoria == null || categoria.isBlank())
                ? "qualquer categoria"
                : categoria.toUpperCase(Locale.ROOT);
        String par = (parceiroId == null) ? "qualquer parceiro" : ("parceiro " + parceiroId);
        return String.format("%s · %s · %s", mcc, cat, par);
    }
}
