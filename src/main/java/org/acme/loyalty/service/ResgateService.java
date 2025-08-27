// package org.acme.loyalty.service;

// import jakarta.enterprise.context.ApplicationScoped;
// import jakarta.inject.Inject;
// import jakarta.transaction.Transactional;
// import jakarta.ws.rs.NotFoundException;
// import org.acme.loyalty.dto.ResgateRequestDTO;
// import org.acme.loyalty.dto.ResgateResponseDTO;
// import org.acme.loyalty.dto.ResgateUpdateDTO;
// import org.acme.loyalty.dto.PageRequestDTO;
// import org.acme.loyalty.entity.Resgate;
// import org.acme.loyalty.entity.Usuario;
// import org.acme.loyalty.entity.Cartao;
// import org.acme.loyalty.entity.Recompensa;
// import org.acme.loyalty.entity.SaldoPontos;
// import org.acme.loyalty.repository.ResgateRepository;
// import org.acme.loyalty.repository.UsuarioRepository;
// import org.acme.loyalty.repository.CartaoRepository;
// import org.acme.loyalty.repository.RecompensaRepository;
// import org.acme.loyalty.repository.SaldoPontosRepository;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.stream.Collectors;

// @ApplicationScoped
// public class ResgateService {

//     @Inject
//     ResgateRepository resgateRepository;

//     @Inject
//     UsuarioRepository usuarioRepository;

//     @Inject
//     CartaoRepository cartaoRepository;

//     @Inject
//     RecompensaRepository recompensaRepository;

//     @Inject
//     SaldoPontosRepository saldoPontosRepository;

//     @Transactional
//     public ResgateResponseDTO solicitarResgate(ResgateRequestDTO request) {
//         // Validar dados do resgate
//         validarResgate(request);

//         // Buscar entidades relacionadas
//         Usuario usuario = usuarioRepository.findByIdOptional(request.usuarioId)
//                 .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + request.usuarioId));

//         Cartao cartao = cartaoRepository.findByIdOptional(request.cartaoId)
//                 .orElseThrow(() -> new NotFoundException("Cartão não encontrado: " + request.cartaoId));

//         Recompensa recompensa = recompensaRepository.findByIdOptional(request.recompensaId)
//                 .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + request.recompensaId));

//         // Validar se cartão pertence ao usuário
//         if (!cartao.usuario.id.equals(request.usuarioId)) {
//             throw new IllegalArgumentException("Cartão não pertence ao usuário informado");
//         }

//         // Validar se recompensa está ativa e disponível
//         if (!recompensa.ativo) {
//             throw new IllegalArgumentException("Recompensa não está ativa");
//         }

//         if (recompensa.estoque <= 0) {
//             throw new IllegalArgumentException("Recompensa sem estoque disponível");
//         }

//         // Verificar saldo de pontos
//         SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(request.usuarioId, request.cartaoId)
//                 .orElseThrow(() -> new NotFoundException("Saldo de pontos não encontrado"));

//         if (saldo.saldo < recompensa.custoPontos) {
//             throw new IllegalArgumentException("Saldo insuficiente para resgate");
//         }

//         // Criar resgate
//         Resgate resgate = new Resgate();
//         resgate.usuario = usuario;
//         resgate.cartao = cartao;
//         resgate.recompensa = recompensa;
//         resgate.pontosUtilizados = recompensa.custoPontos;
//         resgate.status = "PENDENTE";
//         resgate.criadoEm = LocalDateTime.now();
//         resgate.atualizadoEm = LocalDateTime.now();

//         // Persistir resgate
//         resgateRepository.persist(resgate);

//         // TODO: Publicar evento ResgateRequested
//         // eventPublisherService.publishEvent(new ResgateRequestedEvent(resgate.id, ...));

//         return toResgateResponseDTO(resgate);
//     }

//     public ResgateResponseDTO buscarResgatePorId(Long id) {
//         Resgate resgate = resgateRepository.findByIdOptional(id)
//                 .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

//         return toResgateResponseDTO(resgate);
//     }

//     public List<ResgateResponseDTO> listarResgates(String status, Long usuarioId, Long cartaoId,
//                                                   Long recompensaId, String dataInicio, String dataFim,
//                                                   Integer pagina, Integer tamanho) {
        
//         // Construir filtros
//         PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
        
//         List<Resgate> resgates = resgateRepository.findByFiltros(
//             status, usuarioId, cartaoId, recompensaId, dataInicio, dataFim,
//             paginacao.getOffset(), paginacao.getLimit()
//         );

//         return resgates.stream()
//                 .map(this::toResgateResponseDTO)
//                 .collect(Collectors.toList());
//     }

//     @Transactional
//     public ResgateResponseDTO aprovarResgate(Long id, String observacao) {
//         Resgate resgate = resgateRepository.findByIdOptional(id)
//                 .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

//         if (!"PENDENTE".equals(resgate.status)) {
//             throw new IllegalStateException("Resgate deve estar pendente para ser aprovado");
//         }

//         // Aprovar resgate
//         resgate.status = "APROVADO";
//         resgate.observacao = observacao;
//         resgate.aprovadoEm = LocalDateTime.now();
//         resgate.atualizadoEm = LocalDateTime.now();

//         // Persistir alterações
//         resgateRepository.persist(resgate);

