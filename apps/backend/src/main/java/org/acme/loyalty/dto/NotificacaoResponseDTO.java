package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Resposta do serviço de notificações: status geral e por canal (e-mail/SMS/push),
 * IDs correlatos e carimbo de tempo do processamento.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "NotificacaoResponse", description = "Resultado do envio de notificação por canal")
public class NotificacaoResponseDTO {

    // ========= Identificação / correlação =========
    @Schema(description = "ID interno da notificação (gerado pelo serviço)", example = "987654321")
    public Long id;

    @Schema(description = "Correlation ID para rastreamento distribuído", example = "corr-5f84c9b2")
    public String correlationId;

    @Schema(description = "Chave de idempotência/deduplicação recebida", example = "notif-usuario10-20250827-accum-123")
    public String dedupKey;

    @Schema(description = "Evento motivador (ecos do request)")
    public NotificacaoRequestDTO.Evento evento;

    @Schema(description = "Destinatário principal (se resolvido por usuário)")
    public Long usuarioId;

    // ========= Status geral =========
    @Schema(description = "Status geral do processamento",
            enumeration = {"AGENDADA","ENFILEIRADA","RETENTANDO","ENVIADA","FALHA","CANCELADA"},
            example = "ENFILEIRADA")
    public Status status = Status.ENFILEIRADA;

    public enum Status {
        AGENDADA,   // agendado para envio futuro
        ENFILEIRADA, // colocado na fila
        RETENTANDO, // tentando reenviar
        ENVIADA,    // todos os canais enviados
        FALHA,      // todos os canais falharam
        CANCELADA   // cancelado antes do envio
    }

    @Schema(description = "Mensagem/resumo do processamento", example = "Agendado para 2025-08-27T09:30:00")
    public String message;

    // ========= Timestamps =========
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Criação/aceite da solicitação")
    public LocalDateTime createdAt = LocalDateTime.now();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Horário de agendamento (se houver)")
    public LocalDateTime scheduledFor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Início do envio")
    public LocalDateTime sentAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Confirmação de entrega (quando todos os canais reportam entregue)")
    public LocalDateTime deliveredAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Horário da falha (geral)")
    public LocalDateTime failedAt;

    // ========= Resultado por canal =========
    @Schema(description = "Resultados por canal")
    public List<CanalResultado> canais;

    @Schema(description = "Total de canais considerados")
    public Integer totalCanais;

    @Schema(description = "Canais com sucesso (enviado/entregue)")
    public Integer canaisSucesso;

    @Schema(description = "Canais com falha")
    public Integer canaisFalha;

    // ========= Reentregas / DLQ =========
    @Schema(description = "Tentativas executadas (máximo entre os canais)")
    public Integer retryCount;

    @Schema(description = "Encaminhado para DLQ (fila de mortos)?")
    public Boolean sentToDlq;

    // ========= Construtores =========
    public NotificacaoResponseDTO() {}

    // ========= Tipos auxiliares =========
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Resultado detalhado por canal")
    public static class CanalResultado {
        @Schema(description = "Canal", enumeration = {"EMAIL","PUSH","SMS","WEBHOOK"}, example = "EMAIL")
        public Canal canal;

        public enum Canal { EMAIL, PUSH, SMS, WEBHOOK }

        @Schema(description = "Status do canal",
                enumeration = {"AGENDADA","ENFILEIRADA","RETENTANDO","ENVIADA","FALHA","CANCELADA"},
                example = "ENFILEIRADA")
        public CanalStatus status = CanalStatus.ENFILEIRADA;

        public enum CanalStatus { AGENDADA, ENFILEIRADA, RETENTANDO, ENVIADA, FALHA, CANCELADA }

        @Schema(description = "Provedor/transport utilizado", example = "ses | sns | firebase | twilio")
        public String provider;

        @Schema(description = "ID da mensagem no provedor (para consulta posterior)", example = "0102018f1fEXAMPLE")
        public String providerMessageId;

        @Schema(description = "Tentativas executadas neste canal", example = "1")
        public Integer attempts;

        @Schema(description = "Código de erro do provedor (se falha)", example = "ThrottlingException")
        public String errorCode;

        @Schema(description = "Mensagem de erro (se falha)", example = "Daily quota exceeded")
        public String errorMessage;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Última tentativa/atualização")
        public LocalDateTime lastUpdateAt;

