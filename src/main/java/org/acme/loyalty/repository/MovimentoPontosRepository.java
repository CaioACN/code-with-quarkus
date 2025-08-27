package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import org.acme.loyalty.entity.MovimentoPontos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para MovimentoPontos (Panache).
 *
 * Padrão seguido:
 * - Métodos list* retornam find(...).list() (sem Sort API).
 * - Paginação via PanacheQuery.page(Page.of(...)).
 * - Ordenação, quando necessária, feita no JPQL ("order by ...") em vez de q.sort(...).
 *
 * Compatível com Java 17 / Quarkus 3.
 */
@ApplicationScoped
public class MovimentoPontosRepository implements PanacheRepository<MovimentoPontos> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public MovimentoPontos upsert(MovimentoPontos m) {
        if (m == null) return null;
        if (m.id == null) persist(m);
        return m;
    }

    /** Carrega entidade com lock pessimista (útil em acertos concorrentes de saldo). */
    public Optional<MovimentoPontos> findByIdForUpdate(Long id) {
        if (id == null) return Optional.empty();
        var em = getEntityManager();
        MovimentoPontos m = em.find(MovimentoPontos.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(m);
    }

    // --------------------- Buscas simples ---------------------

    public List<MovimentoPontos> listByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1", usuarioId).list();
    }

    public List<MovimentoPontos> listByCartaoId(Long cartaoId) {
        if (cartaoId == null) return List.of();
        return find("cartao.id = ?1", cartaoId).list();
    }

    public List<MovimentoPontos> listByTipo(MovimentoPontos.TipoMovimento tipo) {
        if (tipo == null) return List.of();
        return find("tipo = ?1", tipo).list();
    }

    public List<MovimentoPontos> listByRefTransacaoId(Long refTransacaoId) {
        if (refTransacaoId == null) return List.of();
        // ordenação por criadoEm asc diretamente no JPQL (sem Sort API)
        return find("refTransacaoId = ?1 order by criadoEm asc", refTransacaoId).list();
    }

    // --------------------- Período / Extrato ---------------------

    public List<MovimentoPontos> listByUsuarioAndPeriodo(Long usuarioId, LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null) return List.of();
        if (de == null) de = LocalDateTime.MIN;
        if (ate == null) ate = LocalDateTime.MAX;
        return find("usuario.id = ?1 and criadoEm between ?2 and ?3", usuarioId, de, ate).list();
    }

    public List<MovimentoPontos> listByCartaoAndPeriodo(Long cartaoId, LocalDateTime de, LocalDateTime ate) {
        if (cartaoId == null) return List.of();
        if (de == null) de = LocalDateTime.MIN;
        if (ate == null) ate = LocalDateTime.MAX;
        return find("cartao.id = ?1 and criadoEm between ?2 and ?3", cartaoId, de, ate).list();
    }

    /**
     * Extrato paginado por filtros opcionais.
     * Ordenação por criadoEm desc embutida no JPQL quando aplicável.
     */
    public PanacheQuery<MovimentoPontos> queryExtrato(Long usuarioId,
                                                      Long cartaoId,
                                                      LocalDateTime de,
                                                      LocalDateTime ate,
                                                      MovimentoPontos.TipoMovimento tipo,
                                                      int page, int size) {
        if (de == null) de = LocalDateTime.MIN;
        if (ate == null) ate = LocalDateTime.MAX;

        PanacheQuery<MovimentoPontos> q;

        if (usuarioId != null && cartaoId != null && tipo != null) {
            q = find("usuario.id = ?1 and cartao.id = ?2 and tipo = ?3 and criadoEm between ?4 and ?5 order by criadoEm desc",
                    usuarioId, cartaoId, tipo, de, ate);
        } else if (usuarioId != null && cartaoId != null) {
            q = find("usuario.id = ?1 and cartao.id = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    usuarioId, cartaoId, de, ate);
        } else if (usuarioId != null && tipo != null) {
            q = find("usuario.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    usuarioId, tipo, de, ate);
        } else if (cartaoId != null && tipo != null) {
            q = find("cartao.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    cartaoId, tipo, de, ate);
        } else if (usuarioId != null) {
            q = find("usuario.id = ?1 and criadoEm between ?2 and ?3 order by criadoEm desc",
                    usuarioId, de, ate);
        } else if (cartaoId != null) {
            q = find("cartao.id = ?1 and criadoEm between ?2 and ?3 order by criadoEm desc",
                    cartaoId, de, ate);
        } else if (tipo != null) {
            q = find("tipo = ?1 and criadoEm between ?2 and ?3 order by criadoEm desc",
                    tipo, de, ate);
        } else {
            q = find("criadoEm between ?1 and ?2 order by criadoEm desc", de, ate);
        }

        return q.page(Page.of(page, size));
    }

    // --------------------- JOIN FETCH úteis ---------------------

    /** Carrega movimento com Transacao (evita N+1 quando exibindo origem). */
    public Optional<MovimentoPontos> findWithTransacao(Long id) {
        return find("from MovimentoPontos m left join fetch m.transacao where m.id = ?1", id)
                .firstResultOptional();
    }

    /** Extrato paginado já com Transacao (útil para telas de auditoria). */
    public PanacheQuery<MovimentoPontos> queryWithTransacaoByUsuario(Long usuarioId, int page, int size) {
        if (usuarioId == null) return find("1=0"); // vazio
        var q = find("from MovimentoPontos m left join fetch m.transacao where m.usuario.id = ?1 order by m.criadoEm desc",
                usuarioId);
        return q.page(Page.of(page, size));
    }

    // --------------------- Agregações (SUM/COUNT) ---------------------

    public long countByUsuarioAndTipoInPeriodo(Long usuarioId, MovimentoPontos.TipoMovimento tipo,
                                               LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null || tipo == null) return 0L;
        if (de == null) de = LocalDateTime.MIN;
        if (ate == null) ate = LocalDateTime.MAX;
        return count("usuario.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4",
                usuarioId, tipo, de, ate);
    }

    public Long sumPontosByUsuarioAndTipoInPeriodo(Long usuarioId, MovimentoPontos.TipoMovimento tipo,
                                                   LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null || tipo == null) return 0L;
        return sumPontos("usuario.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4",
                usuarioId, tipo, nvl(de), nvl(ate));
    }

    public Long sumPontosByCartaoAndTipoInPeriodo(Long cartaoId, MovimentoPontos.TipoMovimento tipo,
                                                  LocalDateTime de, LocalDateTime ate) {
        if (cartaoId == null || tipo == null) return 0L;
        return sumPontos("cartao.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4",
                cartaoId, tipo, nvl(de), nvl(ate));
    }

    public Long sumPontosByUsuarioInPeriodo(Long usuarioId, LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null) return 0L;
        return sumPontos("usuario.id = ?1 and criadoEm between ?2 and ?3",
                usuarioId, nvl(de), nvl(ate));
    }

    public Long sumPontosByCartaoInPeriodo(Long cartaoId, LocalDateTime de, LocalDateTime ate) {
        if (cartaoId == null) return 0L;
        return sumPontos("cartao.id = ?1 and criadoEm between ?2 and ?3",
                cartaoId, nvl(de), nvl(ate));
    }

    // --------------------- Helpers internos ---------------------

    private Long sumPontos(String where, Object... params) {
        String jpql = "select coalesce(sum(m.pontos), 0) from MovimentoPontos m where " + where;
        Query q = getEntityManager().createQuery(jpql);
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
        Object r = q.getSingleResult();
        return (r == null) ? 0L : ((Number) r).longValue();
    }

    private static LocalDateTime nvl(LocalDateTime dt) {
        return (dt == null) ? LocalDateTime.MAX : dt;
    }
}
