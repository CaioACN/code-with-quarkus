package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

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
@Table(name = "notificacao", schema = "loyalty")
@Check(constraints = "canal IN ('EMAIL', 'PUSH', 'SMS', 'WEBHOOK') AND status IN ('AGENDADA', 'ENFILEIRADA', 'RETENTANDO', 'ENVIADA', 'FALHA', 'CANCELADA') AND tentativas >= 0 AND tipo IN ('ACUMULO', 'EXPIRACAO', 'RESGATE', 'SISTEMA', 'AJUSTE')")
public class Notificacao extends PanacheEntity {

    // ===================== Relacionamentos (opcionais) =====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_notificacao_usuario"))
    public Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", foreignKey = @ForeignKey(name = "fk_notificacao_cartao"))
    public Cartao cartao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transacao_id", foreignKey = @ForeignKey(name = "fk_notificacao_transacao"))
    public Transacao transacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resgate_id", foreignKey = @ForeignKey(name = "fk_notificacao_resgate"))
    public Resgate resgate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimento_id", foreignKey = @ForeignKey(name = "fk_notificacao_movimento"))
    public MovimentoPontos movimento;

    // ===================== Atributos principais =====================

    @NotNull(message = "Canal é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 16)
    public Canal canal; // EMAIL, PUSH, SMS, WEBHOOK

    @NotNull(message = "Tipo é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    public Tipo tipo;   // ACUMULO, EXPIRACAO, RESGATE, SISTEMA, AJUSTE

    @NotNull(message = "Status é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    public Status status = Status.AGENDADA;

    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    @Column(name = "titulo", length = 200)
    public String titulo;

    @Size(max = 4000, message = "Mensagem deve ter no máximo 4000 caracteres")
    @Column(name = "mensagem", length = 4000)
    public String mensagem;

    /** Destino do envio (e-mail/telefone/token/URL). Armazenar preferencialmente mascarado. */
    @NotBlank(message = "Destino é obrigatório")
    @Size(max = 320, message = "Destino deve ter no máximo 320 caracteres")
    @Column(name = "destino", nullable = false, length = 320)
    public String destino;

    // ===================== Provedor / rastreio =====================

    @Size(max = 60, message = "Provider deve ter no máximo 60 caracteres")
    @Column(name = "provider", length = 60)
    public String provider;

    @Size(max = 120, message = "Provider Message ID deve ter no máximo 120 caracteres")
    @Column(name = "provider_message_id", length = 120)
    public String providerMessageId;

    @Size(max = 180, message = "Erro mensagem deve ter no máximo 180 caracteres")
    @Column(name = "erro_mensagem", length = 180)
    public String erroMensagem;

    // ===================== Datas / Retentativas =====================

    @NotNull(message = "Data de criação é obrigatória")
    @Column(name = "criado_em", nullable = false)
    public LocalDateTime criadoEm = LocalDateTime.now();

    /** Quando enviar (agendamento). */
    @Column(name = "agendado_para")
    public LocalDateTime agendadoPara;

    /** Quando foi efetivamente enviado (status ENVIADA). */
    @Column(name = "enviado_em")
    public LocalDateTime enviadoEm;

    /** Controle de tentativas/retentativa. */
    @NotNull(message = "Tentativas é obrigatório")
    @Min(value = 0, message = "Tentativas deve ser maior ou igual a 0")
    @Column(name = "tentativas", nullable = false)
    public Integer tentativas = 0;

    @Column(name = "ultima_tentativa_em")
    public LocalDateTime ultimaTentativaEm;

    @Column(name = "proxima_tentativa_em")
    public LocalDateTime proximaTentativaEm;

    // ===================== Correlação / Multi-tenant / Template =====================

    @Size(max = 120, message = "Correlation ID deve ter no máximo 120 caracteres")
    @Column(name = "correlation_id", length = 120)
    public String correlationId;

    @Size(max = 60, message = "Tenant ID deve ter no máximo 60 caracteres")
    @Column(name = "tenant_id", length = 60)
    public String tenantId;

    @Size(max = 120, message = "Template deve ter no máximo 120 caracteres")
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
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (titulo != null) titulo = titulo.trim();
        if (mensagem != null) mensagem = mensagem.trim();
        if (destino != null) destino = destino.trim();
        if (provider != null) provider = provider.trim();
        if (providerMessageId != null) providerMessageId = providerMessageId.trim();
        if (erroMensagem != null) erroMensagem = erroMensagem.trim();
        if (correlationId != null) correlationId = correlationId.trim();
        if (tenantId != null) tenantId = tenantId.trim();
        if (template != null) template = template.trim();
        
        // Definir data de criação se não foi definida
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        
        // Definir tentativas se não foi definido
        if (tentativas == null) {
            tentativas = 0;
        }
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

    // ===================== Métodos de validação =====================
    
    /**
     * Valida se o canal é válido conforme DDL
     */
    public boolean temCanalValido() {
        return canal != null;
    }
    
    /**
     * Valida se o tipo é válido conforme DDL
     */
    public boolean temTipoValido() {
        return tipo != null;
    }
    
    /**
     * Valida se o status é válido conforme DDL
     */
    public boolean temStatusValido() {
        return status != null;
    }
    
    /**
     * Valida se as tentativas são válidas conforme DDL
     */
    public boolean temTentativasValidas() {
        return tentativas != null && tentativas >= 0;
    }

    // ===================== Enums (mantidos para compatibilidade) =====================

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
