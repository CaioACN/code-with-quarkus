package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.acme.loyalty.dto.event.TransactionCreatedEvent;
import org.acme.loyalty.dto.event.PointsAccruedEvent;
import org.acme.loyalty.dto.event.PointsExpiredEvent;
import org.acme.loyalty.dto.event.ResgateRequestedEvent;
import org.acme.loyalty.dto.event.ResgateCompletedEvent;
import org.acme.loyalty.dto.event.PontosAjustadosEvent;
import org.acme.loyalty.dto.event.PontosEstornadosEvent;
import org.acme.loyalty.dto.event.NotificacaoEnviadaEvent;

import org.acme.loyalty.entity.Transacao;
import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.entity.Resgate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventPublisherService {

    private static final Logger logger = Logger.getLogger(EventPublisherService.class.getName());

    @Inject ObjectMapper objectMapper; // fornecido por quarkus-rest-jackson

    // ============================ Publicação genérica ============================

    public void publishEvent(Object event) {
        CompletableFuture.runAsync(() -> {
            try {
                publishEventInternal(event, null, null);
            } catch (Exception e) {
                logger.severe("Erro ao publicar evento: " + e.getMessage());
                handleEventPublishError(event, e);
            }
        });
    }

    public void publishEvent(Object event, String correlationId, String messageKey) {
        CompletableFuture.runAsync(() -> {
            try {
                publishEventInternal(event, correlationId, messageKey);
            } catch (Exception e) {
                logger.severe("Erro ao publicar evento (corr=" + correlationId + "): " + e.getMessage());
                handleEventPublishError(event, e);
            }
        });
    }

    // ============================ Overloads tipados (DTO) ============================

    public void publishTransactionCreatedEvent(TransactionCreatedEvent event) { publishEvent(event); }
    public void publishPointsAccruedEvent(PointsAccruedEvent event)         { publishEvent(event); }
    public void publishPointsExpiredEvent(PointsExpiredEvent event)          { publishEvent(event); }
    public void publishResgateRequestedEvent(ResgateRequestedEvent event)    { publishEvent(event); }
    public void publishResgateCompletedEvent(ResgateCompletedEvent event)    { publishEvent(event); }
    public void publishPontosAjustadosEvent(PontosAjustadosEvent event)      { publishEvent(event); }
    public void publishPontosEstornadosEvent(PontosEstornadosEvent event)    { publishEvent(event); }
    public void publishNotificacaoEnviadaEvent(NotificacaoEnviadaEvent event){ publishEvent(event); }

    // ============================ Overloads a partir de ENTIDADES ============================

    public void publishTransactionCreated(Transacao t) {
        if (t == null) return;
        publishTransactionCreatedEvent(toTransactionCreatedEvent(t));
    }

    public void publishPointsAccrued(MovimentoPontos m) {
        if (m == null) return;
        publishPointsAccruedEvent(toPointsAccruedEvent(m));
    }

    public void publishPointsExpired(MovimentoPontos m) {
        if (m == null) return;
        publishPointsExpiredEvent(toPointsExpiredEvent(m));
    }

    public void publishResgateRequested(Resgate r) {
        if (r == null) return;
        publishResgateRequestedEvent(toResgateRequestedEvent(r));
    }

    public void publishResgateCompleted(Resgate r) {
        if (r == null) return;
        publishResgateCompletedEvent(toResgateCompletedEvent(r));
    }

    public void publishPontosAjustados(MovimentoPontos m, String jobId, String observacao) {
        if (m == null) return;
        publishPontosAjustadosEvent(toPontosAjustadosEvent(m, jobId, observacao));
    }

    public void publishPontosEstornados(MovimentoPontos m, String motivo) {
        if (m == null) return;
        publishPontosEstornadosEvent(toPontosEstornadosEvent(m, motivo));
    }

    // ============================ Builders ENTIDADE -> DTO ============================

    private TransactionCreatedEvent toTransactionCreatedEvent(Transacao t) {
        return new TransactionCreatedEvent(
            t.id,
            (t.usuario != null ? t.usuario.id : null),
            (t.cartao  != null ? t.cartao.id  : null),
            t.valor,
            t.moeda,
            t.mcc,
            t.categoria,
            t.dataEvento
        );
    }

    /** Converte MovimentoPontos -> PointsAccruedEvent respeitando Integer pontos. */
    private PointsAccruedEvent toPointsAccruedEvent(MovimentoPontos m) {
        Long pontosLong = (m.pontos != null ? m.pontos : 0L);
        Integer pontosInt = clampToInt(pontosLong);
        return new PointsAccruedEvent(
                (m.usuario != null ? m.usuario.id : null),
                (m.cartao  != null ? m.cartao.id  : null),
                pontosInt,
                m.refTransacaoId,
                (m.criadoEm != null ? m.criadoEm : LocalDateTime.now())
        );
    }

    /** Expiração: DTO usa Integer pontos; garantir negativo e evitar overflow. */
    private PointsExpiredEvent toPointsExpiredEvent(MovimentoPontos m) {
        PointsExpiredEvent e = new PointsExpiredEvent();
        e.usuarioId = (m.usuario != null ? m.usuario.id : null);
        e.cartaoId  = (m.cartao  != null ? m.cartao.id  : null);

        long pontosLong = (m.pontos != null ? m.pontos : 0L);
        long expirado   = (pontosLong > 0 ? -pontosLong : pontosLong);
        e.pontos        = clampToInt(expirado); // Integer (autobox)

        e.jobId    = m.jobId;
        e.criadoEm = (m.criadoEm != null ? m.criadoEm : LocalDateTime.now());
        return e;
    }

    /** Usa a fábrica do DTO (contrato com Long pontos). */
    private ResgateRequestedEvent toResgateRequestedEvent(Resgate r) {
        return ResgateRequestedEvent.fromEntity(r);
    }

    /** Usa a fábrica do DTO (contrato com Long pontos e SLAs). */
    private ResgateCompletedEvent toResgateCompletedEvent(Resgate r) {
        return ResgateCompletedEvent.fromEntity(r);
    }

    private PontosAjustadosEvent toPontosAjustadosEvent(MovimentoPontos m, String jobId, String observacao) {
        PontosAjustadosEvent e = new PontosAjustadosEvent();
        e.usuarioId  = (m.usuario != null ? m.usuario.id : null);
        e.cartaoId   = (m.cartao  != null ? m.cartao.id  : null);

        long pts = (m.pontos != null ? m.pontos : 0L);
        e.pontos     = clampToInt(pts); // Integer

        e.jobId      = (jobId != null ? jobId : m.jobId);
        e.criadoEm   = (m.criadoEm != null ? m.criadoEm : LocalDateTime.now());
        e.observacao = observacao;
        return e;
    }

    /** Estorno: mapeia para PontosEstornadosEvent (sem campo 'motivo'). */
    private PontosEstornadosEvent toPontosEstornadosEvent(MovimentoPontos m, String motivo) {
        PontosEstornadosEvent e = new PontosEstornadosEvent();
        e.movimentoId    = m.id;
        e.usuarioId      = (m.usuario != null ? m.usuario.id : null);
        e.cartaoId       = (m.cartao  != null ? m.cartao.id  : null);
        e.tipo           = (m.tipo != null ? m.tipo.name() : null);

        long pts = (m.pontos != null ? m.pontos : 0L); // geralmente negativo
        e.pontos         = clampToInt(pts);                           // Integer
        e.pontosAbsolutos= Math.abs(pts);                             // Long

        e.refTransacaoId = m.refTransacaoId;
        e.transacaoId    = (m.transacao != null ? m.transacao.id : null);
        e.criadoEm       = (m.criadoEm != null ? m.criadoEm : LocalDateTime.now());

        // A entidade de evento não tem 'motivo'; usamos 'observacao' para carregar o contexto.
        if (motivo != null && !motivo.isBlank()) {
            if (m.observacao != null && !m.observacao.isBlank()) {
                e.observacao = m.observacao + " | Motivo: " + motivo;
            } else {
                e.observacao = "Motivo: " + motivo;
            }
        } else {
            e.observacao = m.observacao;
        }

        e.regraAplicada     = m.regraAplicada;
        e.campanhaAplicada  = m.campanhaAplicada;

        // saldoAposEstorno (opcional) não é conhecido aqui; pode ser preenchido pelo chamador, se necessário.
        return e;
    }

    // ===== Helpers de conversão para Integer (apenas onde o contrato exige Integer) =====

    private static int clampToInt(long v) {
        if (v > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (v < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) v;
    }

    private static Integer clampToInt(Long value) {
        if (value == null) return 0;
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return value.intValue();
    }

    // ============================ Infra “stub” (trocar por Kafka/Rabbit depois) ============================

    private void publishEventInternal(Object event, String correlationId, String messageKey) {
        String topic   = determineTopic(event);
        String key     = (messageKey != null ? messageKey : determineKey(event));
        String type    = event.getClass().getSimpleName();
        String payload = safeJson(event);

        logger.info(() -> "[EVENT] topic=" + topic
                + " key=" + key
                + (correlationId != null ? " corr=" + correlationId : "")
                + " type=" + type
                + " payload=" + payload);

        try { Thread.sleep(50); } // simula ACK
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException("Publicação interrompida", e); }
    }

    private String determineTopic(Object event) {
        if (event instanceof PointsAccruedEvent pae)       return pae.getTopic();
        if (event instanceof TransactionCreatedEvent tce)  return tce.getTopic();
        String type = event.getClass().getSimpleName();
        return switch (type) {
            case "PointsExpiredEvent", "PontosAjustadosEvent", "PontosEstornadosEvent" -> "loyalty.points";
            case "ResgateRequestedEvent", "ResgateCompletedEvent" -> "loyalty.resgates";
            case "NotificacaoEnviadaEvent" -> "loyalty.notifications";
            default -> "loyalty.events";
        };
    }

    private String determineKey(Object event) {
        if (event instanceof PointsAccruedEvent pae)       return pae.getEventKey();
        if (event instanceof TransactionCreatedEvent tce)  return tce.getEventKey();
        try {
            if (event instanceof PointsExpiredEvent e && e.usuarioId != null)       return "u:" + e.usuarioId;
            if (event instanceof ResgateRequestedEvent e && e.resgateId != null)    return "r:" + e.resgateId;
            if (event instanceof ResgateCompletedEvent e && e.resgateId != null)    return "r:" + e.resgateId;
            if (event instanceof PontosAjustadosEvent e && e.usuarioId != null)     return "u:" + e.usuarioId;
            if (event instanceof PontosEstornadosEvent e && e.usuarioId != null)    return "u:" + e.usuarioId;
        } catch (Exception ignore) {}
        return null;
    }

    private String safeJson(Object obj) {
        if (objectMapper == null || obj == null) return String.valueOf(obj);
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return "<json_error:" + e.getMessage() + ">"; }
    }

    private void handleEventPublishError(Object event, Exception error) {
        String eventType = event.getClass().getSimpleName();
        logger.severe("Falha na publicação do evento " + eventType + ": " + error.getMessage());
        logger.warning("Evento " + eventType + " enviado para DLQ (simulado) para reprocessamento posterior");
    }

    // ============================ Retry & Batch ============================

    public void publishEventWithRetry(Object event, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                publishEventInternal(event, null, null);
                return;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    handleEventPublishError(event, e);
                    throw new RuntimeException("Falha na publicação após " + maxRetries + " tentativas", e);
                }
                long delay = (long) Math.pow(2, attempt) * 1000L;
                try { Thread.sleep(delay); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException("Retry interrompido", ie); }
                logger.warning("Tentativa " + attempt + " falhou, tentando novamente em " + delay + "ms");
            }
        }
    }

    public void publishEventBatch(List<Object> events) {
        if (events == null || events.isEmpty()) return;

        logger.info("Publicando lote de " + events.size() + " eventos");

        Map<String, List<Object>> eventsByTopic = events.stream()
                .collect(Collectors.groupingBy(this::determineTopic));

        eventsByTopic.forEach((topic, topicEvents) -> {
            try {
                publishEventBatchToTopic(topic, topicEvents);
            } catch (Exception e) {
                logger.severe("Erro ao publicar lote para tópico " + topic + ": " + e.getMessage());
                topicEvents.forEach(this::publishEvent);
            }
        });
    }

    private void publishEventBatchToTopic(String topic, List<Object> events) {
        logger.info("Publicando " + events.size() + " eventos no tópico " + topic);
        try {
            Thread.sleep(events.size() * 30L);
            logger.info("Lote publicado com sucesso no tópico " + topic);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Publicação em lote interrompida", e);
        }
    }
}
