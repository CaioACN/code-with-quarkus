package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entidade de NOTIFICAÇÃO do sistema de Loyalty.
 * Suporta envio por múltiplos canais, agendamento, retentativas e rastreio de provider.
 *
 * Alinha-se aos DTOs/Events já definidos:
 * - NotificacaoRequestDTO / NotificacaoResponseDTO / NotificacaoUpdateDTO
 * - NotificacaoEnviadaEvent (status: ENVIADA/FALHA/RETENTANDO/AGENDADA)
 *
 * Observações:
 * - Campos de relacionamento são opcionais (dependem do contexto do envio).
 * - Para metadados livres, usar metadataJson (TEXT) serializado em JSON pela aplicação.
 */
@Entity
@Table(name = "notificacao")
public class Notificacao extends PanacheEntity {

    // ===================== Relacionamentos (opcionais) =====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    public Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id")
    public Cartao cartao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id")
    public Transacao transacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resgate_id")
    public Resgate resgate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimento_id")
    public MovimentoPontos movimento;

    // ===================== Atributos principais =====================

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 16)
    public Canal canal; // EMAIL, PUSH, SMS, WEBHOOK

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    public Tipo tipo;   // ACUMULO, EXPIRACAO, RESGATE, SISTEMA, AJUSTE

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    public Status status = Status.AGENDADA;

    @Size(max = 200)
    @Column(name = "titulo", length = 200)
    public String titulo;

    @Size(max = 4000)
    @Column(name = "mensagem", length = 4000)
    public String mensagem;

    /** Destino do envio (e-mail/telefone/token/URL). Armazenar preferencialmente mascarado. */
    @NotBlank
    @Size(max = 320)
    @Column(name = "destino", nullable = false, length = 320)
    public String destino;

    // ===================== Provedor / rastreio =====================

    @Size(max = 60)
    @Column(name = "provider", length = 60)
    public String provider;

    @Size(max = 120)
    @Column(name = "provider_message_id", length = 120)
    public String providerMessageId;

    @Size(max = 180)
    @Column(name = "erro_mensagem", length = 180)
    public String erroMensagem;

    // ===================== Datas / Retentativas =====================

    @NotNull
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm = LocalDateTime.now();

    /** Quando enviar (agendamento). */
    @Column(name = "agendado_para")
    public LocalDateTime agendadoPara;

    /** Quando foi efetivamente enviado (status ENVIADA). */
    @Column(name = "enviado_em")
    public LocalDateTime enviadoEm;

    /** Controle de tentativas/retentativa. */
    @NotNull
    @Min(0)
    @Column(name = "tentativas", nullable = false)
    public Integer tentativas = 0;

    @Column(name = "ultima_tentativa_em")
    public LocalDateTime ultimaTentativaEm;

    @Column(name = "proxima_tentativa_em")
    public LocalDateTime proximaTentativaEm;

    // ===================== Correlação / Multi-tenant / Template =====================

    @Size(max = 120)
    @Column(name = "correlation_id", length = 120)
    public String correlationId;

    @Size(max = 60)
    @Column(name = "tenant_id", length = 60)
    public String tenantId;

    @Size(max = 120)
    @Column(name = "template", length = 120)
    public String template;

    /** Metadados livres em JSON (serializado pela aplicação). */
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    public String metadataJson;

    // ===================== Construtores =====================

    public Notificacao() {}

    public Notificacao(Canal canal, Tipo tipo, String destino, String titulo, String mensagem) {
        this.canal = canal;
        this.tipo = tipo;
        this.destino = destino;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.status = Status.AGENDADA;
        this.criadoEm = LocalDateTime.now();
    }

    // ===================== Regras de negócio =====================

    /** Define status para ENFILEIRADA, pronta para processamento imediato. */
    public void enfileirar() {
        this.status = Status.ENFILEIRADA;
        this.proximaTentativaEm = LocalDateTime.now();
    }

    /** Registra envio com sucesso e preenche rastreio do provider. */
    public void registrarEnvioOk(String provider, String providerMessageId) {
        this.status = Status.ENVIADA;
        this.enviadoEm = LocalDateTime.now();
        this.ultimaTentativaEm = this.enviadoEm;
        this.provider = provider;
        this.providerMessageId = providerMessageId;
        this.erroMensagem = null;
    }

    /**
     * Registra falha e programa retentativa com backoff em segundos.
     * @param erro mensagem de erro
     * @param backoffSeconds segundos até a próxima tentativa (mínimo 5s)
     */
    public void registrarFalha(String erro, int backoffSeconds) {
        this.tentativas = (this.tentativas == null ? 0 : this.tentativas) + 1;
        this.status = Status.RETENTANDO;
        this.ultimaTentativaEm = LocalDateTime.now();
        this.erroMensagem = erro;
        int backoff = Math.max(5, backoffSeconds);
        this.proximaTentativaEm = this.ultimaTentativaEm.plusSeconds(backoff);
    }

    /** Marca falha definitiva (sem nova tentativa). */
    public void marcarFalhaFinal(String erro) {
        this.tentativas = (this.tentativas == null ? 0 : this.tentativas) + 1;
        this.status = Status.FALHA;
        this.ultimaTentativaEm = LocalDateTime.now();
        this.erroMensagem = erro;
        this.proximaTentativaEm = null;
    }

    /** Cancela o envio. */
    public void cancelar() {
        this.status = Status.CANCELADA;
        this.proximaTentativaEm = null;
    }

    /**
     * Indica se está pronta para ser enviada agora (considerando agendamento/retentativa).
     */
    public boolean prontaParaEnvio(LocalDateTime agora) {
        LocalDateTime ref = (agora == null ? LocalDateTime.now() : agora);
        boolean estadoOk = (status == Status.AGENDADA || status == Status.ENFILEIRADA || status == Status.RETENTANDO);
        boolean agendaOk = (agendadoPara == null || !ref.isBefore(agendadoPara));
        boolean retryOk  = (proximaTentativaEm == null || !ref.isBefore(proximaTentativaEm));
        return estadoOk && agendaOk && retryOk;
    }

    /** Retorna o destino mascarado (e-mail/telefone). */
    public String getDestinoMascarado() {
        if (destino == null || destino.isBlank()) return destino;
        String s = destino.trim();
        int at = s.indexOf('@');
        if (at > 1) { // e-mail
            String user = s.substring(0, at);
            String dom = s.substring(at);
            String head = user.length() <= 2 ? "*" : (user.substring(0, 2) + "***");
            return head + dom;
        }
        // telefone (mantém apenas últimos 4)
        if (s.length() >= 4) {
            return "****" + s.substring(s.length() - 4);
        }
        return "***";
    }

    // ===================== Enums =====================

    public enum Canal {
        EMAIL, PUSH, SMS, WEBHOOK
    }

    public enum Tipo {
        ACUMULO, EXPIRACAO, RESGATE, SISTEMA, AJUSTE
    }

    /**
     * Status do fluxo de envio.
     * Alinhado aos eventos/recursos: AGENDADA, ENFILEIRADA, RETENTANDO, ENVIADA, FALHA, CANCELADA.
     */
    public enum Status {
        AGENDADA,
        ENFILEIRADA,
        RETENTANDO,
        ENVIADA,
        FALHA,
        CANCELADA
    }
}
