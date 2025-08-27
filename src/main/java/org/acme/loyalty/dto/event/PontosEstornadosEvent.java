package org.acme.loyalty.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.MovimentoPontos;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento publicado quando ocorre um ESTORNO de pontos.
 * Alinhado ao tópico de domínio (ex.: loyalty.points) e ao contrato "PointsReversed".
 *
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "PontosEstornadosEvent", description = "Evento emitido quando pontos são estornados (MovimentoPontos.ESTORNO)")
public class PontosEstornadosEvent {

    // ===================== Metadados do evento =====================

    @Schema(description = "Identificador único do evento (UUID)")
    public String eventId = UUID.randomUUID().toString();

    @Schema(description = "Tipo do evento", example = "PointsReversed")
    public String eventType = "PointsReversed";

    @Schema(description = "Versão do contrato do evento", example = "1.0")
    public String version = "1.0";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp de emissão do evento")
    public LocalDateTime producedAt = LocalDateTime.now();

    @Schema(description = "ID de correlação (trace/correlation-id)", example = "c2a51d32-9eab-4f2e-8c6f-8c1a5f0a9b33")
    public String correlationId;

    @Schema(description = "Tenant/filial (opcional, se multi-tenant)")
    public String tenantId;

    // ===================== Payload do domínio =====================

    @Schema(description = "ID do movimento de pontos (ESTORNO)")
    public Long movimentoId;

    @Schema(description = "ID do usuário")
    public Long usuarioId;

    @Schema(description = "ID do cartão")
    public Long cartaoId;

    @Schema(description = "Tipo do movimento (esperado: ESTORNO)")
    public String tipo; // ESTORNO

    @Schema(description = "Pontos do estorno (pode ser assinado, conforme a persistência)")
    public Integer pontos; // mantém semântica da entidade

    @Schema(description = "Pontos em valor absoluto (sempre não-negativo)")
    public Long pontosAbsolutos;

    @Schema(description = "ID da transação referenciada (se houver)")
    public Long refTransacaoId;

    @Schema(description = "ID da transação associada (se relacionamento foi preenchido)")
    public Long transacaoId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora de criação do movimento")
    public LocalDateTime criadoEm;

    @Schema(description = "Observação livre do movimento")
    public String observacao;

    @Schema(description = "Regra aplicada (informativo)")
    public String regraAplicada;

    @Schema(description = "Campanha aplicada (informativo)")
    public String campanhaAplicada;

    // Campo opcional que o serviço pode preencher após aplicar o estorno no saldo
    @Schema(description = "Saldo de pontos do usuário/cartão após o estorno (opcional)")
    public Long saldoAposEstorno;

    // ===================== Fábricas / Mapeadores =====================

    /**
     * Cria o evento a partir de um MovimentoPontos do tipo ESTORNO.
     * Observação: relacionamentos LAZY (usuario, cartao, transacao) devem estar carregados conforme a necessidade.
     */
    public static PontosEstornadosEvent fromEntity(MovimentoPontos m) {
        return fromEntity(m, null, null, null);
    }

    public static PontosEstornadosEvent fromEntity(MovimentoPontos m, String correlationId) {
        return fromEntity(m, correlationId, null, null);
    }

    public static PontosEstornadosEvent fromEntity(MovimentoPontos m, String correlationId, String tenantId, Long saldoAposEstorno) {
        if (m == null) return null;

        PontosEstornadosEvent ev = new PontosEstornadosEvent();
        ev.correlationId = correlationId;
        ev.tenantId = tenantId;

        ev.movimentoId = m.id;
        ev.usuarioId = (m.usuario != null ? m.usuario.id : null);
        ev.cartaoId = (m.cartao != null ? m.cartao.id : null);
        ev.tipo = (m.tipo != null ? m.tipo.name() : null);

        ev.pontos = m.pontos;
        ev.pontosAbsolutos = (m.pontos == null ? null : Math.abs(m.pontos.longValue()));

        ev.refTransacaoId = m.refTransacaoId;
        ev.transacaoId = (m.transacao != null ? m.transacao.id : null);

        ev.criadoEm = m.criadoEm;
        ev.observacao = m.observacao;
        ev.regraAplicada = m.regraAplicada;
        ev.campanhaAplicada = m.campanhaAplicada;

        ev.saldoAposEstorno = saldoAposEstorno;

        return ev;
    }

    // ===================== Utilidades =====================

    @Override
    public String toString() {
        return "PontosEstornadosEvent{" +
                "eventId='" + eventId + '\'' +
                ", movimentoId=" + movimentoId +
                ", usuarioId=" + usuarioId +
                ", cartaoId=" + cartaoId +
                ", pontos=" + pontos +
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
        if (!(o instanceof PontosEstornadosEvent that)) return false;
        return Objects.equals(eventId, that.eventId);
    }
}
