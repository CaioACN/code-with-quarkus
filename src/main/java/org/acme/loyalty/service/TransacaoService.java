package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.TransacaoRequestDTO;
import org.acme.loyalty.dto.TransacaoResponseDTO;
import org.acme.loyalty.dto.event.TransactionCreatedEvent;
import org.acme.loyalty.entity.Cartao;
import org.acme.loyalty.entity.Transacao;
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

    // ===================== Criação =====================

    @Transactional
    public Transacao criarTransacao(TransacaoRequestDTO request) {
        // Usuário
        Usuario usuario = usuarioRepository.findByIdOptional(request.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + request.usuarioId));

        // Cartão
        Cartao cartao = cartaoRepository.findByIdOptional(request.cartaoId)
                .orElseThrow(() -> new NotFoundException("Cartão não encontrado: " + request.cartaoId));

        if (!cartao.usuario.id.equals(request.usuarioId)) {
            throw new IllegalArgumentException("Cartão não pertence ao usuário informado");
        }
        if (cartao.estaVencido()) {
            throw new IllegalArgumentException("Cartão está vencido");
        }

        // Transação
        Transacao tx = new Transacao();
        tx.cartao = cartao;
        tx.usuario = usuario;
        tx.valor = request.valor;
        tx.moeda = request.moeda;
        tx.mcc = request.mcc;
        tx.categoria = request.categoria;
        tx.parceiroId = request.parceiroId;
        tx.status = Transacao.StatusTransacao.PENDENTE; // enum!
        tx.dataEvento = (request.dataEvento != null ? request.dataEvento : LocalDateTime.now());
        tx.processadoEm = null; // pendente ainda não processou

        transacaoRepository.persist(tx);

        // Evento de domínio
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                tx.id, tx.usuario.id, tx.cartao.id,
                tx.valor, tx.moeda, tx.mcc, tx.categoria, tx.dataEvento
        );
        eventPublisherService.publishEvent(event);

        return tx;
    }

    // ===================== Consulta =====================

    public List<TransacaoResponseDTO> listarTransacoes(Long usuarioId,
                                                       Long cartaoId,
                                                       String status,
                                                       String dataInicio,
                                                       String dataFim,
                                                       Integer pagina,
                                                       Integer tamanho) {

        int pageIndex = (pagina == null || pagina < 1) ? 0 : (pagina - 1);
        int pageSize  = (tamanho == null || tamanho < 1) ? 20 : tamanho;

        Transacao.StatusTransacao statusEnum = parseStatus(status);
        LocalDateTime ini = parseDateTimeNullable(dataInicio);
        LocalDateTime fim = parseDateTimeNullable(dataFim);

        List<Transacao> lista = transacaoRepository
                .queryAvancada(usuarioId, cartaoId, null, null, statusEnum, ini, fim, pageIndex, pageSize)
                .list();

        return lista.stream().map(this::toTransacaoResponseDTO).collect(Collectors.toList());
    }

    public TransacaoResponseDTO buscarTransacaoPorId(Long id) {
        return transacaoRepository.findByIdOptional(id)
                .map(this::toTransacaoResponseDTO)
                .orElse(null);
    }

    // ===================== Atualização de status =====================

    @Transactional
    public TransacaoResponseDTO atualizarStatus(Long id, String novoStatus) {
        Transacao tx = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));

        Transacao.StatusTransacao novo = parseStatusOrThrow(novoStatus);

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

        if (tx.status != Transacao.StatusTransacao.PENDENTE) {
            throw new IllegalArgumentException("Apenas transações pendentes podem ser deletadas");
        }

        transacaoRepository.deleteById(id);
    }

    // ===================== Pontos =====================

    public Integer consultarPontosTransacao(Long id) {
        Transacao tx = transacaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Transação não encontrada: " + id));
        return tx.pontosGerados;
    }

    // ===================== Helpers =====================

    private TransacaoResponseDTO toTransacaoResponseDTO(Transacao tx) {
        return new TransacaoResponseDTO(
                tx.id,
                tx.cartao.id,
                tx.usuario.id,
                tx.valor,
                tx.moeda,
                tx.mcc,
                tx.categoria,
                tx.parceiroId,
                (tx.status != null ? tx.status.name() : null),
                tx.dataEvento,
                tx.processadoEm,
                tx.pontosGerados
        );
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

    private Transacao.StatusTransacao parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Transacao.StatusTransacao.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null; // status inválido → ignora filtro
        }
    }

    private Transacao.StatusTransacao parseStatusOrThrow(String s) {
        try {
            return Transacao.StatusTransacao.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("Status inválido: " + s);
        }
    }

    /**
     * Regras de transição (compatíveis com o enum da entidade):
     * PENDENTE   -> PROCESSADA, REJEITADA
     * PROCESSADA -> ESTORNADA
     * REJEITADA  -> (terminal)
     * ESTORNADA  -> (terminal)
     */
    private boolean isValidStatusTransition(Transacao.StatusTransacao atual,
                                            Transacao.StatusTransacao novo) {
        if (atual == null || novo == null) return false;
        switch (atual) {
            case PENDENTE:
                return (novo == Transacao.StatusTransacao.PROCESSADA
                        || novo == Transacao.StatusTransacao.REJEITADA);
            case PROCESSADA:
                return (novo == Transacao.StatusTransacao.ESTORNADA);
            case REJEITADA:
            case ESTORNADA:
            default:
                return false;
        }
    }
}
