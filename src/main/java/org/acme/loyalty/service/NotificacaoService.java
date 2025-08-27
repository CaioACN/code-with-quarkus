package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.NotificacaoRequestDTO;
import org.acme.loyalty.dto.NotificacaoResponseDTO;
import org.acme.loyalty.dto.NotificacaoUpdateDTO;
import org.acme.loyalty.dto.PageRequestDTO;
import org.acme.loyalty.dto.ConfiguracaoNotificacaoDTO;
import org.acme.loyalty.entity.Notificacao;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.repository.NotificacaoRepository;
import org.acme.loyalty.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class NotificacaoService {

    @Inject
    NotificacaoRepository notificacaoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    public List<NotificacaoResponseDTO> listarNotificacoes(Long usuarioId, String tipo, 
                                                         Boolean lida, Integer pagina, Integer tamanho) {
        
        // Validar se usuário existe
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // Construir filtros
        PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
        
        List<Notificacao> notificacoes = notificacaoRepository.findByUsuarioId(
            usuarioId, tipo, lida, paginacao.getOffset(), paginacao.getLimit()
        );

        return notificacoes.stream()
                .map(this::toNotificacaoResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void marcarComoLida(Long notificacaoId, Long usuarioId) {
        Notificacao notificacao = notificacaoRepository.findByIdOptional(notificacaoId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada: " + notificacaoId));

        // Validar se notificação pertence ao usuário
        if (!notificacao.usuario.id.equals(usuarioId)) {
            throw new IllegalArgumentException("Notificação não pertence ao usuário");
        }

        // Marcar como lida
        notificacao.lida = true;
        notificacao.lidaEm = LocalDateTime.now();
        notificacao.atualizadoEm = LocalDateTime.now();

        notificacaoRepository.persist(notificacao);
    }

    @Transactional
    public void marcarTodasComoLidas(Long usuarioId) {
        // Buscar todas as notificações não lidas do usuário
        List<Notificacao> notificacoes = notificacaoRepository.findNaoLidasByUsuarioId(usuarioId);

        // Marcar todas como lidas
        LocalDateTime agora = LocalDateTime.now();
        notificacoes.forEach(notificacao -> {
            notificacao.lida = true;
            notificacao.lidaEm = agora;
            notificacao.atualizadoEm = agora;
        });

        // Persistir alterações
        notificacaoRepository.persist(notificacoes);
    }

    @Transactional
    public void deletarNotificacao(Long notificacaoId, Long usuarioId) {
        Notificacao notificacao = notificacaoRepository.findByIdOptional(notificacaoId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada: " + notificacaoId));

        // Validar se notificação pertence ao usuário
        if (!notificacao.usuario.id.equals(usuarioId)) {
            throw new IllegalArgumentException("Notificação não pertence ao usuário");
        }

        notificacaoRepository.deleteById(notificacaoId);
    }

    @Transactional
    public void limparNotificacoesAntigas(Long usuarioId, Integer dias) {
        // Calcular data limite
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(dias);

        // Buscar notificações antigas
        List<Notificacao> notificacoesAntigas = notificacaoRepository.findAntigasByUsuarioId(usuarioId, dataLimite);

        // Deletar notificações antigas
        notificacaoRepository.deleteByIds(notificacoesAntigas.stream()
                .map(n -> n.id)
                .collect(Collectors.toList()));
    }

    public Long contarNotificacoesNaoLidas(Long usuarioId) {
        return notificacaoRepository.countNaoLidasByUsuarioId(usuarioId);
    }

    @Transactional
    public void enviarNotificacao(NotificacaoRequestDTO request) {
        // Validar dados da notificação
        validarNotificacao(request);

        // Buscar usuário
        Usuario usuario = usuarioRepository.findByIdOptional(request.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + request.usuarioId));

        // Verificar se usuário aceita este tipo de notificação
        if (!usuarioAceitaNotificacao(usuario, request.tipo)) {
            return; // Usuário não aceita este tipo de notificação
        }

        // Criar notificação
        Notificacao notificacao = new Notificacao();
        notificacao.usuario = usuario;
        notificacao.tipo = request.tipo;
        notificacao.titulo = request.titulo;
        notificacao.mensagem = request.mensagem;
        notificacao.dados = request.dados;
        notificacao.prioridade = request.prioridade;
        notificacao.lida = false;
        notificacao.criadoEm = LocalDateTime.now();
        notificacao.atualizadoEm = LocalDateTime.now();

        // Persistir notificação
        notificacaoRepository.persist(notificacao);

        // TODO: Enviar notificação pelos canais configurados
        // - E-mail
        // - Push notification
        // - SMS
        // - In-app notification

        // TODO: Publicar evento NotificacaoEnviada
        // eventPublisherService.publishEvent(new NotificacaoEnviadaEvent(notificacao.id, ...));
    }

    @Transactional
    public void enviarNotificacaoEmLote(List<NotificacaoRequestDTO> notificacoes) {
        // Validar lista de notificações
        if (notificacoes == null || notificacoes.isEmpty()) {
            throw new IllegalArgumentException("Lista de notificações não pode ser vazia");
        }

        // Processar cada notificação
        for (NotificacaoRequestDTO request : notificacoes) {
            try {
                enviarNotificacao(request);
            } catch (Exception e) {
                // TODO: Registrar erro e continuar com as próximas
                // logger.error("Erro ao enviar notificação: " + e.getMessage(), e);
            }
        }
    }

    public ConfiguracaoNotificacaoDTO consultarConfiguracaoUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // TODO: Implementar consulta de configurações de notificação
        // - Tipos aceitos
        // - Canais preferidos
        // - Horários permitidos
        // - Frequência máxima

        ConfiguracaoNotificacaoDTO config = new ConfiguracaoNotificacaoDTO();
        config.usuarioId = usuarioId;
        config.emailAtivo = true;
        config.pushAtivo = true;
        config.smsAtivo = false;
        config.inAppAtivo = true;

        return config;
    }

    @Transactional
    public void atualizarConfiguracaoUsuario(Long usuarioId, ConfiguracaoNotificacaoDTO config) {
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // TODO: Implementar atualização de configurações
        // - Validar configurações
        // - Persistir alterações
        // - Aplicar mudanças em tempo real
    }

    private void validarNotificacao(NotificacaoRequestDTO request) {
        if (request.usuarioId == null) {
            throw new IllegalArgumentException("ID do usuário é obrigatório");
        }

        if (request.tipo == null || request.tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo da notificação é obrigatório");
        }

        if (request.titulo == null || request.titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("Título da notificação é obrigatório");
        }

        if (request.mensagem == null || request.mensagem.trim().isEmpty()) {
            throw new IllegalArgumentException("Mensagem da notificação é obrigatória");
        }

        if (request.prioridade == null) {
            request.prioridade = "NORMAL"; // Prioridade padrão
        }
    }

    private boolean usuarioAceitaNotificacao(Usuario usuario, String tipo) {
        // TODO: Implementar verificação de preferências do usuário
        // - Consultar configurações
        // - Verificar se tipo está habilitado
        // - Verificar se não está em blacklist
        // - Verificar horários permitidos

        // Por enquanto, aceita todas as notificações
        return true;
    }

    private NotificacaoResponseDTO toNotificacaoResponseDTO(Notificacao notificacao) {
        return new NotificacaoResponseDTO(
            notificacao.id,
            notificacao.usuario.id,
            notificacao.tipo,
            notificacao.titulo,
            notificacao.mensagem,
            notificacao.dados,
            notificacao.prioridade,
            notificacao.lida,
            notificacao.criadoEm,
            notificacao.lidaEm,
            notificacao.atualizadoEm
        );
    }
}

