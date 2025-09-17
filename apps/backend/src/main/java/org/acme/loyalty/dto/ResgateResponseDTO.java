package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO de resposta para o recurso de Resgates.
 * Reflete o estado atual do pedido de resgate e traz resumos de usuário, cartão e recompensa.
 *
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ResgateResponse", description = "Resposta com dados do resgate e resumos relacionados")
public class ResgateResponseDTO {

    // ================== Identificação e status ==================
    @Schema(description = "ID do resgate")
    public Long id;

    @Schema(description = "Status atual do resgate (PENDENTE, APROVADO, CONCLUIDO, NEGADO, CANCELADO)")
    public String status;

    @Schema(description = "Descrição amigável do status")
    public String statusDescricao;

    // ================== Prazos / Datas ==================
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora de criação do resgate")
    public LocalDateTime criadoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora de aprovação (quando houver)")
    public LocalDateTime aprovadoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora de conclusão (quando houver)")
    public LocalDateTime concluidoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora de negação (quando houver)")
    public LocalDateTime negadoEm;

    @Schema(description = "Tempo (em horas) do ciclo até aprovação (criado→aprovado)")
    public Long horasAteAprovacao;

    @Schema(description = "Tempo (em horas) do ciclo até conclusão (criado→concluído)")
    public Long horasAteConclusao;

    @Schema(description = "Tempo (em horas) até o último marco (aprovado/concluído/negado)")
    public Long horasProcessamento;

    // ================== Dados do pedido ==================
    @Schema(description = "Pontos utilizados no resgate")
    public Long pontosUtilizados;

    @Schema(description = "Observações internas (opcional)")
    public String observacao;

    @Schema(description = "Motivo da negação (quando NEGADO)")
    public String motivoNegacao;

    @Schema(description = "Código de rastreio/logística (quando aplicável)")
    public String codigoRastreio;

    @Schema(description = "Parceiro processador (quando aplicável)")
    public String parceiroProcessador;

    // ================== IDs diretos para compatibilidade ==================
    @Schema(description = "ID do usuário (compatibilidade)")
    public Long usuarioId;

    @Schema(description = "ID do cartão (compatibilidade)")
    public Long cartaoId;

    @Schema(description = "ID da recompensa (compatibilidade)")
    public Long recompensaId;

    // ================== Resumos relacionados ==================
    @Schema(description = "Resumo do usuário")
    public UsuarioResumo usuario;

    @Schema(description = "Resumo do cartão utilizado")
    public CartaoResumo cartao;

    @Schema(description = "Resumo da recompensa")
    public RecompensaResumo recompensa;

