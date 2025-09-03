package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import org.acme.loyalty.entity.Recompensa;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository para Recompensa (Panache).
 *
 * Padrão seguido:
 * - Métodos list* usam find(...).list() (sem q.sort()).
 * - Ordenação é feita com "order by ..." no JPQL quando necessário.
 * - Paginação via PanacheQuery.page(Page.of(...)).
 *
 * Regras de negócio consideradas:
 * - "Disponível": ativo = true, estoque > 0, (validadeRecompensa IS NULL ou >= agora).
 * - Operações de estoque usam UPDATE otimista em nível de linha (com checagem de condição).
 * - Busca por catálogo com filtros opcionais.
 */
@ApplicationScoped
public class RecompensaRepository implements PanacheRepository<Recompensa> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public Recompensa upsert(Recompensa r) {
        if (r == null) return null;
        if (r.id == null) persist(r);
        return r;
    }

    /** Carrega com lock pessimista (útil para ajustes críticos de estoque). */
    public Optional<Recompensa> findByIdForUpdate(Long id) {
        if (id == null) return Optional.empty();
        var em = getEntityManager();
        Recompensa r = em.find(Recompensa.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(r);
    }

    // --------------------- Consultas simples ---------------------

    public Optional<Recompensa> findAtivaById(Long id) {
        if (id == null) return Optional.empty();
        return find("id = ?1 and ativo = true", id).firstResultOptional();
    }

    public List<Recompensa> listAtivas() {
        return find("ativo = true").list();
    }

    public List<Recompensa> listDisponiveis(LocalDateTime agora) {
        agora = (agora == null ? LocalDateTime.now() : agora);
        return find("""
                ativo = true
                and estoque > 0
                and (validadeRecompensa is null or validadeRecompensa >= ?1)
                order by custoPontos asc, descricao asc
                """, agora).list();
    }

    public List<Recompensa> listVencidas(LocalDateTime agora) {
        agora = (agora == null ? LocalDateTime.now() : agora);
        return find("validadeRecompensa is not null and validadeRecompensa < ?1 order by validadeRecompensa desc", agora)
                .list();
    }

    public List<Recompensa> listByTipo(Recompensa.TipoRecompensa tipo) {
        if (tipo == null) return List.of();
        return find("tipo = ?1", tipo).list();
    }

    public List<Recompensa> listByParceiroId(Long parceiroId) {
        if (parceiroId == null) return List.of();
        return find("parceiroId = ?1", parceiroId).list();
    }

    public boolean existsByDescricaoIgnoringId(String descricao, Long exceptId) {
        if (descricao == null || descricao.isBlank()) return false;
        String d = descricao.trim().toLowerCase();
        if (exceptId == null) {
            return count("lower(descricao) = ?1", d) > 0;
        }
        return count("lower(descricao) = ?1 and id <> ?2", d, exceptId) > 0;
    }

    // --------------------- Catálogo / Busca e paginação ---------------------

    /**
     * Busca paginada no catálogo com filtros opcionais.
     * Ordenação padrão: custoPontos asc, descricao asc.
     */
    public PanacheQuery<Recompensa> queryCatalogo(Recompensa.TipoRecompensa tipo,
                                                  Long parceiroId,
                                                  Boolean somenteDisponiveis,
                                                  Long minCusto,
                                                  Long maxCusto,
                                                  Boolean apenasAtivas,
                                                  LocalDateTime agora,
                                                  int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder jpql = new StringBuilder("1=1");

        if (apenasAtivas != null && apenasAtivas) {
            jpql.append(" and ativo = true");
        }
        if (tipo != null) {
            jpql.append(" and tipo = ?").append(params.size() + 1);
            params.add(tipo);
        }
        if (parceiroId != null) {
            jpql.append(" and parceiroId = ?").append(params.size() + 1);
            params.add(parceiroId);
        }
        if (minCusto != null) {
            jpql.append(" and custoPontos >= ?").append(params.size() + 1);
            params.add(minCusto);
        }
        if (maxCusto != null) {
            jpql.append(" and custoPontos <= ?").append(params.size() + 1);
            params.add(maxCusto);
        }
        if (somenteDisponiveis != null && somenteDisponiveis) {
            agora = (agora == null ? LocalDateTime.now() : agora);
            jpql.append("""
                    and estoque > 0
                    and (validadeRecompensa is null or validadeRecompensa >= ?%d)
                    """.formatted(params.size() + 1));
            params.add(agora);
        }

        jpql.append(" order by custoPontos asc, descricao asc");

        PanacheQuery<Recompensa> q = find(jpql.toString(), params.toArray());
        return q.page(Page.of(page, size));
    }

    /** Busca por descrição (like, case-insensitive) com paginação. */
    public PanacheQuery<Recompensa> searchByDescricao(String termo, int page, int size) {
        if (termo == null || termo.isBlank()) {
            return find("1=1 order by descricao asc").page(Page.of(page, size));
        }
        String like = "%" + termo.trim().toLowerCase() + "%";
        return find("lower(descricao) like ?1 order by descricao asc", like)
                .page(Page.of(page, size));
    }

    // --------------------- Operações de estoque (atômicas) ---------------------

    /**
     * Reserva/baixa estoque de forma atômica (se houver saldo suficiente).
     * @return true se atualizou uma linha (sucesso), false caso contrário.
     */
    public boolean reservarEstoque(Long recompensaId, long quantidade) {
        if (recompensaId == null || quantidade <= 0) return false;
        int updated = update(
                "estoque = estoque - ?2, atualizadoEm = ?3 " +
                "where id = ?1 and ativo = true and estoque >= ?2",
                recompensaId, quantidade, LocalDateTime.now());
        return updated > 0;
    }

    /** Repõe/estorna estoque. */
    public boolean reporEstoque(Long recompensaId, long quantidade) {
        if (recompensaId == null || quantidade <= 0) return false;
        int updated = update(
                "estoque = estoque + ?2, atualizadoEm = ?3 " +
                "where id = ?1",
                recompensaId, quantidade, LocalDateTime.now());
        return updated > 0;
    }

    // --------------------- Ativação / desativação ---------------------

    public boolean ativar(Long id) {
        if (id == null) return false;
        return update("ativo = true, atualizadoEm = ?2 where id = ?1", id, LocalDateTime.now()) > 0;
    }

    public boolean desativar(Long id) {
        if (id == null) return false;
        return update("ativo = false, atualizadoEm = ?2 where id = ?1", id, LocalDateTime.now()) > 0;
    }

    // --------------------- Métricas simples ---------------------

    public long countDisponiveis(LocalDateTime agora) {
        agora = (agora == null ? LocalDateTime.now() : agora);
        return count("ativo = true and estoque > 0 and (validadeRecompensa is null or validadeRecompensa >= ?1)", agora);
    }

    public long countVencidas(LocalDateTime agora) {
        agora = (agora == null ? LocalDateTime.now() : agora);
        return count("validadeRecompensa is not null and validadeRecompensa < ?1", agora);
    }
}
