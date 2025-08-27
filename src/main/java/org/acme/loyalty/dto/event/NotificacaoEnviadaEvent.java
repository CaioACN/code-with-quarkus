package org.acme.loyalty.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento publicado quando uma NOTIFICAÇÃO é efetivamente ENVIADA (ou processada pelo gateway).
 * Alinhado a um tópico como "loyalty.notifications".
 *
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "NotificacaoEnviadaEvent", description = "Evento emitido quando uma notificação é enviada")
public class NotificacaoEnviadaEvent {

    // ===================== Metadados do evento =====================

    @Schema(description = "Identificador único do evento (UUID)")
    public String eventId = UUID.randomUUID().toString();

    @Schema(description = "Tipo do evento", example = "NotificationSent")
    public String eventType = "NotificationSent";

    @Schema(description = "Versão do contrato do evento", example = "1.0")
    public String version = "1.0";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp de emissão do evento")
    public LocalDateTime producedAt = LocalDateTime.now();

    @Schema(description = "ID de correlação (trace/correlation-id)", example = "a2d5a1f0-9c3e-4a13-8db2-2c8d6c1a9b21")
    public String correlationId;

    @Schema(description = "Tenant/filial (opcional, se multi-tenant)")
    public String tenantId;

    // ===================== Payload do domínio =====================

    @Schema(description = "ID interno da notificação")
    public Long notificacaoId;

    @Schema(description = "ID do usuário destinatário (quando aplicável)")
    public Long usuarioId;

    @Schema(description = "Canal de envio", enumeration = {"EMAIL","PUSH","SMS","WEBHOOK"})
    public String canal;

    @Schema(description = "Tipo/assunto de negócio", example = "ACUMULO | EXPIRACAO | RESGATE | SISTEMA | AJUSTE")
    public String tipo;

    @Schema(description = "Título/assunto da notificação")
    public String titulo;

    @Schema(description = "Mensagem (texto resumido ou template renderizado)")
    public String mensagem;

    @Schema(description = "Destino (e-mail/telefone/token) — preferencialmente mascarado")
    public String destino;

    @Schema(description = "Status de entrega", enumeration = {"ENVIADA","FALHA","RETENTANDO","AGENDADA"})
    public String status;

    @Schema(description = "Número de tentativas já realizadas")
    public Integer tentativas;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp do envio (quando o gateway aceitou)")
    public LocalDateTime enviadoEm;

    @Schema(description = "Mensagem de erro (se houver falha)")
    public String erroMensagem;

    @Schema(description = "Nome/ID do provedor/gateway (ex.: ses, twilio, fcm)")
    public String provider;

    @Schema(description = "ID do provedor para rastreio (messageId)")
    public String providerMessageId;

    @Schema(description = "Metadados adicionais")
    public Map<String, Object> metadata = new LinkedHashMap<>();

    // ===================== Fábricas / Mapeadores =====================

    /**
     * Constrói o evento a partir de dados básicos.
     * Útil quando não há uma entidade/DTO padronizada.
     */
    public static NotificacaoEnviadaEvent ofBasic(Long notificacaoId,
                                                  Long usuarioId,
                                                  String canal,
                                                  String tipo,
                                                  String titulo,
                                                  String mensagem,
                                                  String destino,
                                                  String status,
                                                  Integer tentativas,
                                                  LocalDateTime enviadoEm,
                                                  String provider,
                                                  String providerMessageId,
                                                  String correlationId,
                                                  String tenantId) {
        NotificacaoEnviadaEvent ev = new NotificacaoEnviadaEvent();
        ev.notificacaoId = notificacaoId;
        ev.usuarioId = usuarioId;
        ev.canal = canal;
        ev.tipo = tipo;
        ev.titulo = titulo;
        ev.mensagem = mensagem;
        ev.destino = mask(destino);
        ev.status = status;
        ev.tentativas = tentativas;
        ev.enviadoEm = enviadoEm;
        ev.provider = provider;
        ev.providerMessageId = providerMessageId;
        ev.correlationId = correlationId;
        ev.tenantId = tenantId;
        return ev;
    }

