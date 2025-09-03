package org.acme.loyalty.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.Resgate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de domínio publicado quando um RESGATE é CONCLUÍDO
 * (ou finalizado em qualquer estado terminal: CONCLUIDO, NEGADO, CANCELADO).
 *
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ResgateCompletedEvent", description = "Evento emitido quando um resgate é finalizado (CONCLUIDO/NEGADO/CANCELADO)")
public class ResgateCompletedEvent {

    // ===================== Metadados do evento =====================

    @Schema(description = "Identificador único do evento (UUID)")
    public String eventId = UUID.randomUUID().toString();

    @Schema(description = "Tipo do evento", example = "ResgateCompleted")
    public String eventType = "ResgateCompleted";

    @Schema(description = "Versão do contrato do evento", example = "1.0")
    public String version = "1.0";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp de emissão do evento")
    public LocalDateTime producedAt = LocalDateTime.now();

    @Schema(description = "ID de correlação (trace/correlation-id)")
    public String correlationId;

    @Schema(description = "Tenant/filial (opcional, se multi-tenant)")
    public String tenantId;

    // ===================== Payload do domínio =====================

    @Schema(description = "ID do resgate finalizado")
    public Long resgateId;

    @Schema(description = "ID do usuário")
    public Long usuarioId;

    @Schema(description = "ID do cartão")
    public Long cartaoId;

    @Schema(description = "ID da recompensa")
    public Long recompensaId;

    @Schema(description = "Pontos utilizados")
    public Long pontos;

    @Schema(description = "Status final (CONCLUIDO, NEGADO, CANCELADO)")
    public String statusFinal;

    @Schema(description = "Indica sucesso (true quando statusFinal=CONCLUIDO)")
    public Boolean sucesso;

    @Schema(description = "Motivo de negação (quando NEGADO)")
    public String motivoNegacao;

    @Schema(description = "Parceiro processador (quando aplicável)")
    public String parceiroProcessador;

    @Schema(description = "Código de rastreio/logística (quando aplicável)")
    public String codigoRastreio;

    @Schema(description = "Observação do pedido (opcional)")
    public String observacao;

    // --- Datas e SLAs ---

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Criado em")
    public LocalDateTime criadoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Aprovado em (quando aplicável)")
    public LocalDateTime aprovadoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Concluído em (quando aplicável)")
    public LocalDateTime concluidoEm;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Negado em (quando aplicável)")
    public LocalDateTime negadoEm;

    @Schema(description = "Horas do ciclo criado→aprovado (quando aplicável)")
    public Long horasCriadoAprovado;

    @Schema(description = "Horas do ciclo criado→concluído (quando aplicável)")
    public Long horasCriadoConcluido;

    @Schema(description = "Horas do ciclo aprovado→concluído (quando aplicável)")
    public Long horasAprovadoConcluido;

    // ===================== Fábricas / Mapeadores =====================

    public static ResgateCompletedEvent fromEntity(Resgate r) {
        return fromEntity(r, null, null);
    }

    public static ResgateCompletedEvent fromEntity(Resgate r, String correlationId) {
        return fromEntity(r, correlationId, null);
    }

    public static ResgateCompletedEvent fromEntity(Resgate r, String correlationId, String tenantId) {
        if (r == null) return null;

        ResgateCompletedEvent ev = new ResgateCompletedEvent();
        ev.correlationId = correlationId;
        ev.tenantId = tenantId;

        ev.resgateId = r.id;
        ev.usuarioId = (r.usuario != null ? r.usuario.id : null);
        ev.cartaoId = (r.cartao != null ? r.cartao.id : null);
        ev.recompensaId = (r.recompensa != null ? r.recompensa.id : null);
        ev.pontos = r.pontosUtilizados;

        ev.statusFinal = (r.status != null ? r.status.name() : null);
        ev.sucesso = Boolean.valueOf(Resgate.StatusResgate.CONCLUIDO.equals(r.status));
        ev.motivoNegacao = r.motivoNegacao;
        ev.parceiroProcessador = r.parceiroProcessador;
        ev.codigoRastreio = r.codigoRastreio;
        ev.observacao = r.observacao;

        ev.criadoEm = r.criadoEm;
        ev.aprovadoEm = r.aprovadoEm;
        ev.concluidoEm = r.concluidoEm;
        ev.negadoEm = r.negadoEm;

        ev.horasCriadoAprovado = hoursBetween(r.criadoEm, r.aprovadoEm);
        ev.horasCriadoConcluido = hoursBetween(r.criadoEm, r.concluidoEm);
        ev.horasAprovadoConcluido = hoursBetween(r.aprovadoEm, r.concluidoEm);

        return ev;
    }

    // ===================== Utilidades =====================

    private static Long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return null;
        return Duration.between(start, end).toHours();
    }

    @Override
    public String toString() {
        return "ResgateCompletedEvent{" +
                "eventId='" + eventId + '\'' +
                ", resgateId=" + resgateId +
                ", statusFinal='" + statusFinal + '\'' +
                ", sucesso=" + sucesso +
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
        if (!(o instanceof ResgateCompletedEvent that)) return false;
        return Objects.equals(eventId, that.eventId);
    }
}
