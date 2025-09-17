package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalTime;

/**
 * Entidade de CONFIGURAÇÃO DE NOTIFICAÇÃO do sistema de Loyalty.
 * Armazena as preferências de notificação de cada usuário.
 */
@Entity
@Table(name = "configuracao_notificacao", schema = "loyalty")
public class ConfiguracaoNotificacao extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_config_notif_usuario"), nullable = false)
    public Usuario usuario;

    // Canais
    @NotNull
    @Column(name = "email_ativo", nullable = false)
    public Boolean emailAtivo = true;

    @NotNull
    @Column(name = "sms_ativo", nullable = false)
    public Boolean smsAtivo = false;

    @NotNull
    @Column(name = "push_ativo", nullable = false)
    public Boolean pushAtivo = true;

    // Eventos
    @NotNull
    @Column(name = "notificar_acumulo", nullable = false)
    public Boolean notificarAcumulo = true;

    @NotNull
    @Column(name = "notificar_expiracao", nullable = false)
    public Boolean notificarExpiracao = true;

    @NotNull
    @Column(name = "notificar_resgate", nullable = false)
    public Boolean notificarResgate = true;

    @Column(name = "notificar_campanha")
    public Boolean notificarCampanha = false;

    // Regras extras
    @Min(0)
    @Column(name = "limite_minimo_pontos_notificar")
    public Integer limiteMinimoPontosNotificar = 0;

    @Size(min = 2, max = 10)
    @Column(name = "idioma_preferido", length = 10)
    public String idiomaPreferido = "pt-BR";

    @Size(min = 1, max = 60)
    @Pattern(regexp = "^[A-Za-z_]+(?:/[A-Za-z_]+)*$", message = "Timezone IANA inválido (ex.: America/Sao_Paulo)")
    @Column(name = "timezone", length = 60)
    public String timezone = "America/Sao_Paulo";

    // Janela de silêncio (quiet hours)
    @Column(name = "silencio_inicio")
    public LocalTime silencioInicio;

    @Column(name = "silencio_fim")
    public LocalTime silencioFim;

    // Digest/Resumo
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "digest", nullable = false)
    public DigestFrequency digest = DigestFrequency.OFF;

    public enum DigestFrequency {
        OFF, DAILY, WEEKLY
    }

    public ConfiguracaoNotificacao() {}

    public ConfiguracaoNotificacao(Usuario usuario) {
        this.usuario = usuario;
    }

    // Métodos de conveniência
    public boolean algumCanalAtivo() {
        return Boolean.TRUE.equals(emailAtivo) || Boolean.TRUE.equals(smsAtivo) || Boolean.TRUE.equals(pushAtivo);
    }

    public boolean algumEventoAtivo() {
        return Boolean.TRUE.equals(notificarAcumulo) ||
               Boolean.TRUE.equals(notificarExpiracao) ||
               Boolean.TRUE.equals(notificarResgate) ||
               Boolean.TRUE.equals(notificarCampanha);
    }
}