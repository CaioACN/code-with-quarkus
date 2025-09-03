package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * DTO para atualização/patch de uma notificação existente.
 * Use-o para reagendar, cancelar, alterar conteúdo/canais ou corrigir metadados.
 *
 * Todos os campos são opcionais (PATCH). Pelo menos um identificador deve ser fornecido:
 *  id OU correlationId OU dedupKey.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "NotificacaoUpdate", description = "Atualização parcial (PATCH) de notificações")
public class NotificacaoUpdateDTO {

    // ===================== Identificação do alvo =====================

    @Schema(description = "ID interno da notificação (quando conhecido)", example = "987654321")
    public Long id;

    @Size(max = 120)
    @Schema(description = "Correlation ID do envio original", example = "corr-5f84c9b2")
    public String correlationId;

    @Size(max = 120)
    @Schema(description = "Chave de idempotência/deduplicação do envio original", example = "notif-usuario10-20250827-accum-123")
    public String dedupKey;

    // (Opcional) permitir retarget
    @Schema(description = "Alterar/definir o usuário destino (opcional)", example = "10")
    public Long usuarioId;

    // ===================== Ações de agendamento/controle =====================

    @Schema(description = "Cancelar o envio (se ainda não enviado/entregue)", example = "false")
    public Boolean cancelar;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Reprogramar/agendar para este instante (sobrepõe agendamento anterior)", example = "2025-08-27T09:30:00")
    public LocalDateTime reprogramarPara;

    @Schema(description = "Forçar envio imediato (remove agendamento)", example = "false")
    public Boolean enviarAgora;

    @Min(0)
    @Schema(description = "TTL (segundos) para filas/retentativas", example = "3600")
    public Integer ttlSegundos;

    @Schema(description = "Prioridade do envio", enumeration = {"LOW","NORMAL","HIGH"})
    public NotificacaoRequestDTO.Prioridade prioridade;

    @Size(max = 200)
    @Schema(description = "Motivo do cancelamento ou observação operacional", example = "Solicitado pelo usuário")
    public String observacao;

    // ===================== Canais e contato (alterações) =====================

    @Schema(description = "Ativar/desativar envio por e-mail")
    public Boolean viaEmail;

    @Schema(description = "Ativar/desativar envio por SMS")
    public Boolean viaSms;

    @Schema(description = "Ativar/desativar envio por push")
    public Boolean viaPush;

    @Email
    @Size(max = 200)
    @Schema(description = "Atualizar e-mail do destinatário", example = "cliente@exemplo.com")
    public String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Telefone deve estar em E.164 (ex.: +5511999998888)")
    @Schema(description = "Atualizar telefone E.164", example = "+5511999998888")
    public String telefoneE164;

    @Size(max = 255)
    @Schema(description = "Atualizar device token para push")
    public String deviceToken;

    // ===================== Conteúdo (atualizações) =====================

    @Size(max = 120)
    @Schema(description = "Trocar o template utilizado", example = "TPL_POINTS_ACCUMULATED_V2")
    public String templateId;

    @Size(max = 200)
    @Schema(description = "Atualizar assunto (e-mail)")
    public String assunto;

    @Size(max = 4000)
    @Schema(description = "Atualizar mensagem (texto)")
    public String mensagem;

    @Schema(description = "Atualizar/adicionar variáveis do template")
    public Map<String, String> variaveis;

    @Size(min = 2, max = 10)
    @Schema(description = "Idioma/locale preferido", example = "pt-BR")
    public String idioma;

    // ===================== Metadados e provedores =====================

    @Schema(description = "Metadados adicionais para auditoria/roteamento (merge)")
    public Map<String, Object> metadata;

    @Schema(description = "Atualizações por canal (status/IDs do provedor)")
    public CanalUpdate canalUpdate;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Atualização específica por canal (opcional)")
    public static class CanalUpdate {
        @Schema(description = "Canal alvo", enumeration = {"EMAIL","SMS","PUSH"}, example = "EMAIL")
        public NotificacaoResponseDTO.CanalResultado.Canal canal;

        @Schema(description = "Status forçado/ajustado no canal",
                enumeration = {"QUEUED","SCHEDULED","SENT","DELIVERED","FAILED","CANCELLED"},
                example = "SCHEDULED")
        public NotificacaoResponseDTO.CanalResultado.CanalStatus status;

        @Size(max = 60)
        @Schema(description = "Provedor/transport (ex.: ses, sns, firebase, twilio)", example = "ses")
        public String provider;

        @Size(max = 180)
        @Schema(description = "ID da mensagem no provedor", example = "0102018f1fEXAMPLE")
        public String providerMessageId;

        @Schema(description = "Sobrescrever contagem de tentativas (se aplicável)", example = "1")
        public Integer attempts;

        @Size(max = 60)
        @Schema(description = "Código de erro do provedor (se falha)", example = "ThrottlingException")
        public String errorCode;

        @Size(max = 400)
        @Schema(description = "Mensagem de erro do provedor (se falha)")
        public String errorMessage;
    }

    // ===================== Validações de regra =====================

    @AssertTrue(message = "Forneça id, correlationId ou dedupKey para identificar a notificação")
    public boolean hasIdentity() {
        return (id != null)
                || (correlationId != null && !correlationId.isBlank())
                || (dedupKey != null && !dedupKey.isBlank());
    }

    @AssertTrue(message = "Não é possível cancelar e enviarAgora/reprogramar simultaneamente")
    public boolean isValidControlCombination() {
        boolean cancel = Boolean.TRUE.equals(cancelar);
        boolean now = Boolean.TRUE.equals(enviarAgora);
        boolean reschedule = reprogramarPara != null;
        if (!cancel) return true;
        return !(now || reschedule);
    }

    // ===================== Conveniências =====================

    /** Garante mapa de variáveis/metadata e idioma default sem alterar valores já setados. */
    public void ensureDefaults() {
        if (this.variaveis == null) this.variaveis = new LinkedHashMap<>();
        if (this.metadata == null) this.metadata = new LinkedHashMap<>();
        if (this.idioma == null || this.idioma.isBlank()) {
            this.idioma = Locale.getDefault().toLanguageTag();
        }
    }

    /** Aplica placeholders no texto informado com as variáveis atuais (não altera o original). */
    public String render(String text) {
        if (text == null || text.isBlank() || variaveis == null || variaveis.isEmpty()) return text;
        String out = text;
        for (Map.Entry<String, String> e : variaveis.entrySet()) {
            String k = Objects.toString(e.getKey(), "");
            String v = Objects.toString(e.getValue(), "");
            out = out.replace("${" + k + "}", v);
        }
        return out;
    }
}
