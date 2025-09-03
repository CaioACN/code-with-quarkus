package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.Recompensa;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resposta pública da entidade Recompensa (catálogo).
 * Inclui campos derivados úteis ao front (disponibilidade, status de estoque, etc.).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "RecompensaResponse", description = "Recompensa do catálogo e seus metadados públicos")
public class RecompensaResponseDTO {

    // ===== Identidade / básicos =====
    @Schema(description = "ID da recompensa", example = "42")
    public Long id;

    @Schema(description = "Tipo da recompensa",
            enumeration = {"PRODUTO_FISICO","PRODUTO_DIGITAL","DESCONTO","CASHBACK","MILHAS","EXPERIENCIA","OUTRO"})
    public Recompensa.TipoRecompensa tipo;

    @Schema(description = "Descrição/título", example = "Fone Bluetooth XYZ")
    public String descricao;

    @Schema(description = "Custo em pontos", example = "2500")
    public Long custoPontos;

    @Schema(description = "Estoque atual", example = "100")
    public Long estoque;

    @Schema(description = "Parceiro (se aplicável)", example = "12345")
    public Long parceiroId;

    @Schema(description = "Ativa no catálogo?")
    public Boolean ativo;

    @Schema(description = "Detalhes adicionais (texto livre)")
    public String detalhes;

    @Schema(description = "URL da imagem ilustrativa")
    public String imagemUrl;

    // ===== Datas =====
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Validade da recompensa (se definida)")
    public LocalDateTime validadeRecompensa;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Criada em")
    public LocalDateTime criadoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Atualizada em")
    public LocalDateTime atualizadoEm;

    // ===== Derivados úteis ao front =====
    @Schema(description = "Disponível para resgate agora? (ativo && estoque>0 && não vencida)")
    public Boolean disponivel;

    @Schema(description = "Está vencida?")
    public Boolean vencida;

    @Schema(description = "Status de estoque (INATIVA, SEM_ESTOQUE, ESTOQUE_BAIXO, ESTOQUE_MEDIO, ESTOQUE_ALTO)")
    public String statusEstoque;

    @Schema(description = "Dias até vencer (se validadeRecompensa definida)")
    public Integer diasParaVencer;

    @Schema(description = "Quantidade de resgates associados (se carregados)")
    public Integer resgatesCount;

    public RecompensaResponseDTO() {}

    // ========================= Factory =========================

    public static RecompensaResponseDTO fromEntity(Recompensa r) {
        if (r == null) return null;

        RecompensaResponseDTO dto = new RecompensaResponseDTO();
        dto.id = r.id;
        dto.tipo = r.tipo;
        dto.descricao = r.descricao;
        dto.custoPontos = r.custoPontos;
        dto.estoque = r.estoque;
        dto.parceiroId = r.parceiroId;
        dto.ativo = r.ativo;
        dto.detalhes = r.detalhes;
        dto.imagemUrl = r.imagemUrl;

        dto.validadeRecompensa = r.validadeRecompensa;
        dto.criadoEm = r.criadoEm;
        dto.atualizadoEm = r.atualizadoEm;

        // Derivados (usando as regras da entidade)
        boolean isVencida = r.estaVencida();
        dto.vencida = isVencida;
        dto.disponivel = r.estaDisponivel();
        dto.statusEstoque = r.getStatusEstoque();

        if (r.validadeRecompensa != null) {
            long dias = Duration.between(LocalDateTime.now(), r.validadeRecompensa).toDays();
            dto.diasParaVencer = (int) dias;
        }

        // Pode causar lazy init se a coleção não foi carregada; por isso é opcional.
        if (r.resgates != null) {
            dto.resgatesCount = r.resgates.size();
        }

        return dto;
    }

    public static List<RecompensaResponseDTO> fromEntityList(List<Recompensa> list) {
        List<RecompensaResponseDTO> out = new ArrayList<>();
        if (list == null || list.isEmpty()) return out;
        for (Recompensa r : list) {
            out.add(fromEntity(r));
        }
        return out;
    }
}
