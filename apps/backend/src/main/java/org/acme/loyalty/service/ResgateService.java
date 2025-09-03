package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.PageRequestDTO;
import org.acme.loyalty.dto.ResgateRequestDTO;
import org.acme.loyalty.dto.ResgateResponseDTO;
import org.acme.loyalty.entity.Cartao;
import org.acme.loyalty.entity.Recompensa;
import org.acme.loyalty.entity.Resgate;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.repository.CartaoRepository;
import org.acme.loyalty.repository.RecompensaRepository;
import org.acme.loyalty.repository.ResgateRepository;
import org.acme.loyalty.repository.SaldoPontosRepository;
import org.acme.loyalty.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ResgateService {

    @Inject
    ResgateRepository resgateRepository;
    @Inject
    UsuarioRepository usuarioRepository;
    @Inject
    CartaoRepository cartaoRepository;
    @Inject
    RecompensaRepository recompensaRepository;
    @Inject
    SaldoPontosRepository saldoPontosRepository;

    // ===================== Solicitação =====================

    @Transactional
    public ResgateResponseDTO solicitarResgate(ResgateRequestDTO request) {
        validarResgate(request);

        Usuario usuario = usuarioRepository.findByIdOptional(request.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + request.usuarioId));

        Cartao cartao = cartaoRepository.findByIdOptional(request.cartaoId)
                .orElseThrow(() -> new NotFoundException("Cartão não encontrado: " + request.cartaoId));

        if (!cartao.usuario.id.equals(request.usuarioId)) {
            throw new IllegalArgumentException("Cartão não pertence ao usuário informado");
        }

        Recompensa recompensa = recompensaRepository.findByIdOptional(request.recompensaId)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + request.recompensaId));

        if (!Boolean.TRUE.equals(recompensa.ativo)) {
            throw new IllegalArgumentException("Recompensa não está ativa");
        }
        if (recompensa.estoque == null || recompensa.estoque <= 0) {
            throw new IllegalArgumentException("Recompensa sem estoque disponível");
        }

        // checa saldo atual (sem debitar ainda)
        Long saldoAtual = saldoPontosRepository.obterSaldoAtual(request.usuarioId, request.cartaoId);
        if (saldoAtual < recompensa.custoPontos) {
            throw new IllegalArgumentException("Saldo insuficiente para resgate");
        }

        Resgate r = new Resgate();
        r.usuario = usuario;
        r.cartao = cartao;
        r.recompensa = recompensa;
        r.pontosUtilizados = recompensa.custoPontos;
        r.status = Resgate.StatusResgate.PENDENTE;
        r.criadoEm = LocalDateTime.now();
        // (não setamos atualizadoEm/canceladoEm pois não existem no entity)

        resgateRepository.persist(r);
        return ResgateResponseDTO.fromEntity(r);
    }

    // ===================== Consultas =====================

    public ResgateResponseDTO buscarResgatePorId(Long id) {
        Resgate r = resgateRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));
        return ResgateResponseDTO.fromEntity(r);
    }

    /**
     * Lista com filtros usando JPQL dinâmico local (para não depender de métodos
     * ausentes no repositório).
     */
    public List<ResgateResponseDTO> listarResgates(String status,
            Long usuarioId,
            Long cartaoId,
            Long recompensaId,
            String dataInicio,
            String dataFim,
            Integer pagina,
            Integer tamanho) {

        PageRequestDTO page = new PageRequestDTO(pagina, tamanho);

        StringBuilder ql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        int idx = 1;

        if (status != null && !status.isBlank()) {
            ql.append("status = ?").append(idx++);
            params.add(Resgate.StatusResgate.valueOf(status.trim().toUpperCase()));
        }
        if (usuarioId != null) {
            if (ql.length() > 0)
                ql.append(" and ");
            ql.append("usuario.id = ?").append(idx++);
            params.add(usuarioId);
        }
        if (cartaoId != null) {
            if (ql.length() > 0)
                ql.append(" and ");
            ql.append("cartao.id = ?").append(idx++);
            params.add(cartaoId);
        }
        if (recompensaId != null) {
            if (ql.length() > 0)
                ql.append(" and ");
            ql.append("recompensa.id = ?").append(idx++);
            params.add(recompensaId);
        }
        if (dataInicio != null && !dataInicio.isBlank()) {
            if (ql.length() > 0)
                ql.append(" and ");
            ql.append("criadoEm >= ?").append(idx++);
            params.add(LocalDateTime.parse(dataInicio));
        }
        if (dataFim != null && !dataFim.isBlank()) {
            if (ql.length() > 0)
                ql.append(" and ");
            ql.append("criadoEm <= ?").append(idx++);
            params.add(LocalDateTime.parse(dataFim));
        }

        var query = (ql.length() > 0)
                ? resgateRepository.find(ql.toString(), params.toArray())
                : resgateRepository.findAll();

        // paginação (Panache usa índice zero-based)
        int pageIndex = (pagina == null || pagina < 1) ? 0 : pagina - 1;
        int pageSize = (tamanho == null || tamanho < 1) ? 20 : tamanho;

        List<Resgate> lista = query.page(pageIndex, pageSize).list();
        return lista.stream()
                .map(ResgateResponseDTO::fromEntity)
                .collect(Collectors.toList());

    }

    public List<ResgateResponseDTO> listarResgatesUsuario(Long usuarioId, String status) {
        StringBuilder ql = new StringBuilder("usuario.id = ?1");
        List<Object> params = new ArrayList<>();
        params.add(usuarioId);

        if (status != null && !status.isBlank()) {
            ql.append(" and status = ?2");
            params.add(Resgate.StatusResgate.valueOf(status.trim().toUpperCase()));
        }

        List<Resgate> lista = resgateRepository.find(ql.toString(), params.toArray()).list();
        return lista.stream().map(ResgateResponseDTO::fromEntity).collect(Collectors.toList());
    }

    public Object acompanharResgate(Long id) {
        // TODO: implementar quando houver requisitos
        resgateRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));
        return null;
    }

    // ===================== Workflow: Aprovar / Concluir / Negar / Cancelar
    // =====================

    @Transactional
    public ResgateResponseDTO aprovarResgate(Long id, String observacao) {
        Resgate r = resgateRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

        if (r.status != Resgate.StatusResgate.PENDENTE) {
            throw new IllegalStateException("Resgate deve estar pendente para ser aprovado");
        }

        r.status = Resgate.StatusResgate.APROVADO;
        r.observacao = observacao;
        r.aprovadoEm = LocalDateTime.now();

        resgateRepository.persist(r);
        return ResgateResponseDTO.fromEntity(r);
    }

    @Transactional
    public ResgateResponseDTO concluirResgate(Long id, String observacao) {
        Resgate r = resgateRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

        if (r.status != Resgate.StatusResgate.APROVADO) {
            throw new IllegalStateException("Resgate deve estar aprovado para ser concluído");
        }

        // 1) Debita pontos com validação (não permite ficar negativo)
        boolean debited = saldoPontosRepository.removerPontos(r.usuario.id, r.cartao.id, r.pontosUtilizados);
        if (!debited) {
            throw new IllegalStateException("Saldo insuficiente para concluir o resgate");
        }

        // 2) Baixa 1 unidade do estoque de forma atômica
        boolean stocked = recompensaRepository.reservarEstoque(r.recompensa.id, 1);
        if (!stocked) {
            // rollback dos pontos
            saldoPontosRepository.adicionarPontos(r.usuario.id, r.cartao.id, r.pontosUtilizados);
            throw new IllegalStateException("Falha ao reservar estoque da recompensa");
        }

        r.status = Resgate.StatusResgate.CONCLUIDO;
        r.observacao = observacao;
        r.concluidoEm = LocalDateTime.now();

        resgateRepository.persist(r);
        return ResgateResponseDTO.fromEntity(r);
    }

    @Transactional
    public ResgateResponseDTO negarResgate(Long id, String motivo) {
        Resgate r = resgateRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

        if (r.status != Resgate.StatusResgate.PENDENTE) {
            throw new IllegalStateException("Resgate deve estar pendente para ser negado");
        }

        r.status = Resgate.StatusResgate.NEGADO;
        r.motivoNegacao = motivo;
        r.observacao = motivo;
        r.negadoEm = LocalDateTime.now();

        resgateRepository.persist(r);
        return ResgateResponseDTO.fromEntity(r);
    }

    @Transactional
    public ResgateResponseDTO cancelarResgate(Long id, String motivo) {
        Resgate r = resgateRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Resgate não encontrado: " + id));

        if (r.status != Resgate.StatusResgate.PENDENTE && r.status != Resgate.StatusResgate.APROVADO) {
            throw new IllegalStateException("Apenas resgates pendentes ou aprovados podem ser cancelados");
        }

        r.status = Resgate.StatusResgate.CANCELADO;
        r.observacao = motivo;
        // NOTA: não setamos canceladoEm porque o entity não expõe esse campo

        resgateRepository.persist(r);
        return ResgateResponseDTO.fromEntity(r);
    }

    // ===================== Validações =====================

    private void validarResgate(ResgateRequestDTO request) {
        if (request.usuarioId == null)
            throw new IllegalArgumentException("ID do usuário é obrigatório");
        if (request.cartaoId == null)
            throw new IllegalArgumentException("ID do cartão é obrigatório");
        if (request.recompensaId == null)
            throw new IllegalArgumentException("ID da recompensa é obrigatório");
    }
}
