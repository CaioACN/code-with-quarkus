package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import org.acme.loyalty.entity.Notificacao;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Repository para Notificacao (Panache).
 *
 * Padrão seguido (como nos repositórios anteriores):
 * - Métodos list* usam find(...).list() (sem q.sort()).
 * - Ordenação feita via "order by ..." no JPQL quando necessário.
 * - Paginação via PanacheQuery.page(Page.of(...)).
 *
 * Regras úteis do domínio:
 * - Pendentes de envio: status em {AGENDADA, ENFILEIRADA, RETENTANDO}
 *   e (agendadoPara IS NULL ou <= agora) e (proximaTentativaEm IS NULL ou <= agora).
 * - Lookup por provider/providerMessageId para deduplicação de callbacks.
 */
@ApplicationScoped
public class NotificacaoRepository implements PanacheRepository<Notificacao> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public Notificacao upsert(Notificacao n) {
        if (n == null) return null;
        if (n.id == null) persist(n);
        return n;
    }

    /** Carrega entidade com lock pessimista (útil ao consolidar retentativas). */
    public Optional<Notificacao> findByIdForUpdate(Long id) {
        if (id == null) return Optional.empty();
        var em = getEntityManager();
        Notificacao n = em.find(Notificacao.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(n);
    }

    // --------------------- Buscas simples ---------------------

    public List<Notificacao> listByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1", usuarioId).list();
    }

    public List<Notificacao> listByStatus(Notificacao.Status status) {
        if (status == null) return List.of();
        return find("status = ?1", status).list();
    }

    public List<Notificacao> listByCanal(Notificacao.Canal canal) {
        if (canal == null) return List.of();
        return find("canal = ?1", canal).list();
    }

    public List<Notificacao> listByTipo(Notificacao.Tipo tipo) {
        if (tipo == null) return List.of();
        return find("tipo = ?1", tipo).list();
    }

    public Optional<Notificacao> findByProviderMessageId(String provider, String providerMessageId) {
        if (provider == null || provider.isBlank() || providerMessageId == null || providerMessageId.isBlank()) {
            return Optional.empty();
        }
        return find("provider = ?1 and providerMessageId = ?2", provider, providerMessageId).firstResultOptional();
    }

    /** Últimas N notificações do usuário (ordenadas por criadoEm desc). */
    public List<Notificacao> listRecentesByUsuario(Long usuarioId, int limit) {
        if (usuarioId == null || limit <= 0) return List.of();
        return find("usuario.id = ?1 order by criadoEm desc", usuarioId)
                .page(Page.of(0, limit))
                .list();
    }

    /** Notificações criadas dentro de um período (ordenadas por criadoEm desc). */
    public List<Notificacao> listByPeriodo(LocalDateTime de, LocalDateTime ate) {
        de = (de == null ? LocalDateTime.MIN : de);
        ate = (ate == null ? LocalDateTime.MAX : ate);
        return find("criadoEm between ?1 and ?2 order by criadoEm desc", de, ate).list();
    }

    // --------------------- Pendências de envio ---------------------

    /**
     * Lista notificações prontas para envio, conforme regra:
     * status em {AGENDADA, ENFILEIRADA, RETENTANDO} AND
     * (agendadoPara IS NULL OR agendadoPara <= agora) AND
     * (proximaTentativaEm IS NULL OR proximaTentativaEm <= agora)
     * Ordenadas por coalesce(proximaTentativaEm, agendadoPara, criadoEm) asc.
     */
    public List<Notificacao> listPendentesParaEnvio(LocalDateTime agora) {
        agora = (agora == null ? LocalDateTime.now() : agora);
        var estados = Arrays.asList(
                Notificacao.Status.AGENDADA,
                Notificacao.Status.ENFILEIRADA,
                Notificacao.Status.RETENTANDO
        );
        return find("""
                status in ?1
                and (agendadoPara is null or agendadoPara <= ?2)
                and (proximaTentativaEm is null or proximaTentativaEm <= ?2)
                order by coalesce(proximaTentativaEm, agendadoPara, criadoEm) asc
                """, estados, agora).list();
    }

    /** Próximo “lote” para envio, limitado por 'limit' (primeira página). */
    public List<Notificacao> nextBatchParaEnvio(LocalDateTime agora, int limit) {
        if (limit <= 0) return List.of();
        agora = (agora == null ? LocalDateTime.now() : agora);
        var estados = Arrays.asList(
                Notificacao.Status.AGENDADA,
                Notificacao.Status.ENFILEIRADA,
                Notificacao.Status.RETENTANDO
        );
        PanacheQuery<Notificacao> q = find("""
                status in ?1
                and (agendadoPara is null or agendadoPara <= ?2)
                and (proximaTentativaEm is null or proximaTentativaEm <= ?2)
                order by coalesce(proximaTentativaEm, agendadoPara, criadoEm) asc
                """, estados, agora);
        return q.page(Page.of(0, limit)).list();
    }

    public long countPendentesParaEnvio(LocalDateTime agora) {
        agora = (agora == null ? LocalDateTime.now() : agora);
        var estados = Arrays.asList(
                Notificacao.Status.AGENDADA,
                Notificacao.Status.ENFILEIRADA,
                Notificacao.Status.RETENTANDO
        );
        return count("""
                status in ?1
                and (agendadoPara is null or agendadoPara <= ?2)
                and (proximaTentativaEm is null or proximaTentativaEm <= ?2)
                """, estados, agora);
    }

    // --------------------- Paginação com filtros ---------------------

    /**
     * Consulta paginada por filtros opcionais, ordenada por criadoEm desc.
     */
    public PanacheQuery<Notificacao> queryByFiltros(Long usuarioId,
                                                    Notificacao.Canal canal,
                                                    Notificacao.Status status,
                                                    Notificacao.Tipo tipo,
                                                    LocalDateTime de,
                                                    LocalDateTime ate,
                                                    int page, int size) {
        de = (de == null ? LocalDateTime.MIN : de);
        ate = (ate == null ? LocalDateTime.MAX : ate);

        PanacheQuery<Notificacao> q;

        if (usuarioId != null && canal != null && status != null && tipo != null) {
            q = find("usuario.id = ?1 and canal = ?2 and status = ?3 and tipo = ?4 and criadoEm between ?5 and ?6 order by criadoEm desc",
                    usuarioId, canal, status, tipo, de, ate);
        } else if (usuarioId != null && canal != null && status != null) {
            q = find("usuario.id = ?1 and canal = ?2 and status = ?3 and criadoEm between ?4 and ?5 order by criadoEm desc",
                    usuarioId, canal, status, de, ate);
        } else if (usuarioId != null && canal != null) {
            q = find("usuario.id = ?1 and canal = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    usuarioId, canal, de, ate);
        } else if (usuarioId != null && status != null) {
            q = find("usuario.id = ?1 and status = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    usuarioId, status, de, ate);
        } else if (usuarioId != null && tipo != null) {
            q = find("usuario.id = ?1 and tipo = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    usuarioId, tipo, de, ate);
        } else if (canal != null && status != null) {
            q = find("canal = ?1 and status = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    canal, status, de, ate);
        } else if (status != null && tipo != null) {
            q = find("status = ?1 and tipo = ?2 and criadoEm between ?3 and ?4 order by criadoEm desc",
                    status, tipo, de, ate);
        } else if (usuarioId != null) {
            q = find("usuario.id = ?1 and criadoEm between ?2 and ?3 order by criadoEm desc",
                    usuarioId, de, ate);
        } else if (canal != null) {
            q = find("canal = ?1 and criadoEm between ?2 and ?3 order by criadoEm desc",
                    canal, de, ate);
        } else if (status != null) {
            q = find("status = ?1 and criadoEm between ?2 and ?3 order by criadoEm desc",
                    status, de, ate);
        } else if (tipo != null) {
            q = find("tipo = ?1 and criadoEm between ?2 and ?3 order by criadoEm desc",
                    tipo, de, ate);
        } else {
            q = find("criadoEm between ?1 and ?2 order by criadoEm desc", de, ate);
        }

        return q.page(Page.of(page, size));
    }

    // --------------------- Contagens ---------------------

    public long countByStatusInPeriodo(Notificacao.Status status, LocalDateTime de, LocalDateTime ate) {
        if (status == null) return 0L;
        de = (de == null ? LocalDateTime.MIN : de);
        ate = (ate == null ? LocalDateTime.MAX : ate);
        return count("status = ?1 and criadoEm between ?2 and ?3", status, de, ate);
    }

    public long countFalhasRecentes(int ultimasHoras) {
        if (ultimasHoras <= 0) return 0L;
        LocalDateTime corte = LocalDateTime.now().minusHours(ultimasHoras);
        return count("status = ?1 and ultimaTentativaEm >= ?2",
                Notificacao.Status.FALHA, corte);
    }
}
