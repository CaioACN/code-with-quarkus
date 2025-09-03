package org.acme.loyalty.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.acme.loyalty.entity.MovimentoPontos;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento publicado quando ocorre um AJUSTE de pontos.
 * Alinhado ao tópico de domínio (ex.: loyalty.points) e ao contrato "PointsAdjusted".
 *
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "PontosAjustadosEvent", description = "Evento emitido quando pontos são ajustados (MovimentoPontos.AJUSTE)")
public class PontosAjustadosEvent {

    // ===================== Metadados do evento =====================

    @Schema(description = "Identificador único do evento (UUID)")
    public String eventId = UUID.randomUUID().toString();

    @Schema(description = "Tipo do evento", example = "PointsAdjusted")
    public String eventType = "PointsAdjusted";

    @Schema(description = "Versão do contrato do evento", example = "1.0")
    public String version = "1.0";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp de emissão do evento")
    public LocalDateTime producedAt = LocalDateTime.now();

    @Schema(description = "ID de correlação (trace/correlation-id)", example = "b9a7c7c8-5a0e-44f3-9f2f-2b1d6a0f3e21")
    public String correlationId;

    @Schema(description = "Tenant/filial (opcional, se multi-tenant)")
    public String tenantId;

    // ===================== Payload do domínio =====================

    @Schema(description = "ID do movimento de pontos (AJUSTE)")
    public Long movimentoId;

    @Schema(description = "ID do usuário")
    public Long usuarioId;

    @Schema(description = "ID do cartão")
    public Long cartaoId;

    @Schema(description = "Tipo do movimento (esperado: AJUSTE)")
    public String tipo; // AJUSTE

    @Schema(description = "Pontos do ajuste (pode ser positivo=crédito ou negativo=débito)")
    public Integer pontos;

    @Schema(description = "Pontos em valor absoluto (sempre não-negativo)")
    public Long pontosAbsolutos;

    @Schema(description = "Direção do ajuste derivada do sinal dos pontos", example = "CREDITO")
    public String direcao; // CREDITO | DEBITO

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

    @Schema(description = "Identificador de job (quando ajuste é oriundo de processamento em lote)")
    public String jobId;

    // Campo opcional preenchido pelo serviço após aplicar o ajuste no saldo
    @Schema(description = "Saldo de pontos do usuário/cartão após o ajuste (opcional)")
    public Long saldoAposAjuste;

    // ===================== Fábricas / Mapeadores =====================

    /**
     * Cria o evento a partir de um MovimentoPontos do tipo AJUSTE.
     * Observação: relacionamentos LAZY (usuario, cartao, transacao) devem estar carregados conforme a necessidade.
     */
    public static PontosAjustadosEvent fromEntity(MovimentoPontos m) {
        return fromEntity(m, null, null, null);
    }

    public static PontosAjustadosEvent fromEntity(MovimentoPontos m, String correlationId) {
        return fromEntity(m, correlationId, null, null);
    }

    public static PontosAjustadosEvent fromEntity(MovimentoPontos m, String correlationId, String tenantId, Long saldoAposAjuste) {
        if (m == null) return null;

        PontosAjustadosEvent ev = new PontosAjustadosEvent();
        ev.correlationId = correlationId;
        ev.tenantId = tenantId;

        ev.movimentoId = m.id;
        ev.usuarioId = (m.usuario != null ? m.usuario.id : null);
        ev.cartaoId = (m.cartao != null ? m.cartao.id : null);
        ev.tipo = (m.tipo != null ? m.tipo.name() : null);

        ev.pontos = m.pontos;
        ev.pontosAbsolutos = (m.pontos == null ? null : Math.abs(m.pontos.longValue()));
        ev.direcao = (m.pontos == null) ? null : (m.pontos >= 0 ? "CREDITO" : "DEBITO");

        ev.refTransacaoId = m.refTransacaoId;
        ev.transacaoId = (m.transacao != null ? m.transacao.id : null);

        ev.criadoEm = m.criadoEm;
        ev.observacao = m.observacao;
        ev.regraAplicada = m.regraAplicada;
        ev.campanhaAplicada = m.campanhaAplicada;
        ev.jobId = m.jobId;

        ev.saldoAposAjuste = saldoAposAjuste;

        return ev;
    }

    // ===================== Utilidades =====================

    @Override
    public String toString() {
        return "PontosAjustadosEvent{" +
                "eventId='" + eventId + '\'' +
                ", movimentoId=" + movimentoId +
                ", usuarioId=" + usuarioId +
                ", cartaoId=" + cartaoId +
                ", pontos=" + pontos +
                ", direcao='" + direcao + '\'' +
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
        if (!(o instanceof PontosAjustadosEvent that)) return false;
        return Objects.equals(eventId, that.eventId);
    }
}
