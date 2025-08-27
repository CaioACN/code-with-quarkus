package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para Transacao (Panache).
 * Regras principais:
 * - Transações são únicas por ID
 * - Consultas por usuário, cartão e período são frequentes
 * - Status controla o processamento de pontos
 */
@ApplicationScoped
public class TransacaoRepository implements PanacheRepository<Transacao> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public Transacao upsert(Transacao t) {
        if (t == null) return null;
        if (t.id == null) persist(t);
        return t;
    }

    // --------------------- Busca por atributos ---------------------

    public List<Transacao> listByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1", usuarioId).list();
    }

    public List<Transacao> listByCartaoId(Long cartaoId) {
        if (cartaoId == null) return List.of();
        return find("cartao.id = ?1", cartaoId).list();
    }

    public List<Transacao> listByStatus(Transacao.StatusTransacao status) {
        if (status == null) return List.of();
        return find("status = ?1", status).list();
    }

    public List<Transacao> listByParceiroId(Long parceiroId) {
        if (parceiroId == null) return List.of();
        return find("parceiroId = ?1", parceiroId).list();
    }

    public List<Transacao> listByMcc(String mcc) {
        if (mcc == null || mcc.isBlank()) return List.of();
        return find("mcc = ?1", mcc.trim()).list();
    }

    public List<Transacao> listByCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return List.of();
        return find("lower(categoria) like ?1", "%" + categoria.trim().toLowerCase() + "%").list();
    }

    // --------------------- Busca por período ---------------------

    public List<Transacao> listByPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return find("dataEvento >= ?1 and dataEvento <= ?2", inicio, fim).list();
    }

    public List<Transacao> listByUsuarioAndPeriodo(Long usuarioId, LocalDateTime inicio, LocalDateTime fim) {
        if (usuarioId == null) return List.of();
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return find("usuario.id = ?1 and dataEvento >= ?2 and dataEvento <= ?3", usuarioId, inicio, fim).list();
    }

    public List<Transacao> listByCartaoAndPeriodo(Long cartaoId, LocalDateTime inicio, LocalDateTime fim) {
        if (cartaoId == null) return List.of();
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return find("cartao.id = ?1 and dataEvento >= ?2 and dataEvento <= ?3", cartaoId, inicio, fim).list();
    }

    // --------------------- Transações pendentes de processamento ---------------------

    public List<Transacao> listarPendentesProcessamento() {
        return find("status = ?1", Transacao.StatusTransacao.PENDENTE).list();
    }

    public List<Transacao> listarPendentesProcessamentoPorUsuario(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1 and status = ?2", usuarioId, Transacao.StatusTransacao.PENDENTE).list();
    }

    public long countPendentesProcessamento() {
        return count("status = ?1", Transacao.StatusTransacao.PENDENTE);
    }

    // --------------------- Estatísticas e agregações ---------------------

    public BigDecimal calcularValorTotalPorUsuario(Long usuarioId, LocalDateTime inicio, LocalDateTime fim) {
        if (usuarioId == null) return BigDecimal.ZERO;
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        
        Object result = find("select sum(valor) from Transacao where usuario.id = ?1 and dataEvento >= ?2 and dataEvento <= ?3", 
                         usuarioId, inicio, fim).firstResult();
        return result != null ? (BigDecimal) result : BigDecimal.ZERO;
    }

    public BigDecimal calcularValorTotalPorCartao(Long cartaoId, LocalDateTime inicio, LocalDateTime fim) {
        if (cartaoId == null) return BigDecimal.ZERO;
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        
        Object result = find("select sum(valor) from Transacao where cartao.id = ?1 and dataEvento >= ?2 and dataEvento <= ?3", 
                         cartaoId, inicio, fim).firstResult();
        return result != null ? (BigDecimal) result : BigDecimal.ZERO;
    }

    public long countTransacoesPorUsuario(Long usuarioId, LocalDateTime inicio, LocalDateTime fim) {
        if (usuarioId == null) return 0;
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return count("usuario.id = ?1 and dataEvento >= ?2 and dataEvento <= ?3", usuarioId, inicio, fim);
    }

    // --------------------- JOIN FETCH úteis ---------------------

    /** Carrega transação com cartão e usuário (evita N+1). */
    public Optional<Transacao> findWithCartaoAndUsuario(Long id) {
        return find("from Transacao t join fetch t.cartao join fetch t.usuario where t.id = ?1", id).firstResultOptional();
    }

    /** Carrega transações do usuário com cartões (evita N+1). */
    public List<Transacao> listByUsuarioWithCartao(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("from Transacao t join fetch t.cartao where t.usuario.id = ?1", usuarioId).list();
    }

    // --------------------- Paginação & Busca avançada ---------------------

    /**
     * Busca avançada com filtros opcionais e paginação.
     */
    public PanacheQuery<Transacao> queryAvancada(Long usuarioId, Long cartaoId, String mcc, 
                                                String categoria, Transacao.StatusTransacao status,
                                                LocalDateTime inicio, LocalDateTime fim, 
                                                int page, int size) {
        StringBuilder query = new StringBuilder();
        var params = new java.util.ArrayList<>();
        int paramIndex = 1;

        if (usuarioId != null) {
            query.append("usuario.id = ?").append(paramIndex++);
            params.add(usuarioId);
        }

        if (cartaoId != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("cartao.id = ?").append(paramIndex++);
            params.add(cartaoId);
        }

        if (mcc != null && !mcc.isBlank()) {
            if (query.length() > 0) query.append(" and ");
            query.append("mcc = ?").append(paramIndex++);
            params.add(mcc.trim());
        }

        if (categoria != null && !categoria.isBlank()) {
            if (query.length() > 0) query.append(" and ");
            query.append("lower(categoria) like ?").append(paramIndex++);
            params.add("%" + categoria.trim().toLowerCase() + "%");
        }

        if (status != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("status = ?").append(paramIndex++);
            params.add(status);
        }

        if (inicio != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("dataEvento >= ?").append(paramIndex++);
            params.add(inicio);
        }

        if (fim != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("dataEvento <= ?").append(paramIndex++);
            params.add(fim);
        }

        String finalQuery = query.length() > 0 ? query.toString() : null;
        PanacheQuery<Transacao> panacheQuery;
        
        if (finalQuery != null) {
            panacheQuery = find(finalQuery, params.toArray());
        } else {
            panacheQuery = findAll();
        }

        return panacheQuery.page(Page.of(page, size));
    }
}
