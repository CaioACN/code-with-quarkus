package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.SaldoPontos;

import java.time.LocalDateTime;
import java.util.*;

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

    /** Upsert seguro para chave composta (usa merge ao invés de persist cego). */
    public SaldoPontos upsert(SaldoPontos sp) {
        if (sp == null) return null;
        // merge garante que, se já existir, será atualizado; se novo, será inserido.
        return getEntityManager().merge(sp);
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
        return find("atualizadoEm between ?1 and ?2", inicio, fim).list();
    }

    // --------------------- JOIN FETCH úteis ---------------------

    /** Carrega saldo com usuário e cartão (evita N+1). */
    public Optional<SaldoPontos> findWithUsuarioAndCartao(Long usuarioId, Long cartaoId) {
        if (usuarioId == null || cartaoId == null) return Optional.empty();
        return find("""
                    from SaldoPontos sp
                    join fetch sp.usuario
                    join fetch sp.cartao
                    where sp.usuario.id = ?1 and sp.cartao.id = ?2
                    """, usuarioId, cartaoId).firstResultOptional();
    }

    /** Carrega saldos do usuário com cartões (evita N+1). */
    public List<SaldoPontos> listByUsuarioWithCartao(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("from SaldoPontos sp join fetch sp.cartao where sp.usuario.id = ?1", usuarioId).list();
    }

    // --------------------- Operações de negócio conforme regra 17.7 ---------------------
    
    /**
     * Atualização atômica do saldo conforme regra 17.7:
     * Consistência via UPSERT atômico junto do movimento_pontos
     */
    public SaldoPontos atualizarSaldoAtomicamente(Long usuarioId, Long cartaoId, Long novoSaldo) {
        if (usuarioId == null || cartaoId == null || novoSaldo == null || novoSaldo < 0) {
            return null;
        }
        
        var opt = findByUsuarioAndCartao(usuarioId, cartaoId);
        if (opt.isPresent()) {
            var saldo = opt.get();
            saldo.saldo = novoSaldo;
            saldo.atualizadoEm = LocalDateTime.now();
            return saldo;
        } else {
            // Criar novo saldo se não existir
            var saldo = new SaldoPontos();
            saldo.usuario = getEntityManager().getReference(org.acme.loyalty.entity.Usuario.class, usuarioId);
            saldo.cartao = getEntityManager().getReference(org.acme.loyalty.entity.Cartao.class, cartaoId);
            saldo.saldo = novoSaldo;
            saldo.atualizadoEm = LocalDateTime.now();
            return upsert(saldo);
        }
    }
    
    /**
     * Decrementa saldo com validação atômica conforme regra 17.7:
     * Impedir saldo negativo: validação em resgates e UPDATE condicional
     */
    public boolean debitarSaldoAtomicamente(Long usuarioId, Long cartaoId, Long pontos) {
        if (usuarioId == null || cartaoId == null || pontos == null || pontos <= 0) {
            return false;
        }
        
        // UPDATE condicional para evitar saldo negativo
        int rowsUpdated = getEntityManager().createQuery(
            "UPDATE SaldoPontos s SET s.saldo = s.saldo - ?1, s.atualizadoEm = ?2 " +
            "WHERE s.usuario.id = ?3 AND s.cartao.id = ?4 AND s.saldo >= ?1")
            .setParameter(1, pontos)
            .setParameter(2, LocalDateTime.now())
            .setParameter(3, usuarioId)
            .setParameter(4, cartaoId)
            .executeUpdate();
            
        return rowsUpdated > 0;
    }
    
    /**
     * Incrementa saldo atômico conforme regra 17.7:
     * Mantido somente por operações de negócio (não alterar manualmente)
     */
    public boolean creditarSaldoAtomicamente(Long usuarioId, Long cartaoId, Long pontos) {
        if (usuarioId == null || cartaoId == null || pontos == null || pontos <= 0) {
            return false;
        }
        
        // UPSERT atômico
        int rowsUpdated = getEntityManager().createQuery(
            "UPDATE SaldoPontos s SET s.saldo = s.saldo + ?1, s.atualizadoEm = ?2 " +
            "WHERE s.usuario.id = ?3 AND s.cartao.id = ?4")
            .setParameter(1, pontos)
            .setParameter(2, LocalDateTime.now())
            .setParameter(3, usuarioId)
            .setParameter(4, cartaoId)
            .executeUpdate();
            
        if (rowsUpdated == 0) {
            // Criar novo registro se não existir
            var saldo = new SaldoPontos();
            saldo.usuario = getEntityManager().getReference(org.acme.loyalty.entity.Usuario.class, usuarioId);
            saldo.cartao = getEntityManager().getReference(org.acme.loyalty.entity.Cartao.class, cartaoId);
            saldo.saldo = pontos;
            saldo.atualizadoEm = LocalDateTime.now();
            upsert(saldo);
            return true;
        }
        
        return true;
    }

    /** Adiciona pontos ao saldo (não cria novo registro aqui). */
    public SaldoPontos adicionarPontos(Long usuarioId, Long cartaoId, Long pontos) {
        if (usuarioId == null || cartaoId == null || pontos == null || pontos <= 0) return null;
        var opt = findByUsuarioAndCartao(usuarioId, cartaoId);
        if (opt.isEmpty()) return null;

        var saldo = opt.get();
        saldo.saldo = (saldo.saldo != null ? saldo.saldo : 0L) + pontos;
        saldo.atualizadoEm = LocalDateTime.now();
        // entidade já gerenciada em transação; não precisa persist()
        return saldo;
    }

    /** Remove pontos do saldo (não permite saldo negativo). */
    public boolean removerPontos(Long usuarioId, Long cartaoId, Long pontos) {
        if (usuarioId == null || cartaoId == null || pontos == null || pontos <= 0) return false;

        var opt = findByUsuarioAndCartao(usuarioId, cartaoId);
        if (opt.isEmpty()) return false;

        var saldo = opt.get();
        if (saldo.saldo == null || saldo.saldo < pontos) return false;

        saldo.saldo -= pontos;
        saldo.atualizadoEm = LocalDateTime.now();
        return true;
    }

    /** Verifica se usuário tem saldo suficiente. */
    public boolean temSaldoSuficiente(Long usuarioId, Long cartaoId, Long pontosNecessarios) {
        if (usuarioId == null || cartaoId == null || pontosNecessarios == null || pontosNecessarios <= 0) return false;
        return findByUsuarioAndCartao(usuarioId, cartaoId)
                .map(sp -> (sp.saldo != null && sp.saldo >= pontosNecessarios))
                .orElse(false);
    }

    /** Obtém saldo atual (0 se não existir). */
    public Long obterSaldoAtual(Long usuarioId, Long cartaoId) {
        if (usuarioId == null || cartaoId == null) return 0L;
        return findByUsuarioAndCartao(usuarioId, cartaoId).map(sp -> sp.saldo != null ? sp.saldo : 0L).orElse(0L);
    }

    // --------------------- Estatísticas e agregações ---------------------

    /** Soma de todos os saldos do sistema (usado no dashboard e métricas). */
    public Long sumSaldoTotal() {
        Object r = find("select coalesce(sum(s.saldo), 0) from SaldoPontos s").firstResult();
        return (r instanceof Number) ? ((Number) r).longValue() : 0L;
    }

    public Long calcularSaldoTotalPorUsuario(Long usuarioId) {
        if (usuarioId == null) return 0L;
        Object r = find("select coalesce(sum(saldo),0) from SaldoPontos where usuario.id = ?1", usuarioId).firstResult();
        return (r instanceof Number) ? ((Number) r).longValue() : 0L;
    }

    public Long calcularSaldoTotalPorCartao(Long cartaoId) {
        if (cartaoId == null) return 0L;
        Object r = find("select coalesce(sum(saldo),0) from SaldoPontos where cartao.id = ?1", cartaoId).firstResult();
        return (r instanceof Number) ? ((Number) r).longValue() : 0L;
    }

    public long countSaldosPositivos() { return count("saldo > 0"); }
    public long countSaldosZero()      { return count("saldo = 0"); }

    /** Valida consistência: saldo_pontos == soma(movimento_pontos) por (usuario,cartao). */
    public List<Map<String, Object>> validarSaldos() {
        String jpql = """
            select sp.usuario.id,
                   sp.cartao.id,
                   sp.saldo,
                   (select coalesce(sum(m.pontos),0)
                      from MovimentoPontos m
                     where m.usuario.id = sp.usuario.id
                       and m.cartao.id  = sp.cartao.id)
            from SaldoPontos sp
            """;
        List<Object[]> rows = getEntityManager().createQuery(jpql, Object[].class).getResultList();

        List<Map<String, Object>> inconsistencias = new ArrayList<>();
        for (Object[] r : rows) {
            Long usuarioId = ((Number) r[0]).longValue();
            Long cartaoId  = ((Number) r[1]).longValue();
            Long saldo     = (r[2] == null) ? 0L : ((Number) r[2]).longValue();
            Long somaMov   = (r[3] == null) ? 0L : ((Number) r[3]).longValue();
            if (!Objects.equals(saldo, somaMov)) {
                inconsistencias.add(Map.of(
                    "usuarioId", usuarioId,
                    "cartaoId",  cartaoId,
                    "saldo",     saldo,
                    "somaMovimentos", somaMov
                ));
            }
        }
        return inconsistencias;
    }

    // --------------------- Paginação & Busca avançada ---------------------

    /** Busca avançada com filtros opcionais e paginação. */
    public List<SaldoPontos> queryAvancada(Long usuarioId, Long cartaoId, Long saldoMinimo,
                                           Long saldoMaximo, LocalDateTime atualizadoDesde,
                                           int page, int size) {
        StringBuilder q = new StringBuilder();
        var params = new ArrayList<>();
        int p = 1;

        if (usuarioId != null) {
            q.append("usuario.id = ?").append(p++);
            params.add(usuarioId);
        }
        if (cartaoId != null) {
            if (q.length() > 0) q.append(" and ");
            q.append("cartao.id = ?").append(p++);
            params.add(cartaoId);
        }
        if (saldoMinimo != null) {
            if (q.length() > 0) q.append(" and ");
            q.append("saldo >= ?").append(p++);
            params.add(saldoMinimo);
        }
        if (saldoMaximo != null) {
            if (q.length() > 0) q.append(" and ");
            q.append("saldo <= ?").append(p++);
            params.add(saldoMaximo);
        }
        if (atualizadoDesde != null) {
            if (q.length() > 0) q.append(" and ");
            q.append("atualizadoEm >= ?").append(p++);
            params.add(atualizadoDesde);
        }

        return (q.length() > 0
                ? find(q.toString(), params.toArray()).page(Page.of(page, size)).list()
                : findAll().page(Page.of(page, size)).list());
    }
}
