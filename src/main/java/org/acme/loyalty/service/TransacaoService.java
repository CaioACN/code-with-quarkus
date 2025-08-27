package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.TransacaoRequestDTO;
import org.acme.loyalty.dto.TransacaoResponseDTO;
import org.acme.loyalty.entity.Transacao;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.entity.Cartao;
import org.acme.loyalty.repository.TransacaoRepository;
import org.acme.loyalty.repository.UsuarioRepository;
import org.acme.loyalty.repository.CartaoRepository;
import org.acme.loyalty.dto.event.TransactionCreatedEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TransacaoService {

    @Inject
    TransacaoRepository transacaoRepository;

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    CartaoRepository cartaoRepository;

    @Inject
    EventPublisherService eventPublisherService;

    @Transactional
    public Transacao criarTransacao(TransacaoRequestDTO request) {
        // Validar se usuário existe
        Usuario usuario = usuarioRepository.findByIdOptional(request.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + request.usuarioId));

        // Validar se cartão existe e pertence ao usuário
        Cartao cartao = cartaoRepository.findByIdOptional(request.cartaoId)
                .orElseThrow(() -> new NotFoundException("Cartão não encontrado: " + request.cartaoId));

        if (!cartao.usuario.id.equals(request.usuarioId)) {
            throw new IllegalArgumentException("Cartão não pertence ao usuário informado");
        }

        // Validar se cartão está ativo
        if (cartao.estaVencido()) {
            throw new IllegalArgumentException("Cartão está vencido");
        }

        // Criar transação
        Transacao transacao = new Transacao();
        transacao.cartao = cartao;
        transacao.usuario = usuario;
        transacao.valor = request.valor;
        transacao.moeda = request.moeda;
        transacao.mcc = request.mcc;
        transacao.categoria = request.categoria;
        transacao.parceiroId = request.parceiroId;
        transacao.status = "PENDENTE";
        transacao.dataEvento = request.dataEvento != null ? request.dataEvento : LocalDateTime.now();
        transacao.processadoEm = LocalDateTime.now();

        // Persistir transação
        transacaoRepository.persist(transacao);

        // Publicar evento TransactionCreated
        TransactionCreatedEvent event = new TransactionCreatedEvent(
            transacao.id, transacao.usuario.id, transacao.cartao.id,
            transacao.valor, transacao.moeda, transacao.mcc, transacao.categoria,
            transacao.dataEvento
        );
        eventPublisherService.publishEvent(event);

        return transacao;
    }

    public List<TransacaoResponseDTO> listarTransacoes(
            Long usuarioId, Long cartaoId, String status,
            String dataInicio, String dataFim, Integer pagina, Integer tamanho) {
        
        // Construir query baseada nos filtros
        StringBuilder query = new StringBuilder();
        List<Object> params = new java.util.ArrayList<>();

        if (usuarioId != null) {
            query.append("usuario.id = ?").append(params.size() + 1);
            params.add(usuarioId);
        }

        if (cartaoId != null) {
            if (query.length() > 0) query.append(" AND ");
            query.append("cartao.id = ?").append(params.size() + 1);
            params.add(cartaoId);
        }

        if (status != null) {
            if (query.length() > 0) query.append(" AND ");
            query.append("status = ?").append(params.size() + 1);
            params.add(status);
        }

        if (dataInicio != null) {
            if (query.length() > 0) query.append(" AND ");
            query.append("dataEvento >= ?").append(params.size() + 1);
            params.add(parseDateTime(dataInicio));
        }

        if (dataFim != null) {
            if (query.length() > 0) query.append(" AND ");
            query.append("dataEvento <= ?").append(params.size() + 1);
            params.add(parseDateTime(dataFim));
        }

        // Executar query com paginação
        List<Transacao> transacoes;
        if (query.length() > 0) {
            transacoes = transacaoRepository.find(query.toString(), params.toArray())
                    .page(pagina - 1, tamanho)
                    .list();
        } else {
            transacoes = transacaoRepository.findAll()
                    .page(pagina - 1, tamanho)
                    .list();
        }

        // Converter para DTOs
        return transacoes.stream()
                .map(this::toTransacaoResponseDTO)
                .collect(Collectors.toList());
    }

    public TransacaoResponseDTO buscarTransacaoPorId(Long id) {
        Transacao transacao = transacaoRepository.findByIdOptional(id)
                .orElse(null);

        return transacao != null ? toTransacaoResponseDTO(transacao) : null;
    }

    @Transactional
    public TransacaoResponseDTO atualizarStatus(Long id, String novoStatus) {
        Transacao transacao = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));

        // Validar transição de status
        if (!isValidStatusTransition(transacao.status, novoStatus)) {
            throw new IllegalArgumentException("Transição de status inválida: " + transacao.status + " -> " + novoStatus);
        }

        // Atualizar status
        transacao.status = novoStatus;
        transacao.processadoEm = LocalDateTime.now();

        // Persistir alterações
        transacaoRepository.persist(transacao);

        return toTransacaoResponseDTO(transacao);
    }

    @Transactional
    public void deletarTransacao(Long id) {
        Transacao transacao = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));

        // Validar se pode ser deletada
        if (!"PENDENTE".equals(transacao.status)) {
            throw new IllegalArgumentException("Apenas transações pendentes podem ser deletadas");
        }

        transacaoRepository.deleteById(id);
    }

    public Integer consultarPontosTransacao(Long id) {
        Transacao transacao = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));

        return transacao.pontosGerados;
    }

    private TransacaoResponseDTO toTransacaoResponseDTO(Transacao transacao) {
        return new TransacaoResponseDTO(
            transacao.id,
            transacao.cartao.id,
            transacao.usuario.id,
            transacao.valor,
            transacao.moeda,
            transacao.mcc,
            transacao.categoria,
            transacao.parceiroId,
            transacao.status,
            transacao.dataEvento,
            transacao.processadoEm,
            transacao.pontosGerados
        );
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                return LocalDateTime.parse(dateTimeStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de data inválido: " + dateTimeStr);
        }
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // Implementar lógica de validação de transição de status
        if ("PENDENTE".equals(currentStatus)) {
            return "PROCESSANDO".equals(newStatus) || "CANCELADA".equals(newStatus);
        } else if ("PROCESSANDO".equals(currentStatus)) {
            return "CONCLUIDA".equals(newStatus) || "ERRO".equals(newStatus);
        }
        return false;
    }
}

