package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.Resgate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository para Resgate (Panache).
 * Regras principais:
 * - Status controla o fluxo: PENDENTE → APROVADO → CONCLUIDO|NEGADO
 * - Pontos são debitados apenas após aprovação
 * - Histórico completo de resgates é mantido
 *
 * Compatível com Java 17 / Quarkus 3.
 */
@ApplicationScoped
public class ResgateRepository implements PanacheRepository<Resgate> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public Resgate upsert(Resgate r) {
        if (r == null) return null;
        if (r.id == null) persist(r);
        return r;
    }

    // --------------------- Busca por atributos ---------------------

    public List<Resgate> listByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1", usuarioId).list();
    }

    public List<Resgate> listByCartaoId(Long cartaoId) {
        if (cartaoId == null) return List.of();
        return find("cartao.id = ?1", cartaoId).list();
    }

    public List<Resgate> listByRecompensaId(Long recompensaId) {
        if (recompensaId == null) return List.of();
        return find("recompensa.id = ?1", recompensaId).list();
    }

    public List<Resgate> listByStatus(Resgate.StatusResgate status) {
        if (status == null) return List.of();
        return find("status = ?1", status).list();
    }

    public List<Resgate> listByParceiroId(Long parceiroId) {
        if (parceiroId == null) return List.of();
        return find("recompensa.parceiroId = ?1", parceiroId).list();
    }

    // --------------------- Busca por período ---------------------

    public List<Resgate> listByPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return find("criadoEm between ?1 and ?2", inicio, fim).list();
    }

    public List<Resgate> listByUsuarioAndPeriodo(Long usuarioId, LocalDateTime inicio, LocalDateTime fim) {
        if (usuarioId == null) return List.of();
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return find("usuario.id = ?1 and criadoEm between ?2 and ?3", usuarioId, inicio, fim).list();
    }

    public List<Resgate> listByCartaoAndPeriodo(Long cartaoId, LocalDateTime inicio, LocalDateTime fim) {
        if (cartaoId == null) return List.of();
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();
        return find("cartao.id = ?1 and criadoEm between ?2 and ?3", cartaoId, inicio, fim).list();
    }

    // --------------------- Resgates por status ---------------------

    public List<Resgate> listarPendentes() {
        return find("status = ?1", Resgate.StatusResgate.PENDENTE).list();
    }

    public List<Resgate> listarPendentesPorUsuario(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1 and status = ?2", usuarioId, Resgate.StatusResgate.PENDENTE).list();
    }

    public List<Resgate> listarAprovados() {
        return find("status = ?1", Resgate.StatusResgate.APROVADO).list();
    }

    public List<Resgate> listarConcluidos() {
        return find("status = ?1", Resgate.StatusResgate.CONCLUIDO).list();
    }

    public List<Resgate> listarNegados() {
        return find("status = ?1", Resgate.StatusResgate.NEGADO).list();
    }

    public long countPendentes() {
        return count("status = ?1", Resgate.StatusResgate.PENDENTE);
    }

    public long countAprovados() {
        return count("status = ?1", Resgate.StatusResgate.APROVADO);
    }

    public long countConcluidos() {
        return count("status = ?1", Resgate.StatusResgate.CONCLUIDO);
    }

    public long countNegados() {
        return count("status = ?1", Resgate.StatusResgate.NEGADO);
    }

    // --------------------- JOIN FETCH úteis ---------------------

    /** Carrega resgate com usuário, cartão e recompensa (evita N+1). */
    public Optional<Resgate> findWithUsuarioAndCartaoAndRecompensa(Long id) {
        return find("from Resgate r join fetch r.usuario join fetch r.cartao join fetch r.recompensa where r.id = ?1", id)
                .firstResultOptional();
    }

    /** Carrega resgates do usuário com recompensas (evita N+1). */
    public List<Resgate> listByUsuarioWithRecompensa(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("from Resgate r join fetch r.recompensa where r.usuario.id = ?1", usuarioId).list();
    }

    /** Carrega resgates do cartão com recompensas (evita N+1). */
    public List<Resgate> listByCartaoWithRecompensa(Long cartaoId) {
        if (cartaoId == null) return List.of();
        return find("from Resgate r join fetch r.recompensa where r.cartao.id = ?1", cartaoId).list();
    }

    // --------------------- Operações de negócio ---------------------

    /** Aprova um resgate pendente. */
    public boolean aprovarResgate(Long resgateId) {
        if (resgateId == null) return false;

        Optional<Resgate> resgateOpt = findByIdOptional(resgateId);
        if (resgateOpt.isEmpty()) return false;

        Resgate resgate = resgateOpt.get();
        if (!Resgate.StatusResgate.PENDENTE.equals(resgate.status)) return false;

        resgate.status = Resgate.StatusResgate.APROVADO;
        resgate.aprovadoEm = LocalDateTime.now();
        return true;
    }

    /** Conclui um resgate aprovado. */
    public boolean concluirResgate(Long resgateId) {
        if (resgateId == null) return false;

        Optional<Resgate> resgateOpt = findByIdOptional(resgateId);
        if (resgateOpt.isEmpty()) return false;

        Resgate resgate = resgateOpt.get();
        if (!Resgate.StatusResgate.APROVADO.equals(resgate.status)) return false;

        resgate.status = Resgate.StatusResgate.CONCLUIDO;
        resgate.concluidoEm = LocalDateTime.now();
        return true;
    }

    /** Nega um resgate pendente. */
    public boolean negarResgate(Long resgateId, String motivo) {
        if (resgateId == null) return false;

        Optional<Resgate> resgateOpt = findByIdOptional(resgateId);
        if (resgateOpt.isEmpty()) return false;

        Resgate resgate = resgateOpt.get();
        if (!Resgate.StatusResgate.PENDENTE.equals(resgate.status)) return false;

        resgate.status = Resgate.StatusResgate.NEGADO;
        resgate.negadoEm = LocalDateTime.now();
        resgate.motivoNegacao = motivo;
        return true;
    }

    /** Verifica se usuário tem resgates pendentes. */
    public boolean temResgatesPendentes(Long usuarioId) {
        if (usuarioId == null) return false;
        return count("usuario.id = ?1 and status = ?2", usuarioId, Resgate.StatusResgate.PENDENTE) > 0;
    }

    /** Verifica se usuário tem resgates aprovados não concluídos. */
    public boolean temResgatesAprovadosNaoConcluidos(Long usuarioId) {
        if (usuarioId == null) return false;
        return count("usuario.id = ?1 and status = ?2", usuarioId, Resgate.StatusResgate.APROVADO) > 0;
    }

    // --------------------- Estatísticas e agregações ---------------------

    public Long calcularPontosUtilizadosPorUsuario(Long usuarioId, LocalDateTime inicio, LocalDateTime fim) {
        if (usuarioId == null) return 0L;
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();

        List<Resgate.StatusResgate> sts = List.of(Resgate.StatusResgate.APROVADO, Resgate.StatusResgate.CONCLUIDO);
        Object result = find(
                "select sum(pontosUtilizados) from Resgate " +
                "where usuario.id = ?1 and status in ?2 and criadoEm between ?3 and ?4",
                usuarioId, sts, inicio, fim
        ).firstResult();
        return (result instanceof Number) ? ((Number) result).longValue() : 0L;
    }

    public Long calcularPontosUtilizadosPorCartao(Long cartaoId, LocalDateTime inicio, LocalDateTime fim) {
        if (cartaoId == null) return 0L;
        if (inicio == null) inicio = LocalDateTime.now().minusDays(30);
        if (fim == null) fim = LocalDateTime.now();

        List<Resgate.StatusResgate> sts = List.of(Resgate.StatusResgate.APROVADO, Resgate.StatusResgate.CONCLUIDO);
        Object result = find(
                "select sum(pontosUtilizados) from Resgate " +
                "where cartao.id = ?1 and status in ?2 and criadoEm between ?3 and ?4",
                cartaoId, sts, inicio, fim
        ).firstResult();
        return (result instanceof Number) ? ((Number) result).longValue() : 0L;
    }

    public long countResgatesPorStatus(Resgate.StatusResgate status) {
        if (status == null) return 0;
        return count("status = ?1", status);
    }

    // --------------------- Paginação & Busca avançada ---------------------

    /**
     * Busca avançada com filtros opcionais e paginação, ordenada por criadoEm desc.
     */
    public PanacheQuery<Resgate> queryAvancada(Long usuarioId, Long cartaoId, Long recompensaId,
                                               Resgate.StatusResgate status, Long parceiroId,
                                               LocalDateTime criadoDesde, LocalDateTime criadoAte,
                                               int page, int size) {

        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        int i = 1;

        if (usuarioId != null) {
            where.append("usuario.id = ?").append(i++);
            params.add(usuarioId);
        }
        if (cartaoId != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("cartao.id = ?").append(i++);
            params.add(cartaoId);
        }
        if (recompensaId != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("recompensa.id = ?").append(i++);
            params.add(recompensaId);
        }
        if (status != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("status = ?").append(i++);
            params.add(status);
        }
        if (parceiroId != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("recompensa.parceiroId = ?").append(i++);
            params.add(parceiroId);
        }
        if (criadoDesde != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("criadoEm >= ?").append(i++);
            params.add(criadoDesde);
        }
        if (criadoAte != null) {
            if (where.length() > 0) where.append(" and ");
            where.append("criadoEm <= ?").append(i++);
            params.add(criadoAte);
        }

        String jpql = (where.length() > 0 ? where + " " : "") + "order by criadoEm desc";
        PanacheQuery<Resgate> q = (where.length() > 0) ? find(jpql, params.toArray()) : find("order by criadoEm desc");

        return q.page(Page.of(page, size));
    }
}