    /**
     * Constrói a partir de um DTO de resposta de notificação (se existir no projeto).
     * Este método é "best effort": apenas mapeia campos que encontrar.
     */
    public static NotificacaoEnviadaEvent fromResponseDTO(Object maybeDto) {
        if (maybeDto == null) return null;
        NotificacaoEnviadaEvent ev = new NotificacaoEnviadaEvent();
        try {
            // Reflection simples para evitar dependência rígida do tipo.
            var cls = maybeDto.getClass();
            ev.notificacaoId     = getLong(cls, maybeDto, "id");
            ev.usuarioId         = getLong(cls, maybeDto, "usuarioId");
            ev.canal             = getString(cls, maybeDto, "canal");
            ev.tipo              = getString(cls, maybeDto, "tipo");
            ev.titulo            = getString(cls, maybeDto, "titulo");
            ev.mensagem          = getString(cls, maybeDto, "mensagem");
            ev.destino           = mask(getString(cls, maybeDto, "destino"));
            ev.status            = getString(cls, maybeDto, "status");
            ev.tentativas        = getInteger(cls, maybeDto, "tentativas");
            ev.enviadoEm         = getDateTime(cls, maybeDto, "enviadoEm");
            ev.erroMensagem      = getString(cls, maybeDto, "erroMensagem");
            ev.provider          = getString(cls, maybeDto, "provider");
            ev.providerMessageId = getString(cls, maybeDto, "providerMessageId");
            // metadata (se existir)
            Object meta = getField(cls, maybeDto, "metadata");
            if (meta instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    ev.metadata.put(String.valueOf(e.getKey()), e.getValue());
                }
            }
        } catch (Exception ignored) {
            // Em caso de incompatibilidade, apenas devolve campos que conseguiu mapear.
        }
        return ev;
    }

    // ===================== Utilidades =====================

    private static String mask(String dst) {
        if (dst == null || dst.isBlank()) return dst;
        String s = dst.trim();
        // e-mail
        int at = s.indexOf('@');
        if (at > 1) {
            String user = s.substring(0, at);
            String dom = s.substring(at);
            String head = user.length() <= 2 ? "*" : (user.substring(0, 2) + "***");
            return head + dom;
        }
        // telefone (mantém últimos 4)
        if (s.length() >= 4) {
            String last4 = s.substring(s.length() - 4);
            return "****" + last4;
        }
        return "***";
    }

    private static Object getField(Class<?> cls, Object obj, String name) {
        try {
            var f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(obj);
        } catch (NoSuchFieldException e) {
            // tenta getter padrão
            try {
                var m = cls.getMethod("get" + upper(name));
                return m.invoke(obj);
            } catch (Exception ignored) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(Class<?> c, Object o, String n) {
        Object v = getField(c, o, n);
        return (v == null) ? null : String.valueOf(v);
    }

    private static Long getLong(Class<?> c, Object o, String n) {
        Object v = getField(c, o, n);
        if (v instanceof Number num) return num.longValue();
        try { return (v == null) ? null : Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static Integer getInteger(Class<?> c, Object o, String n) {
        Object v = getField(c, o, n);
        if (v instanceof Number num) return num.intValue();
        try { return (v == null) ? null : Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    private static LocalDateTime getDateTime(Class<?> c, Object o, String n) {
        Object v = getField(c, o, n);
        return (v instanceof LocalDateTime ldt) ? ldt : null;
    }

    private static String upper(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public String toString() {
        return "NotificacaoEnviadaEvent{" +
                "eventId='" + eventId + '\'' +
                ", notificacaoId=" + notificacaoId +
                ", usuarioId=" + usuarioId +
                ", canal='" + canal + '\'' +
                ", status='" + status + '\'' +
                ", provider='" + provider + '\'' +
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
        if (!(o instanceof NotificacaoEnviadaEvent that)) return false;
        return Objects.equals(eventId, that.eventId);
    }
}
