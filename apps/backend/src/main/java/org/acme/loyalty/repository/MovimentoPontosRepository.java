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

@ApplicationScoped
public class MovimentoPontosRepository implements PanacheRepository<MovimentoPontos> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public MovimentoPontos upsert(MovimentoPontos m) {
        if (m == null)
            return null;
        if (m.id == null)
            persist(m);
        return m;
    }

    /**
     * Carrega entidade com lock pessimista (útil em acertos concorrentes de saldo).
     */
    public Optional<MovimentoPontos> findByIdForUpdate(Long id) {
        if (id == null)
            return Optional.empty();
        var em = getEntityManager();
        MovimentoPontos m = em.find(MovimentoPontos.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(m);
    }

    // --------------------- Buscas simples ---------------------

    public List<MovimentoPontos> listByUsuarioId(Long usuarioId) {
        if (usuarioId == null)
            return List.of();
        return find("usuario.id = ?1", usuarioId).list();
    }

    public List<MovimentoPontos> listByCartaoId(Long cartaoId) {
        if (cartaoId == null)
            return List.of();
        return find("cartao.id = ?1", cartaoId).list();
    }

    public List<MovimentoPontos> listByTipo(MovimentoPontos.TipoMovimento tipo) {
        if (tipo == null)
            return List.of();
        return find("tipo = ?1", tipo).list();
    }

    public List<MovimentoPontos> listByRefTransacaoId(Long refTransacaoId) {
        if (refTransacaoId == null)
            return List.of();
        return find("refTransacaoId = ?1 order by criadoEm asc", refTransacaoId).list();
    }

    // --------------------- Período / Extrato ---------------------

    public List<MovimentoPontos> listByUsuarioAndPeriodo(Long usuarioId, LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null)
            return List.of();
        if (de == null)
            de = LocalDateTime.MIN;
        if (ate == null)
            ate = LocalDateTime.MAX;
        return find("usuario.id = ?1 and criadoEm between ?2 and ?3", usuarioId, de, ate).list();
    }

    /** Alias usado pelo AdminService. */
    public List<MovimentoPontos> listByUsuarioBetween(Long usuarioId, LocalDateTime de, LocalDateTime ate) {
        return listByUsuarioAndPeriodo(usuarioId, de, ate);
    }

    public List<MovimentoPontos> listByCartaoAndPeriodo(Long cartaoId, LocalDateTime de, LocalDateTime ate) {
        if (cartaoId == null)
            return List.of();
        if (de == null)
            de = LocalDateTime.MIN;
        if (ate == null)
            ate = LocalDateTime.MAX;
        return find("cartao.id = ?1 and criadoEm between ?2 and ?3", cartaoId, de, ate).list();
    }

    /**
     * Extrato paginado por filtros opcionais (ordenado por criadoEm desc).
     */
    public PanacheQuery<MovimentoPontos> queryExtrato(Long usuarioId,
            Long cartaoId,
            LocalDateTime de,
            LocalDateTime ate,
            MovimentoPontos.TipoMovimento tipo,
            int page, int size) {
        if (de == null)
            de = LocalDateTime.MIN;
        if (ate == null)
            ate = LocalDateTime.MAX;

        PanacheQuery<MovimentoPontos> q;

        if (usuarioId != null && cartaoId != null && tipo != null) {
            q = find(
                    "usuario.id = ?1 and cartao.id = ?2 and tipo = ?3 and criadoEm between ?4 and ?5 order by criadoEm desc",
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
        if (usuarioId == null)
            return find("1=0"); // vazio
        var q = find(
                "from MovimentoPontos m left join fetch m.transacao where m.usuario.id = ?1 order by m.criadoEm desc",
                usuarioId);
        return q.page(Page.of(page, size));
    }

    // --------------------- Regras de negócio 17.6 ---------------------
    
    /**
     * Busca movimentos de crédito conforme regra 17.6:
     * positivo para créditos (ACUMULO, AJUSTE+)
     */
    public List<MovimentoPontos> listarCreditos(Long usuarioId, Long cartaoId, LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null) return List.of();
        if (de == null) de = LocalDateTime.MIN;
        if (ate == null) ate = LocalDateTime.MAX;
        
        if (cartaoId != null) {
            return find("usuario.id = ?1 and cartao.id = ?2 and tipo in (?3, ?4) and pontos > 0 and criadoEm between ?5 and ?6 order by criadoEm desc",
                       usuarioId, cartaoId, MovimentoPontos.TipoMovimento.ACUMULO, MovimentoPontos.TipoMovimento.AJUSTE, de, ate).list();
        } else {
            return find("usuario.id = ?1 and tipo in (?2, ?3) and pontos > 0 and criadoEm between ?4 and ?5 order by criadoEm desc",
                       usuarioId, MovimentoPontos.TipoMovimento.ACUMULO, MovimentoPontos.TipoMovimento.AJUSTE, de, ate).list();
        }
    }
    
    /**
     * Busca movimentos de débito conforme regra 17.6:
     * negativo para débitos (RESGATE, EXPIRACAO, ESTORNO)
     */
    public List<MovimentoPontos> listarDebitos(Long usuarioId, Long cartaoId, LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null) return List.of();
        if (de == null) de = LocalDateTime.MIN;
        if (ate == null) ate = LocalDateTime.MAX;
        
        if (cartaoId != null) {
            return find("usuario.id = ?1 and cartao.id = ?2 and tipo in (?3, ?4, ?5) and pontos < 0 and criadoEm between ?6 and ?7 order by criadoEm desc",
                       usuarioId, cartaoId, MovimentoPontos.TipoMovimento.RESGATE, MovimentoPontos.TipoMovimento.EXPIRACAO, MovimentoPontos.TipoMovimento.ESTORNO, de, ate).list();
        } else {
            return find("usuario.id = ?1 and tipo in (?2, ?3, ?4) and pontos < 0 and criadoEm between ?5 and ?6 order by criadoEm desc",
                       usuarioId, MovimentoPontos.TipoMovimento.RESGATE, MovimentoPontos.TipoMovimento.EXPIRACAO, MovimentoPontos.TipoMovimento.ESTORNO, de, ate).list();
        }
    }
    
    /**
     * Busca movimentos vinculados a transação conforme regra 17.6:
     * ref_transacao_id vincula acúmulos/estornos à transacao de origem
     */
    public List<MovimentoPontos> listarVinculadosATransacao(Long transacaoId) {
        if (transacaoId == null) return List.of();
        return find("refTransacaoId = ?1 order by criadoEm asc", transacaoId).list();
    }
    
    /**
     * Verifica se já existe movimento para a transação (idempotência)
     */
    public boolean existeMovimentoParaTransacao(Long transacaoId, MovimentoPontos.TipoMovimento tipo) {
        if (transacaoId == null || tipo == null) return false;
        return count("refTransacaoId = ?1 and tipo = ?2", transacaoId, tipo) > 0;
    }
    
    /**
     * Busca movimentos de expiração por job conforme regra 17.11
     */
    public List<MovimentoPontos> listarExpiracoesPorJob(String jobId) {
        if (jobId == null || jobId.isBlank()) return List.of();
        return find("tipo = ?1 and jobId = ?2 order by criadoEm asc", MovimentoPontos.TipoMovimento.EXPIRACAO, jobId).list();
    }

    // --------------------- Agregações (SUM/COUNT) ---------------------

    public long countByUsuarioAndTipoInPeriodo(Long usuarioId, MovimentoPontos.TipoMovimento tipo,
            LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null || tipo == null)
            return 0L;
        if (de == null)
            de = LocalDateTime.MIN;
        if (ate == null)
            ate = LocalDateTime.MAX;
        return count("usuario.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4",
                usuarioId, tipo, de, ate);
    }

    public Long sumPontosByUsuarioAndTipoInPeriodo(Long usuarioId, MovimentoPontos.TipoMovimento tipo,
            LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null || tipo == null)
            return 0L;
        return sumPontos("usuario.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4",
                usuarioId, tipo, nvl(de), nvl(ate));
    }

    public Long sumPontosByCartaoAndTipoInPeriodo(Long cartaoId, MovimentoPontos.TipoMovimento tipo,
            LocalDateTime de, LocalDateTime ate) {
        if (cartaoId == null || tipo == null)
            return 0L;
        return sumPontos("cartao.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4",
                cartaoId, tipo, nvl(de), nvl(ate));
    }

    public Long sumPontosByUsuarioInPeriodo(Long usuarioId, LocalDateTime de, LocalDateTime ate) {
        if (usuarioId == null)
            return 0L;
        return sumPontos("usuario.id = ?1 and criadoEm between ?2 and ?3",
                usuarioId, nvl(de), nvl(ate));
    }

    public Long sumPontosByCartaoInPeriodo(Long cartaoId, LocalDateTime de, LocalDateTime ate) {
        if (cartaoId == null)
            return 0L;
        return sumPontos("cartao.id = ?1 and criadoEm between ?2 and ?3",
                cartaoId, nvl(de), nvl(ate));
    }

    /** Soma pontos por tipo em um intervalo (usado pelo AdminService). */
    public Long sumPontosByTipoBetween(MovimentoPontos.TipoMovimento tipo, LocalDateTime ini, LocalDateTime fim) {
        if (tipo == null)
            return 0L;
        if (ini == null)
            ini = LocalDateTime.MIN;
        if (fim == null)
            fim = LocalDateTime.MAX;
        return sumPontos("tipo = ?1 and criadoEm between ?2 and ?3", tipo, ini, fim);
    }

    /** Idempotência de ajustes: verifica se já existe movimento com o jobId. */
    public boolean existsByJobId(String jobId) {
        if (jobId == null || jobId.isBlank())
            return false;
        return count("jobId = ?1", jobId) > 0;
    }

    /**
     * Limpeza por retenção (LGPD/operacional). Retorna número de registros
     * apagados.
     */
    public long deleteOlderThan(LocalDateTime limite) {
        if (limite == null)
            return 0L;
        return delete("criadoEm < ?1", limite); // retorna long
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
