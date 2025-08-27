package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ConfiguracaoNotificacao", description = "Preferências de notificações do usuário para o programa de pontos")
public class ConfiguracaoNotificacaoDTO {

    @Schema(description = "ID do usuário dono das preferências", example = "10")
    public Long usuarioId;

    // Canais
    @NotNull
    @Schema(description = "Ativa envio por e-mail", example = "true", required = true)
    public Boolean emailAtivo;

    @NotNull
    @Schema(description = "Ativa envio por SMS", example = "false", required = true)
    public Boolean smsAtivo;

    @NotNull
    @Schema(description = "Ativa envio por push (app)", example = "true", required = true)
    public Boolean pushAtivo;

    // Eventos
    @NotNull
    @Schema(description = "Notificar em acúmulo de pontos", example = "true", required = true)
    public Boolean notificarAcumulo;

    @NotNull
    @Schema(description = "Notificar em expiração de pontos", example = "true", required = true)
    public Boolean notificarExpiracao;

    @NotNull
    @Schema(description = "Notificar em resgate de pontos", example = "true", required = true)
    public Boolean notificarResgate;

    @Schema(description = "Notificar sobre campanhas/booster", example = "false")
    public Boolean notificarCampanha;

    // Regras extras
    @Min(0)
    @Schema(description = "Valor mínimo (em pontos) para enviar notificações de acúmulo/resgate", example = "50")
    public Integer limiteMinimoPontosNotificar;

    @Size(min = 2, max = 10)
    @Schema(description = "Idioma preferido para mensagens", example = "pt-BR")
    public String idiomaPreferido;

    @Size(min = 1, max = 60)
    @Pattern(regexp = "^[A-Za-z_]+(?:/[A-Za-z_]+)*$", message = "Timezone IANA inválido (ex.: America/Sao_Paulo)")
    @Schema(description = "Timezone IANA para janelas de silêncio e agendamentos", example = "America/Sao_Paulo")
    public String timezone;

    // Janela de silêncio (quiet hours)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Schema(description = "Início da janela de silêncio (sem envio de notificações)", example = "22:00")
    public LocalTime silencioInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @Schema(description = "Fim da janela de silêncio (sem envio de notificações)", example = "07:00")
    public LocalTime silencioFim;

    // Digest/Resumo
    @NotNull
    @Schema(description = "Frequência de resumo/digest em vez de eventos individuais", example = "OFF", required = true,
            enumeration = {"OFF", "DAILY", "WEEKLY"})
    public DigestFrequency digest = DigestFrequency.OFF;

    public enum DigestFrequency { OFF, DAILY, WEEKLY }

    public ConfiguracaoNotificacaoDTO() {}

    // ---------- Conveniências ----------

    @Schema(hidden = true)
    public boolean algumCanalAtivo() {
        return Boolean.TRUE.equals(emailAtivo) || Boolean.TRUE.equals(smsAtivo) || Boolean.TRUE.equals(pushAtivo);
    }

    @Schema(hidden = true)
    public boolean algumEventoAtivo() {
        return Boolean.TRUE.equals(notificarAcumulo) ||
               Boolean.TRUE.equals(notificarExpiracao) ||
               Boolean.TRUE.equals(notificarResgate) ||
               Boolean.TRUE.equals(notificarCampanha);
    }

    // ---------- Validações de regra de negócio ----------

    @AssertTrue(message = "Pelo menos um canal (e-mail, SMS ou push) deve estar ativo quando o digest estiver OFF")
    public boolean isCanalObrigatorioQuandoSemDigest() {
        if (digest != DigestFrequency.OFF) return true; // quando usa digest, pode não ter canais de evento imediato
        return algumCanalAtivo();
    }

    @AssertTrue(message = "Pelo menos um tipo de evento deve estar habilitado")
    public boolean isAlgumEventoSelecionado() {
        return algumEventoAtivo();
    }

    @AssertTrue(message = "Se informadas, as horas de silêncio não podem ser iguais")
    public boolean isJanelaSilencioValida() {
        if (silencioInicio == null || silencioFim == null) return true;
        // Janela pode cruzar meia-noite; só impedimos iguais (daria janela vazia/24h bloqueada)
        return !silencioInicio.equals(silencioFim);
    }
}