    // ================== Tipos auxiliares ==================
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UsuarioResumo {
        public Long id;
        public String nome;
        public String email;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CartaoResumo {
        public Long id;
        public String numeroMascarado; // ****-****-****-1234
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecompensaResumo {
        public Long id;
        public String descricao;
        public String tipo;       // ex.: PRODUTO_FISICO
        public Long parceiroId;   // se existir na recompensa
    }

    // ================== Factory/Mapper ==================

    /**
     * Mapeia a entidade Resgate para o DTO de resposta.
     * Observação: este método não força inicialização de relacionamentos LAZY.
     * Garanta no serviço que os dados necessários estejam carregados.
     */
    public static ResgateResponseDTO fromEntity(org.acme.loyalty.entity.Resgate r) {
        if (r == null) return null;

        ResgateResponseDTO dto = new ResgateResponseDTO();
        dto.id = r.id;
        dto.status = (r.status != null ? r.status.name() : null);
        dto.statusDescricao = safeStatusDescricao(r);

        dto.criadoEm = r.criadoEm;
        dto.aprovadoEm = r.aprovadoEm;
        dto.concluidoEm = r.concluidoEm;
        dto.negadoEm = r.negadoEm;

        dto.horasAteAprovacao = hoursBetween(r.criadoEm, r.aprovadoEm);
        dto.horasAteConclusao = hoursBetween(r.criadoEm, r.concluidoEm);
        dto.horasProcessamento = hoursBetween(r.criadoEm, lastMilestone(r));

        dto.pontosUtilizados = r.pontosUtilizados;
        dto.observacao = r.observacao;
        dto.motivoNegacao = r.motivoNegacao;
        dto.codigoRastreio = r.codigoRastreio;
        dto.parceiroProcessador = r.parceiroProcessador;

        // Usuario
        if (r.usuario != null) {
            dto.usuarioId = r.usuario.id; // ID direto para compatibilidade
            dto.usuario = new UsuarioResumo();
            dto.usuario.id = r.usuario.id;
            // Pode estar LAZY; os campos abaixo assumem que foram carregados pelo serviço
            dto.usuario.nome = r.usuario.nome;
            dto.usuario.email = r.usuario.email;
        }

        // Cartão
        if (r.cartao != null) {
            dto.cartaoId = r.cartao.id; // ID direto para compatibilidade
            dto.cartao = new CartaoResumo();
            dto.cartao.id = r.cartao.id;
            // tenta mascarar número se disponível
            dto.cartao.numeroMascarado = tryMask(r);
        }

        // Recompensa
        if (r.recompensa != null) {
            dto.recompensaId = r.recompensa.id; // ID direto para compatibilidade
            dto.recompensa = new RecompensaResumo();
            dto.recompensa.id = r.recompensa.id;
            dto.recompensa.descricao = r.recompensa.descricao;
            dto.recompensa.tipo = (r.recompensa.tipo != null ? r.recompensa.tipo.name() : null);
            dto.recompensa.parceiroId = r.recompensa.parceiroId;
        }

        return dto;
    }

    // ================== Helpers ==================

    private static String safeStatusDescricao(org.acme.loyalty.entity.Resgate r) {
        try {
            // usa a lógica centralizada da entidade se disponível
            String desc = r.getStatusDescricao();
            if (desc != null) return desc;
        } catch (Exception ignored) {}
        // fallback simples
        if (r.status == null) return null;
        
        if (r.status == org.acme.loyalty.entity.Resgate.StatusResgate.PENDENTE) return "Aguardando Aprovação";
        if (r.status == org.acme.loyalty.entity.Resgate.StatusResgate.APROVADO) return "Aprovado";
        if (r.status == org.acme.loyalty.entity.Resgate.StatusResgate.CONCLUIDO) return "Concluído";
        if (r.status == org.acme.loyalty.entity.Resgate.StatusResgate.NEGADO) return "Negado";
        if (r.status == org.acme.loyalty.entity.Resgate.StatusResgate.CANCELADO) return "Cancelado";
        
        return "Status Desconhecido";
    }

    private static LocalDateTime lastMilestone(org.acme.loyalty.entity.Resgate r) {
        if (r == null) return null;
        if (r.concluidoEm != null) return r.concluidoEm;
        if (r.negadoEm != null) return r.negadoEm;
        if (r.aprovadoEm != null) return r.aprovadoEm;
        return null;
    }

    private static Long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return null;
        return Duration.between(start, end).toHours();
        // Se preferir precisão decimal, converta para BigDecimal como nos outros DTOs.
    }

    private static String tryMask(org.acme.loyalty.entity.Resgate r) {
        try {
            // Se o Cartao expõe método de máscara, usa-o
            String mask = r.cartao.getNumeroMascarado();
            if (mask != null && !mask.isBlank()) return mask;
        } catch (Exception ignored) {}
        // fallback: tenta mascarar se o número estiver acessível
        try {
            String n = r.cartao.numero;
            if (n == null || n.length() < 4) return null;
            String last4 = n.substring(n.length() - 4);
            return "****-****-****-" + last4;
        } catch (Exception ignored) {}
        return null;
    }

    // ================== Equals/HashCode (útil para testes) ==================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResgateResponseDTO that)) return false;
        return Objects.equals(id, that.id) &&
               Objects.equals(status, that.status) &&
               Objects.equals(criadoEm, that.criadoEm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, criadoEm);
    }
}
