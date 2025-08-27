package org.acme.loyalty.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.Resgate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de domínio publicado quando um RESGATE é SOLICITADO (PENDENTE).
 * Pensado para publicação em mensageria (ex.: Kafka em loyalty.redeems).
 *
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ResgateRequestedEvent", description = "Evento emitido quando um resgate é criado/solicitado")
public class ResgateRequestedEvent {

    // ===================== Metadados do evento =====================

    @Schema(description = "Identificador único do evento (UUID)")
    public String eventId = UUID.randomUUID().toString();

    @Schema(description = "Tipo do evento", example = "ResgateRequested")
    public String eventType = "ResgateRequested";

    @Schema(description = "Versão do contrato do evento", example = "1.0")
    public String version = "1.0";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp de emissão do evento")
    public LocalDateTime producedAt = LocalDateTime.now();

    @Schema(description = "ID de correlação (trace/correlation-id)", example = "2b1f2a9d-50a3-4cb6-9b83-6d8a1c2a7b9c")
    public String correlationId;

    @Schema(description = "Tenant/filial (opcional, se multi-tenant)")
    public String tenantId;

    // ===================== Payload do domínio =====================

    @Schema(description = "ID do resgate criado")
    public Long resgateId;

    @Schema(description = "ID do usuário solicitante")
    public Long usuarioId;

    @Schema(description = "ID do cartão utilizado")
    public Long cartaoId;

    @Schema(description = "ID da recompensa")
    public Long recompensaId;

    @Schema(description = "Pontos utilizados na solicitação")
    public Long pontos;

    @Schema(description = "Status atual do resgate (esperado: PENDENTE no momento da emissão)", example = "PENDENTE")
    public String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora de criação do resgate")
    public LocalDateTime criadoEm;

    @Schema(description = "Parceiro processador (quando aplicável)")
    public String parceiroProcessador;

    @Schema(description = "Código de rastreio/logística (se aplicável)")
    public String codigoRastreio;

    @Schema(description = "Observação/nota do pedido (opcional)")
    public String observacao;

    // ===================== Fábricas / Mapeadores =====================

    /**
     * Cria o evento a partir da entidade Resgate.
     * Observação: garanta que os relacionamentos LAZY necessários estejam carregados previamente.
     */
    public static ResgateRequestedEvent fromEntity(Resgate r) {
        return fromEntity(r, null, null);
    }

    public static ResgateRequestedEvent fromEntity(Resgate r, String correlationId) {
        return fromEntity(r, correlationId, null);
    }

    public static ResgateRequestedEvent fromEntity(Resgate r, String correlationId, String tenantId) {
        if (r == null) return null;

        ResgateRequestedEvent ev = new ResgateRequestedEvent();
        ev.correlationId = correlationId;
        ev.tenantId = tenantId;

        ev.resgateId = r.id;
        ev.usuarioId = (r.usuario != null ? r.usuario.id : null);
        ev.cartaoId = (r.cartao != null ? r.cartao.id : null);
        ev.recompensaId = (r.recompensa != null ? r.recompensa.id : null);
        ev.pontos = r.pontosUtilizados;
        ev.status = (r.status != null ? r.status.name() : null);
        ev.criadoEm = r.criadoEm;
        ev.parceiroProcessador = r.parceiroProcessador;
        ev.codigoRastreio = r.codigoRastreio;
        ev.observacao = r.observacao;

        // Garante semanticamente que este evento só seja emitido para criação
        // (não força, apenas sinaliza; a aplicação decide o momento da emissão).
        return ev;
    }

    // ===================== Utilidades =====================

    @Override
    public String toString() {
        return "ResgateRequestedEvent{" +
                "eventId='" + eventId + '\'' +
                ", resgateId=" + resgateId +
                ", usuarioId=" + usuarioId +
                ", cartaoId=" + cartaoId +
                ", recompensaId=" + recompensaId +
                ", pontos=" + pontos +
                ", status='" + status + '\'' +
                ", producedAt=" + producedAt +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResgateRequestedEvent that)) return false;
        return Objects.equals(eventId, that.eventId);
    }
}