//         // TODO: Publicar evento ResgateApproved
//         // eventPublisherService.publishEvent(new ResgateApprovedEvent(resgate.id, ...));

//         return toResgateResponseDTO(resgate);
//     }

//     @Transactional
//     public ResgateResponseDTO concluirResgate(Long id, String observacao) {
//         Resgate resgate = resgateRepository.findByIdOptional(id)
//                 .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

//         if (!"APROVADO".equals(resgate.status)) {
//             throw new IllegalStateException("Resgate deve estar aprovado para ser concluído");
//         }

//         // Concluir resgate
//         resgate.status = "CONCLUIDO";
//         resgate.observacao = observacao;
//         resgate.concluidoEm = LocalDateTime.now();
//         resgate.atualizadoEm = LocalDateTime.now();

//         // Baixar pontos do saldo
//         baixarPontosResgate(resgate);

//         // Atualizar estoque da recompensa
//         atualizarEstoqueRecompensa(resgate.recompensa);

//         // Persistir alterações
//         resgateRepository.persist(resgate);

//         // TODO: Publicar evento ResgateCompleted
//         // eventPublisherService.publishEvent(new ResgateCompletedEvent(resgate.id, ...));

//         return toResgateResponseDTO(resgate);
//     }

//     @Transactional
//     public ResgateResponseDTO negarResgate(Long id, String motivo) {
//         Resgate resgate = resgateRepository.findByIdOptional(id)
//                 .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

//         if (!"PENDENTE".equals(resgate.status)) {
//             throw new IllegalStateException("Resgate deve estar pendente para ser negado");
//         }

//         // Negar resgate
//         resgate.status = "NEGADO";
//         resgate.observacao = motivo;
//         resgate.negadoEm = LocalDateTime.now();
//         resgate.atualizadoEm = LocalDateTime.now();

//         // Persistir alterações
//         resgateRepository.persist(resgate);

//         // TODO: Publicar evento ResgateDenied
//         // eventPublisherService.publishEvent(new ResgateDeniedEvent(resgate.id, ...));

//         return toResgateResponseDTO(resgate);
//     }

//     @Transactional
//     public ResgateResponseDTO cancelarResgate(Long id, String motivo) {
//         Resgate resgate = resgateRepository.findByIdOptional(id)
//                 .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

//         if (!"PENDENTE".equals(resgate.status)) {
//             throw new IllegalStateException("Resgate deve estar pendente para ser cancelado");
//         }

//         // Cancelar resgate
//         resgate.status = "CANCELADO";
//         resgate.observacao = motivo;
//         resgate.canceladoEm = LocalDateTime.now();
//         resgate.atualizadoEm = LocalDateTime.now();

//         // Persistir alterações
//         resgateRepository.persist(resgate);

//         // TODO: Publicar evento ResgateCancelled
//         // eventPublisherService.publishEvent(new ResgateCancelledEvent(resgate.id, ...));

//         return toResgateResponseDTO(resgate);
//     }

//     public List<ResgateResponseDTO> listarResgatesUsuario(Long usuarioId, String status) {
//         List<Resgate> resgates = resgateRepository.findByUsuarioId(usuarioId, status);

//         return resgates.stream()
//                 .map(this::toResgateResponseDTO)
//                 .collect(Collectors.toList());
//     }

//     public Object acompanharResgate(Long id) {
//         Resgate resgate = resgateRepository.findByIdOptional(id)
//                 .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

//         // TODO: Implementar acompanhamento do resgate
//         // - Histórico de mudanças de status
//         // - Tempo em cada status
//         // - Notificações enviadas
//         // - Logs de auditoria

//         return null;
//     }

//     private void validarResgate(ResgateRequestDTO request) {
//         if (request.usuarioId == null) {
//             throw new IllegalArgumentException("ID do usuário é obrigatório");
//         }

//         if (request.cartaoId == null) {
//             throw new IllegalArgumentException("ID do cartão é obrigatório");
//         }

//         if (request.recompensaId == null) {
//             throw new IllegalArgumentException("ID da recompensa é obrigatório");
//         }
//     }

//     private void baixarPontosResgate(Resgate resgate) {
//         SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(
//             resgate.usuario.id, resgate.cartao.id
//         ).orElseThrow(() -> new NotFoundException("Saldo de pontos não encontrado"));

//         saldo.saldo -= resgate.pontosUtilizados;
//         saldo.atualizadoEm = LocalDateTime.now();

//         saldoPontosRepository.persist(saldo);

//         // TODO: Registrar movimento de pontos (RESGATE)
//         // registrarMovimentoPontos(saldo, resgate, "RESGATE");
//     }

//     private void atualizarEstoqueRecompensa(Recompensa recompensa) {
//         recompensa.estoque--;
//         recompensa.atualizadoEm = LocalDateTime.now();

//         recompensaRepository.persist(recompensa);
//     }

//     private ResgateResponseDTO toResgateResponseDTO(Resgate resgate) {
//         return new ResgateResponseDTO(
//             resgate.id,
//             resgate.usuario.id,
//             resgate.cartao.id,
//             resgate.recompensa.id,
//             resgate.pontosUtilizados,
//             resgate.status,
//             resgate.criadoEm,
//             resgate.aprovadoEm,
//             resgate.concluidoEm,
//             resgate.negadoEm,
//             resgate.canceladoEm,
//             resgate.observacao
//         );
//     }
// }

