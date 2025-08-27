package org.acme.loyalty.service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.ConfiguracaoNotificacaoDTO;
import org.acme.loyalty.dto.NotificacaoRequestDTO;
import org.acme.loyalty.dto.NotificacaoResponseDTO;
import org.acme.loyalty.entity.Notificacao;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.repository.NotificacaoRepository;
import org.acme.loyalty.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificacaoService {

    @Inject NotificacaoRepository notificacaoRepository;
    @Inject UsuarioRepository usuarioRepository;

    // -------------------- Listagem com filtros/paginação --------------------
    public List<NotificacaoResponseDTO> listarNotificacoes(Long usuarioId,
                                                           String tipo,
                                                           Boolean lida, // ignorado: entidade não tem esse campo
                                                           Integer pagina,
                                                           Integer tamanho) {
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        int pageIndex = (pagina == null ? 0 : Math.max(0, pagina - 1)); // 1-based -> 0-based
        int pageSize  = (tamanho == null || tamanho <= 0) ? 20 : tamanho;

        Notificacao.Tipo tipoEnum = parseTipo(tipo);

        PanacheQuery<Notificacao> pq = notificacaoRepository.queryByFiltros(
                usuarioId,
                null,      // canal
                null,      // status
                tipoEnum,  // tipo
                null,      // de
                null,      // até
                pageIndex, pageSize
        );

        return pq.list().stream().map(this::toNotificacaoResponseDTO).collect(Collectors.toList());
    }

    // -------------------- Marcações de leitura --------------------
    @Transactional
    public void marcarComoLida(Long notificacaoId, Long usuarioId) {
        Notificacao n = notificacaoRepository.findByIdOptional(notificacaoId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada: " + notificacaoId));

        if (n.usuario == null || !Objects.equals(n.usuario.id, usuarioId)) {
            throw new IllegalArgumentException("Notificação não pertence ao usuário");
        }

        String stamp = "\"readAt\":\"" + LocalDateTime.now() + "\"";
        n.metadataJson = mergeJsonMarker(n.metadataJson, stamp);
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        List<Notificacao> lista = notificacaoRepository.listByUsuarioId(usuarioId);
        String stamp = "\"readAt\":\"" + LocalDateTime.now() + "\"";
        for (Notificacao n : lista) {
            n.metadataJson = mergeJsonMarker(n.metadataJson, stamp);
        }
    }

    // -------------------- Exclusão / limpeza --------------------
    @Transactional
    public void deletarNotificacao(Long notificacaoId, Long usuarioId) {
        Notificacao n = notificacaoRepository.findByIdOptional(notificacaoId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada: " + notificacaoId));
        if (n.usuario == null || !Objects.equals(n.usuario.id, usuarioId)) {
            throw new IllegalArgumentException("Notificação não pertence ao usuário");
        }
        notificacaoRepository.deleteById(notificacaoId);
    }

    @Transactional
    public void limparNotificacoesAntigas(Long usuarioId, Integer dias) {
        int d = (dias == null || dias <= 0) ? 30 : dias;
        LocalDateTime limite = LocalDateTime.now().minusDays(d);
        notificacaoRepository.delete("usuario.id = ?1 and criadoEm < ?2", usuarioId, limite);
    }

    // -------------------- “Não lidas” (aproximação) --------------------
    public Long contarNotificacoesNaoLidas(Long usuarioId) {
        // Aproximação: conta ENVIADAS (não parseia metadataJson)
        return notificacaoRepository.count("usuario.id = ?1 and status = ?2",
                usuarioId, Notificacao.Status.ENVIADA);
    }

    // -------------------- Envio --------------------
    @Transactional
    public void enviarNotificacao(NotificacaoRequestDTO request) {
        validarNotificacao(request);

        Usuario usuario = null;
        if (request.usuarioId != null) {
            usuario = usuarioRepository.findByIdOptional(request.usuarioId)
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + request.usuarioId));
        }

        var rendered = request.renderContent();

        // EMAIL
        if (Boolean.TRUE.equals(request.viaEmail)) {
            String email = (request.email != null && !request.email.isBlank())
                    ? request.email
                    : (usuario != null ? usuario.email : null);
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("E-mail não informado para viaEmail=true");
            }
            Notificacao n = baseFromRequest(request, usuario,
                    Notificacao.Canal.EMAIL, email, rendered.assunto, rendered.mensagem);
            notificacaoRepository.persist(n);
        }

        // SMS
        if (Boolean.TRUE.equals(request.viaSms)) {
            if (request.telefoneE164 == null || request.telefoneE164.isBlank()) {
                throw new IllegalArgumentException("Telefone E.164 é obrigatório para viaSms=true");
            }
            Notificacao n = baseFromRequest(request, usuario,
                    Notificacao.Canal.SMS, request.telefoneE164, rendered.assunto, rendered.mensagem);
            notificacaoRepository.persist(n);
        }

        // PUSH
        if (Boolean.TRUE.equals(request.viaPush)) {
            if (request.deviceToken == null || request.deviceToken.isBlank()) {
                throw new IllegalArgumentException("Device token é obrigatório para viaPush=true");
            }
            Notificacao n = baseFromRequest(request, usuario,
                    Notificacao.Canal.PUSH, request.deviceToken, rendered.assunto, rendered.mensagem);
            notificacaoRepository.persist(n);
        }

        // TODO: enfileirar processamento e publicar evento quando efetivamente enviado
    }

    @Transactional
    public void enviarNotificacaoEmLote(List<NotificacaoRequestDTO> notificacoes) {
        if (notificacoes == null || notificacoes.isEmpty()) {
            throw new IllegalArgumentException("Lista de notificações não pode ser vazia");
        }
        for (NotificacaoRequestDTO r : notificacoes) {
            try {
                enviarNotificacao(r);
            } catch (Exception ignored) {
                // TODO: logar e continuar
            }
        }
    }

    // -------------------- Configurações (placeholder) --------------------
    public ConfiguracaoNotificacaoDTO consultarConfiguracaoUsuario(Long usuarioId) {
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        ConfiguracaoNotificacaoDTO cfg = new ConfiguracaoNotificacaoDTO();
        cfg.usuarioId = usuarioId;
        cfg.emailAtivo = true;
        cfg.pushAtivo  = true;
        cfg.smsAtivo   = false;
        // removido: cfg.inAppAtivo (campo não existe no DTO)
        return cfg;
    }

    @Transactional
    public void atualizarConfiguracaoUsuario(Long usuarioId, ConfiguracaoNotificacaoDTO config) {
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));
        // TODO: persistir preferências em entidade própria
    }

    // -------------------- Helpers --------------------
    private void validarNotificacao(NotificacaoRequestDTO r) {
        if (r == null) throw new IllegalArgumentException("Dados da notificação são obrigatórios");

        if (!Boolean.TRUE.equals(r.viaEmail) && !Boolean.TRUE.equals(r.viaSms) && !Boolean.TRUE.equals(r.viaPush)) {
            throw new IllegalArgumentException("Selecione pelo menos um canal (viaEmail/viaSms/viaPush)");
        }

        boolean semUsuario = (r.usuarioId == null);
        if (semUsuario) {
            boolean okEmail = Boolean.TRUE.equals(r.viaEmail) && r.email != null && !r.email.isBlank();
            boolean okSms   = Boolean.TRUE.equals(r.viaSms)   && r.telefoneE164 != null && !r.telefoneE164.isBlank();
            boolean okPush  = Boolean.TRUE.equals(r.viaPush)  && r.deviceToken != null && !r.deviceToken.isBlank();
            if (!(okEmail || okSms || okPush)) {
                throw new IllegalArgumentException("Informe contato compatível com os canais selecionados ou um usuarioId");
            }
        }

        if ((r.templateId == null || r.templateId.isBlank())
                && (r.mensagem == null || r.mensagem.isBlank())) {
            throw new IllegalArgumentException("Informe templateId ou mensagem");
        }

        r.ensureDefaults();
    }

    private Notificacao baseFromRequest(NotificacaoRequestDTO req,
                                        Usuario usuario,
                                        Notificacao.Canal canal,
                                        String destino,
                                        String assuntoRender,
                                        String mensagemRender) {

        Notificacao n = new Notificacao();
        n.usuario = usuario;
        n.canal = canal;
        n.tipo = mapEventoToTipo(req.evento);
        n.status = (req.enviarApos != null ? Notificacao.Status.AGENDADA : Notificacao.Status.ENFILEIRADA);
        n.titulo = assuntoRender;
        n.mensagem = mensagemRender;
        n.destino = destino;
        n.criadoEm = LocalDateTime.now();
        n.agendadoPara = req.enviarApos;
        n.correlationId = req.correlationId;
        n.template = req.templateId;
        n.metadataJson = buildMetadataJson(req);
        return n;
    }

    private Notificacao.Tipo mapEventoToTipo(NotificacaoRequestDTO.Evento ev) {
        if (ev == null) return Notificacao.Tipo.SISTEMA;
        return switch (ev) {
            case ACUMULO -> Notificacao.Tipo.ACUMULO;
            case EXPIRACAO -> Notificacao.Tipo.EXPIRACAO;
            case RESGATE -> Notificacao.Tipo.RESGATE;
            case AJUSTE -> Notificacao.Tipo.AJUSTE;
            case SISTEMA -> Notificacao.Tipo.SISTEMA;
        };
    }

    private Notificacao.Tipo parseTipo(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Notificacao.Tipo.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }

    private static String mergeJsonMarker(String current, String markerKV) {
        if (current == null || current.isBlank()) {
            return "{ " + markerKV + " }";
        }
        String trimmed = current.trim();
        if (trimmed.endsWith("}")) {
            String base = trimmed.substring(0, trimmed.length() - 1).trim();
            if (base.endsWith("{")) {
                return "{ " + markerKV + " }";
            }
            return base + ", " + markerKV + " }";
        }
        return current + " | " + markerKV;
    }

    private static String buildMetadataJson(NotificacaoRequestDTO r) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotNull(map, "variaveis", r.variaveis);
        putIfNotNull(map, "idioma", r.idioma);
        putIfNotNull(map, "ttlSegundos", r.ttlSegundos);
        putIfNotNull(map, "prioridade", (r.prioridade != null ? r.prioridade.name() : null));
        putIfNotNull(map, "dedupKey", r.dedupKey);
        putIfNotNull(map, "origem", r.origem);
        putIfNotNull(map, "metadata", r.metadata);
        putIfNotNull(map, "usuarioId", r.usuarioId);
        putIfNotNull(map, "cartaoId", r.cartaoId);
        putIfNotNull(map, "transacaoId", r.transacaoId);
        putIfNotNull(map, "resgateId", r.resgateId);

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(escape(String.valueOf(e.getKey()))).append("\":");
            sb.append(toJsonValue(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object val) {
        if (val != null) map.put(key, val);
    }

    private static String toJsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (var e : m.entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append("\"").append(escape(String.valueOf(e.getKey()))).append("\":");
                sb.append(toJsonValue(e.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (v instanceof Collection<?> c) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (var e : c) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(toJsonValue(e));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private NotificacaoResponseDTO toNotificacaoResponseDTO(Notificacao n) {
        NotificacaoResponseDTO dto = new NotificacaoResponseDTO();
        dto.id = n.id;
        dto.correlationId = n.correlationId;
        dto.usuarioId = (n.usuario != null ? n.usuario.id : null);
        dto.createdAt = n.criadoEm;
        dto.scheduledFor = n.agendadoPara;

        switch (n.status) {
            case AGENDADA   -> dto.status = NotificacaoResponseDTO.Status.SCHEDULED;
            case ENFILEIRADA, RETENTANDO -> dto.status = NotificacaoResponseDTO.Status.QUEUED;
            case ENVIADA    -> {
                dto.status = NotificacaoResponseDTO.Status.SENT;
                dto.sentAt = (n.enviadoEm != null ? n.enviadoEm : dto.sentAt);
            }
            case FALHA      -> {
                dto.status = NotificacaoResponseDTO.Status.FAILED;
                dto.failedAt = (n.ultimaTentativaEm != null ? n.ultimaTentativaEm : LocalDateTime.now());
            }
            case CANCELADA  -> dto.status = NotificacaoResponseDTO.Status.CANCELLED;
        }

        NotificacaoResponseDTO.CanalResultado canal = new NotificacaoResponseDTO.CanalResultado();
        canal.canal = switch (n.canal) {
            case EMAIL -> NotificacaoResponseDTO.CanalResultado.Canal.EMAIL;
            case SMS   -> NotificacaoResponseDTO.CanalResultado.Canal.SMS;
            case PUSH, WEBHOOK -> NotificacaoResponseDTO.CanalResultado.Canal.PUSH;
        };
        canal.status = switch (n.status) {
            case AGENDADA   -> NotificacaoResponseDTO.CanalResultado.CanalStatus.SCHEDULED;
            case ENFILEIRADA, RETENTANDO -> NotificacaoResponseDTO.CanalResultado.CanalStatus.QUEUED;
            case ENVIADA    -> NotificacaoResponseDTO.CanalResultado.CanalStatus.SENT;
            case FALHA      -> NotificacaoResponseDTO.CanalResultado.CanalStatus.FAILED;
            case CANCELADA  -> NotificacaoResponseDTO.CanalResultado.CanalStatus.CANCELLED;
        };
        canal.provider = n.provider;
        canal.providerMessageId = n.providerMessageId;
        canal.attempts = n.tentativas;
        canal.errorMessage = n.erroMensagem;
        canal.errorCode = null;
        canal.lastUpdateAt = (n.ultimaTentativaEm != null ? n.ultimaTentativaEm :
                              n.enviadoEm != null ? n.enviadoEm : n.criadoEm);

        dto.canais = List.of(canal);
        dto.recomputeTotals();
        return dto;
    }
}
