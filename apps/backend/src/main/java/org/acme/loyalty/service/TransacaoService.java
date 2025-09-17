package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.PageResponseDTO;
import org.acme.loyalty.dto.TransacaoRequestDTO;
import org.acme.loyalty.dto.TransacaoResponseDTO;
import org.acme.loyalty.dto.event.TransactionCreatedEvent;
import org.acme.loyalty.entity.Cartao;
import org.acme.loyalty.entity.Transacao;
import org.acme.loyalty.entity.Transacao.StatusTransacao;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.repository.CartaoRepository;
import org.acme.loyalty.repository.TransacaoRepository;
import org.acme.loyalty.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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

    // Construtor sem argumentos necessário para proxy CDI
    public TransacaoService() {
    }

    // ===================== Criação =====================

    @Transactional
    public Transacao criarTransacao(TransacaoRequestDTO request) {
        // Validações básicas
        if (request.valor == null || request.valor.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valor deve ser >= 0");
        }

        // Usuário
        Usuario usuario = usuarioRepository.findByIdOptional(request.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + request.usuarioId));

        // Cartão
        Cartao cartao = cartaoRepository.findByIdOptional(request.cartaoId)
                .orElseThrow(() -> new NotFoundException("Cartão não encontrado: " + request.cartaoId));

        if (!cartao.usuario.id.equals(request.usuarioId)) {
            throw new IllegalArgumentException("Cartão não pertence ao usuário informado");
        }
        if (!cartao.podeReceberTransacoes()) {
            throw new IllegalArgumentException("Cartão não pode receber transações (vencido ou inativo)");
        }

        // Idempotência conforme regra 17.3
        var transacaoExistente = transacaoRepository.findByChaveNatural(
            request.cartaoId, request.dataEvento, request.autorizacao);
        if (transacaoExistente.isPresent()) {
            return transacaoExistente.get(); // Retorna transação existente
        }

        // Criar nova transação
        Transacao tx = request.toEntity(cartao, usuario);
        tx.status = StatusTransacao.APROVADA; // Conforme regra 17.3
        tx.processadoEm = null; // será processada posteriormente

        transacaoRepository.persist(tx);
        transacaoRepository.flush(); // Força a sincronização com o banco
        
        // Recarrega a entidade com as relações usando JOIN FETCH
        tx = transacaoRepository.findWithCartaoAndUsuario(tx.id)
                .orElseThrow(() -> new RuntimeException("Erro ao recarregar transação"));

        // Evento de domínio
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                tx.id, tx.usuario.id, tx.cartao.id,
                tx.valor, tx.moeda, tx.mcc, tx.categoria, tx.dataEvento
        );
        eventPublisherService.publishEvent(event);

        return tx;
    }

    // ===================== Consulta =====================

    public PageResponseDTO<TransacaoResponseDTO> listarTransacoes(Long usuarioId,
                                                                    Long cartaoId,
                                                                    String status,
                                                                    String dataInicio,
                                                                    String dataFim,
                                                                    Integer pagina,
                                                                    Integer tamanho) {

        int pageIndex = (pagina == null || pagina < 1) ? 0 : (pagina - 1);
        int pageSize  = (tamanho == null || tamanho < 1) ? 20 : tamanho;

        StatusTransacao statusEnum = parseStatus(status);
        LocalDateTime ini = parseDateTimeNullable(dataInicio);
        LocalDateTime fim = parseDateTimeNullable(dataFim);

        // Buscar dados paginados
        List<Transacao> lista = transacaoRepository
                .queryAvancada(usuarioId, cartaoId, null, null, statusEnum, ini, fim, pageIndex, pageSize)
                .list();

        // Contar total de elementos
        long totalElements = transacaoRepository
                .queryAvancadaCount(usuarioId, cartaoId, null, null, statusEnum, ini, fim);

        List<TransacaoResponseDTO> content = lista.stream()
                .map(this::toTransacaoResponseDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.of(
                content,
                totalElements,
                tamanho,
                pagina - 1
        );
    }

    public TransacaoResponseDTO buscarTransacaoPorId(Long id) {
        return transacaoRepository.findWithCartaoAndUsuario(id)
                .map(this::toTransacaoResponseDTO)
                .orElse(null);
    }

    // ===================== Atualização de status =====================

    @Transactional
    public TransacaoResponseDTO atualizarStatus(Long id, String novoStatus) {
        Transacao tx = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));

        StatusTransacao novo = parseStatusOrThrow(novoStatus);

        if (!isValidStatusTransition(tx.status, novo)) {
            throw new IllegalArgumentException("Transição de status inválida: " + tx.status + " -> " + novo);
        }

        tx.status = novo;
        tx.processadoEm = LocalDateTime.now(); // marca o instante da mudança

        transacaoRepository.persist(tx);
        return toTransacaoResponseDTO(tx);
    }

    // ===================== Exclusão =====================

    @Transactional
    public void deletarTransacao(Long id) {
        Transacao tx = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));

        if (tx.status != StatusTransacao.APROVADA) {
            throw new IllegalArgumentException("Apenas transações aprovadas podem ser deletadas");
        }

        transacaoRepository.deleteById(id);
    }

    // ===================== Estorno conforme regra 17.3 =====================
    
    @Transactional
    public TransacaoResponseDTO estornarTransacao(Long id, String motivo) {
        Transacao tx = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));
        
        if (!tx.podeSerProcessada()) {
            throw new IllegalArgumentException("Transação não pode ser estornada no status atual: " + tx.status);
        }
        
        // Marcar como estornada
        tx.status = StatusTransacao.ESTORNADA;
        tx.processadoEm = LocalDateTime.now();
        
        transacaoRepository.persist(tx);
        
        // Disparar evento para gerar movimento ESTORNO no serviço de pontos quando necessário
        
        return toTransacaoResponseDTO(tx);
    }

    // ===================== Pontos =====================

    public Integer consultarPontosTransacao(Long id) {
        Transacao tx = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));
        return tx.pontosGerados;
    }

    // ===================== Helpers =====================

    private TransacaoResponseDTO toTransacaoResponseDTO(Transacao tx) {
        return TransacaoResponseDTO.fromEntity(tx);
    }

    private LocalDateTime parseDateTimeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return parseDateTime(value);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                // Se vier apenas a data, considerar início do dia
                return LocalDateTime.parse(dateTimeStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de data inválido: " + dateTimeStr);
        }
    }

    private StatusTransacao parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return StatusTransacao.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null; // status inválido → ignora filtro
        }
    }

    private StatusTransacao parseStatusOrThrow(String s) {
        try {
            return StatusTransacao.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Status inválido: " + s);
        }
    }

    /**
     * Regras de transição conforme regra 17.3:
     * APROVADA   -> NEGADA, ESTORNADA, AJUSTE
     * NEGADA     -> (terminal)
     * ESTORNADA  -> (terminal)
     * AJUSTE     -> (terminal)
     */
    private boolean isValidStatusTransition(StatusTransacao atual,
                                            StatusTransacao novo) {
        if (atual == null || novo == null) return false;
        if (atual == StatusTransacao.APROVADA) {
            return (novo == StatusTransacao.NEGADA
                    || novo == StatusTransacao.ESTORNADA
                    || novo == StatusTransacao.AJUSTE);
        } else if (atual == StatusTransacao.NEGADA
                || atual == StatusTransacao.ESTORNADA
                || atual == StatusTransacao.AJUSTE) {
            return false;
        } else {
            return false;
        }
    }
}
