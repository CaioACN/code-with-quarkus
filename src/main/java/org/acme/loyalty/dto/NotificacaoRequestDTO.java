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
 * Requisição para envio de notificação (e-mail, SMS e/ou push) do sistema de pontos.
 * Compatível com Cursor/Quarkus (Java 17, Jakarta Validation, MicroProfile OpenAPI).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "NotificacaoRequest", description = "Parâmetros para envio de notificações do programa de pontos")
public class NotificacaoRequestDTO {

    // ===================== Contexto do domínio =====================

    @Schema(description = "ID do usuário destinatário (se informado, contato pode ser resolvido pelo serviço)", example = "10")
    public Long usuarioId;

    @Schema(description = "ID do cartão relacionado (opcional)", example = "1")
    public Long cartaoId;

    @Schema(description = "ID da transação relacionada (opcional)", example = "123")
    public Long transacaoId;

    @Schema(description = "ID do resgate relacionado (opcional)", example = "555")
    public Long resgateId;

    @NotNull
    @Schema(description = "Tipo do evento que motiva a notificação", required = true,
            enumeration = {"ACUMULO","EXPIRACAO","RESGATE","AJUSTE","SISTEMA"})
    public Evento evento = Evento.SISTEMA;

    public enum Evento { ACUMULO, EXPIRACAO, RESGATE, AJUSTE, SISTEMA }

    // ===================== Canais e contato =====================

    @NotNull
    @Schema(description = "Enviar por e-mail?", example = "true", required = true)
    public Boolean viaEmail = Boolean.TRUE;

    @NotNull
    @Schema(description = "Enviar por SMS?", example = "false", required = true)
    public Boolean viaSms = Boolean.FALSE;

    @NotNull
    @Schema(description = "Enviar por push (mobile/web)?", example = "false", required = true)
    public Boolean viaPush = Boolean.FALSE;

    @Email
    @Size(max = 200)
    @Schema(description = "E-mail do destinatário (obrigatório se viaEmail=true e usuarioId não informado)", example = "cliente@exemplo.com")
    public String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Telefone deve estar em E.164 (ex.: +5511999998888)")
    @Schema(description = "Telefone E.164 (obrigatório se viaSms=true e usuarioId não informado)", example = "+5511999998888")
    public String telefoneE164;

    @Size(max = 255)
    @Schema(description = "Device token / registrationId para push (obrigatório se viaPush=true e usuarioId não informado)")
    public String deviceToken;

    // ===================== Conteúdo =====================

    @Size(max = 120)
    @Schema(description = "Identificador do template cadastrado (se fornecido, assunto/mensagem podem ser gerados a partir dele)", example = "TPL_POINTS_ACCUMULATED")
    public String templateId;

    @Size(max = 200)
    @Schema(description = "Assunto (e-mail) – opcional quando usa template", example = "Você ganhou pontos!")
    public String assunto;

    @Size(max = 4000)
    @Schema(description = "Mensagem (texto) – pode conter placeholders do mapa 'variaveis'", example = "Olá, ${nome}. Você acumulou ${pontos} pontos.")
    public String mensagem;

    @Schema(description = "Variáveis para template/placeholders (ex.: nome, pontos, data, etc.)")
    public Map<String, String> variaveis;

    @Size(min = 2, max = 10)
    @Schema(description = "Idioma/locale preferido para a mensagem", example = "pt-BR")
    public String idioma;

    // ===================== Entrega/roteamento =====================

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Agendar envio para depois de (opcional)", example = "2025-08-27T09:30:00")
    public LocalDateTime enviarApos;

    @Min(0)
    @Schema(description = "Tempo de vida da mensagem em segundos (TTL) para filas/retentativas", example = "3600")
    public Integer ttlSegundos;

    @NotNull
    @Schema(description = "Prioridade de envio", required = true, enumeration = {"LOW","NORMAL","HIGH"})
    public Prioridade prioridade = Prioridade.NORMAL;

    public enum Prioridade { LOW, NORMAL, HIGH }

    @Size(max = 120)
    @Schema(description = "Chave de deduplicação/idempotência (evita mensagens duplicadas)", example = "notif-usuario10-20250827-accum-123")
    public String dedupKey;

    @Size(max = 120)
    @Schema(description = "Correlation ID para rastreio distribuído", example = "corr-5f84c9b2")
    public String correlationId;

    @Size(max = 60)
    @Schema(description = "Origem do envio (serviço/worker)", example = "loyalty-notifier")
    public String origem;

    @Schema(description = "Metadados adicionais para roteamento/auditoria")
    public Map<String, Object> metadata;

    // ===================== Validações de regra =====================

    @AssertTrue(message = "Pelo menos um canal deve estar ativo (viaEmail, viaSms ou viaPush)")
    public boolean isAlgumCanalSelecionado() {
        return Boolean.TRUE.equals(viaEmail) || Boolean.TRUE.equals(viaSms) || Boolean.TRUE.equals(viaPush);
    }

    @AssertTrue(message = "Informe usuarioId OU dados de contato compatíveis com os canais selecionados")
    public boolean isDestinoValido() {
        if (usuarioId != null) return true;
        boolean okEmail = Boolean.TRUE.equals(viaEmail) && email != null && !email.isBlank();
        boolean okSms   = Boolean.TRUE.equals(viaSms)   && telefoneE164 != null && !telefoneE164.isBlank();
        boolean okPush  = Boolean.TRUE.equals(viaPush)  && deviceToken != null && !deviceToken.isBlank();
        return okEmail || okSms || okPush;
    }

    // ===================== Conveniências =====================

    /** Define default de idioma se não informado (herda do Locale da JVM). */
    public void ensureDefaults() {
        if (this.idioma == null || this.idioma.isBlank()) {
            Locale l = Locale.getDefault();
            this.idioma = l.toLanguageTag(); // ex.: pt-BR
        }
        if (this.variaveis == null) this.variaveis = new LinkedHashMap<>();
    }

    /** Renderiza uma string substituindo placeholders ${chave} pelas variáveis informadas. */
    public String render(String text) {
        if (text == null || text.isBlank() || variaveis == null || variaveis.isEmpty()) return text;
        String out = text;
        for (Map.Entry<String, String> e : variaveis.entrySet()) {
            String key = Objects.toString(e.getKey(), "");
            String val = Objects.toString(e.getValue(), "");
            out = out.replace("${" + key + "}", val);
        }
        return out;
    }

    /** Retorna assunto/mensagem já com variáveis aplicadas (não altera os campos originais). */
    @Schema(hidden = true)
    public RenderedContent renderContent() {
        ensureDefaults();
        return new RenderedContent(render(this.assunto), render(this.mensagem));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RenderedContent {
        public final String assunto;
        public final String mensagem;
        public RenderedContent(String assunto, String mensagem) {
            this.assunto = assunto;
            this.mensagem = mensagem;
        }
    }
}