        @Schema(description = "Metadados adicionais (headers, response codes, etc.)")
        public Map<String, Object> metadata;

        public CanalResultado() {}

        public static CanalResultado of(Canal canal, CanalStatus status, String provider) {
            CanalResultado cr = new CanalResultado();
            cr.canal = canal;
            cr.status = status;
            cr.provider = provider;
            cr.attempts = 0;
            cr.lastUpdateAt = LocalDateTime.now();
            return cr;
        }
    }

    // ========= Fábricas / helpers =========

    /** Cria resposta básica a partir do request, marcando agendamento se existir. */
    public static NotificacaoResponseDTO acceptedFrom(NotificacaoRequestDTO req, Long generatedId) {
        NotificacaoResponseDTO r = new NotificacaoResponseDTO();
        r.id = generatedId;
        r.correlationId = req.correlationId;
        r.dedupKey = req.dedupKey;
        r.evento = req.evento;
        r.usuarioId = req.usuarioId;

        // Inicializa canais conforme seleção do request
        r.canais = new ArrayList<>();
        if (Boolean.TRUE.equals(req.viaEmail)) r.canais.add(CanalResultado.of(CanalResultado.Canal.EMAIL, CanalResultado.CanalStatus.ENFILEIRADA, null));
        if (Boolean.TRUE.equals(req.viaSms))   r.canais.add(CanalResultado.of(CanalResultado.Canal.SMS,   CanalResultado.CanalStatus.ENFILEIRADA, null));
        if (Boolean.TRUE.equals(req.viaPush))  r.canais.add(CanalResultado.of(CanalResultado.Canal.PUSH,  CanalResultado.CanalStatus.ENFILEIRADA, null));

        if (req.enviarApos != null) {
            r.status = Status.AGENDADA;
            r.scheduledFor = req.enviarApos;
            r.message = "Agendado";
        } else {
            r.status = Status.ENFILEIRADA;
            r.message = "Enfileirado";
        }
        r.recomputeTotals();
        return r;
    }

    /** Atualiza status geral com base nos canais. */
    public void recomputeTotals() {
        if (canais == null || canais.isEmpty()) {
            totalCanais = 0; canaisSucesso = 0; canaisFalha = 0;
            status = Status.FALHA;
            return;
        }
        totalCanais = canais.size();
        int ok = 0, fail = 0, sent = 0, scheduled = 0;
        int retriesMax = 0;
        for (CanalResultado c : canais) {
            if (c.attempts != null) retriesMax = Math.max(retriesMax, c.attempts);
            switch (c.status) {
                case ENVIADA -> { ok++; sent++; }
                case AGENDADA -> scheduled++;
                case FALHA    -> fail++;
                case CANCELADA -> fail++;
                case ENFILEIRADA -> {}
                case RETENTANDO -> {}
            }
        }
        this.retryCount = retriesMax;
        this.canaisSucesso = ok;
        this.canaisFalha = fail;

        if (sent == totalCanais) {
            status = Status.ENVIADA;
            if (sentAt == null) sentAt = LocalDateTime.now();
        } else if (ok > 0 && fail > 0) {
            status = Status.ENVIADA;
        } else if (ok > 0) {
            status = Status.ENVIADA;
            if (sentAt == null) sentAt = LocalDateTime.now();
        } else if (scheduled == totalCanais) {
            status = Status.AGENDADA;
        } else if (fail == totalCanais) {
            status = Status.FALHA;
            if (failedAt == null) failedAt = LocalDateTime.now();
        } else {
            // mistura de ENFILEIRADA/ENVIADA/AGENDADA
            if (sent > 0) status = Status.ENVIADA;
            else status = Status.ENFILEIRADA;
        }
    }

    /** Marca DLQ e ajusta status/horário. */
    public void markDlq(String reason) {
        this.sentToDlq = true;
        this.status = Status.FALHA;
        this.failedAt = LocalDateTime.now();
        if (this.message == null || this.message.isBlank()) {
            this.message = "Enviado para DLQ: " + Objects.toString(reason, "motivo não informado");
        }
    }

    /** Verdadeiro se não há falhas e pelo menos um canal foi enviado. */
    public boolean isSuccessful() {
        return status == Status.ENVIADA;
    }
}
