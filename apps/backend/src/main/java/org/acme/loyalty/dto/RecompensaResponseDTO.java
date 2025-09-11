package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Schema(name = "RecompensaResponse", description = "Recompensa do catálogo e seus metadados públicos")
public class RecompensaResponseDTO {

    // ===== Identidade / básicos =====
    @Schema(description = "ID da recompensa", example = "42")
    @JsonProperty("id")
    public Long id;

    @Schema(description = "Tipo da recompensa",
            enumeration = {"MILHAS","GIFT","CASHBACK","PRODUTO"})
    @JsonProperty("tipo")
    public String tipo;

    @Schema(description = "Descrição/título", example = "Fone Bluetooth XYZ")
    @JsonProperty("descricao")
    public String descricao;

    @Schema(description = "Custo em pontos", example = "2500")
    @JsonProperty("custoPontos")
    public Long custoPontos;

    @Schema(description = "Estoque atual", example = "100")
    @JsonProperty("estoque")
    public Long estoque;

    @Schema(description = "Parceiro (se aplicável)", example = "12345")
    @JsonProperty("parceiroId")
    public Long parceiroId;

    @Schema(description = "Ativa no catálogo?")
    @JsonProperty("ativo")
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
    
    @Schema(description = "Disponível para resgate? (ativo && custoPontos>0 && disponivel)")
    public Boolean disponivelParaResgate;

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

        System.out.println("DEBUG: fromEntity - Entidade recebida: " + r);
        System.out.println("DEBUG: fromEntity - ID: " + r.id);
        System.out.println("DEBUG: fromEntity - Tipo: " + r.tipo);
        System.out.println("DEBUG: fromEntity - Descrição: " + r.descricao);

        RecompensaResponseDTO dto = new RecompensaResponseDTO();
        dto.id = r.id;
        dto.tipo = r.tipo != null ? r.tipo.name() : null;
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
        dto.vencida = r.estaVencida();
        dto.disponivel = r.estaDisponivel();
        dto.disponivelParaResgate = r.estaDisponivelParaResgate();
        dto.statusEstoque = r.getStatusEstoque();
        
        System.out.println("DEBUG: fromEntity - DTO vencida: " + dto.vencida);
        System.out.println("DEBUG: fromEntity - DTO disponivel: " + dto.disponivel);
        System.out.println("DEBUG: fromEntity - DTO disponivelParaResgate: " + dto.disponivelParaResgate);
        System.out.println("DEBUG: fromEntity - DTO statusEstoque: " + dto.statusEstoque);
        System.out.println("DEBUG: fromEntity - Entidade ativo: " + r.ativo);
        System.out.println("DEBUG: fromEntity - Entidade estoque: " + r.estoque);
        System.out.println("DEBUG: fromEntity - Entidade custoPontos: " + r.custoPontos);

        System.out.println("DEBUG: fromEntity - DTO final: " + dto);
        System.out.println("DEBUG: fromEntity - DTO ID final: " + dto.id);
        System.out.println("DEBUG: fromEntity - DTO Tipo final: " + dto.tipo);
        System.out.println("DEBUG: fromEntity - DTO Descrição final: " + dto.descricao);

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
