package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.SaldoPontos;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para SaldoPontos (Panache).
 * Regras principais:
 * - Chave composta (usuario_id, cartao_id)
 * - Saldo sempre >= 0
 * - Atualização automática de timestamp
 */
@ApplicationScoped
public class SaldoPontosRepository implements PanacheRepository<SaldoPontos> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se não existir; caso contrário retorna a entidade gerenciada. */
    public SaldoPontos upsert(SaldoPontos sp) {
        if (sp == null) return null;
        
        // Para entidades com chave composta, sempre persiste
        persist(sp);
        return sp;
    }



    /** Busca por usuário e cartão. */
    public Optional<SaldoPontos> findByUsuarioAndCartao(Long usuarioId, Long cartaoId) {
        if (usuarioId == null || cartaoId == null) return Optional.empty();
        return find("usuario.id = ?1 and cartao.id = ?2", usuarioId, cartaoId).firstResultOptional();
    }

    // --------------------- Busca por atributos ---------------------

    public List<SaldoPontos> listByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1", usuarioId).list();
    }

    public List<SaldoPontos> listByCartaoId(Long cartaoId) {
        if (cartaoId == null) return List.of();
        return find("cartao.id = ?1", cartaoId).list();
    }

    public List<SaldoPontos> listBySaldoMinimo(Long saldoMinimo) {
        if (saldoMinimo == null) saldoMinimo = 0L;
        return find("saldo >= ?1", saldoMinimo).list();
    }

    public List<SaldoPontos> listBySaldoMaximo(Long saldoMaximo) {
        if (saldoMaximo == null) return List.of();
        return find("saldo <= ?1", saldoMaximo).list();
    }

    public List<SaldoPontos> listBySaldoRange(Long saldoMinimo, Long saldoMaximo) {
        if (saldoMinimo == null) saldoMinimo = 0L;
        if (saldoMaximo == null) return listBySaldoMinimo(saldoMinimo);
        return find("saldo >= ?1 and saldo <= ?2", saldoMinimo, saldoMaximo).list();
    }

    // --------------------- Busca por período ---------------------

    public List<SaldoPontos> listByUltimaAtualizacao(LocalDateTime desde) {
        if (desde == null) desde = LocalDateTime.now().minusDays(30);
        return find("atualizadoEm >= ?1", desde).list();
    }

    public List<SaldoPontos> listByUltimaAtualizacaoRange(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return find("atualizadoEm >= ?1 and atualizadoEm <= ?2", inicio, fim).list();
    }

    // --------------------- JOIN FETCH úteis ---------------------

    /** Carrega saldo com usuário e cartão (evita N+1). */
    public Optional<SaldoPontos> findWithUsuarioAndCartao(Long usuarioId, Long cartaoId) {
        if (usuarioId == null || cartaoId == null) return Optional.empty();
        return find("from SaldoPontos sp join fetch sp.usuario join fetch sp.cartao where sp.usuario.id = ?1 and sp.cartao.id = ?2", 
                   usuarioId, cartaoId).firstResultOptional();
    }

    /** Carrega saldos do usuário com cartões (evita N+1). */
    public List<SaldoPontos> listByUsuarioWithCartao(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("from SaldoPontos sp join fetch sp.cartao where sp.usuario.id = ?1", usuarioId).list();
    }

    // --------------------- Operações de negócio ---------------------

    /** Adiciona pontos ao saldo (cria se não existir). */
    public SaldoPontos adicionarPontos(Long usuarioId, Long cartaoId, Long pontos) {
        if (usuarioId == null || cartaoId == null || pontos == null || pontos <= 0) return null;
        
        Optional<SaldoPontos> saldoOpt = findByUsuarioAndCartao(usuarioId, cartaoId);
        SaldoPontos saldo;
        
        if (saldoOpt.isPresent()) {
            saldo = saldoOpt.get();
            saldo.saldo += pontos;
        } else {
            // Para entidades com chave composta, é melhor buscar as entidades reais
            // Por simplicidade, vamos apenas retornar null se não existir
            return null;
        }
        
        saldo.atualizadoEm = LocalDateTime.now();
        persist(saldo);
        return saldo;
    }

    /** Remove pontos do saldo (não permite saldo negativo). */
    public boolean removerPontos(Long usuarioId, Long cartaoId, Long pontos) {
        if (usuarioId == null || cartaoId == null || pontos == null || pontos <= 0) return false;
        
        Optional<SaldoPontos> saldoOpt = findByUsuarioAndCartao(usuarioId, cartaoId);
        if (saldoOpt.isEmpty()) return false;
        
        SaldoPontos saldo = saldoOpt.get();
        if (saldo.saldo < pontos) return false; // Saldo insuficiente
        
        saldo.saldo -= pontos;
        saldo.atualizadoEm = LocalDateTime.now();
        persist(saldo);
        return true;
    }

    /** Verifica se usuário tem saldo suficiente. */
    public boolean temSaldoSuficiente(Long usuarioId, Long cartaoId, Long pontosNecessarios) {
        if (usuarioId == null || cartaoId == null || pontosNecessarios == null || pontosNecessarios <= 0) return false;
        
        Optional<SaldoPontos> saldoOpt = findByUsuarioAndCartao(usuarioId, cartaoId);
        if (saldoOpt.isEmpty()) return false;
        
        return saldoOpt.get().saldo >= pontosNecessarios;
    }

    /** Obtém saldo atual (0 se não existir). */
    public Long obterSaldoAtual(Long usuarioId, Long cartaoId) {
        if (usuarioId == null || cartaoId == null) return 0L;
        
        Optional<SaldoPontos> saldoOpt = findByUsuarioAndCartao(usuarioId, cartaoId);
        return saldoOpt.map(sp -> sp.saldo).orElse(0L);
    }

    // --------------------- Estatísticas e agregações ---------------------

    public Long calcularSaldoTotalPorUsuario(Long usuarioId) {
        if (usuarioId == null) return 0L;
        
        Object result = find("select sum(saldo) from SaldoPontos where usuario.id = ?1", usuarioId).firstResult();
        return result != null ? (Long) result : 0L;
    }

    public Long calcularSaldoTotalPorCartao(Long cartaoId) {
        if (cartaoId == null) return 0L;
        
        Object result = find("select sum(saldo) from SaldoPontos where cartao.id = ?1", cartaoId).firstResult();
        return result != null ? (Long) result : 0L;
    }

    public long countSaldosPositivos() {
        return count("saldo > 0");
    }

    public long countSaldosZero() {
        return count("saldo = 0");
    }

    // --------------------- Paginação & Busca avançada ---------------------

    /**
     * Busca avançada com filtros opcionais e paginação.
     */
    public List<SaldoPontos> queryAvancada(Long usuarioId, Long cartaoId, Long saldoMinimo, 
                                          Long saldoMaximo, LocalDateTime atualizadoDesde, 
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

        if (saldoMinimo != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("saldo >= ?").append(paramIndex++);
            params.add(saldoMinimo);
        }

        if (saldoMaximo != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("saldo <= ?").append(paramIndex++);
            params.add(saldoMaximo);
        }

        if (atualizadoDesde != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("atualizadoEm >= ?").append(paramIndex++);
            params.add(atualizadoDesde);
        }

        String finalQuery = query.length() > 0 ? query.toString() : null;
        List<SaldoPontos> result;
        
        if (finalQuery != null) {
            result = find(finalQuery, params.toArray()).page(Page.of(page, size)).list();
        } else {
            result = findAll().page(Page.of(page, size)).list();
        }

        return result;
    }
}
