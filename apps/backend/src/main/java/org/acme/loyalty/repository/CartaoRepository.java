package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import org.acme.loyalty.entity.Cartao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para Cartao (Panache).
 * Regras úteis:
 * - Número é único.
 * - Vencimento (validade) é um LocalDate (apenas mês/ano costumam importar no mundo real).
 * - Consultas por usuário e por “últimos 4 dígitos” são frequentes.
 *
 * Compatível com Java 17 / Quarkus 3.
 */
@ApplicationScoped
public class CartaoRepository implements PanacheRepository<Cartao> {

    private static final Sort DEFAULT_SORT =
            Sort.by("validade").ascending().and("id").ascending();

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public Cartao upsert(Cartao c) {
        if (c == null) return null;
        if (c.id == null) persist(c);
        return c;
    }

    /** Carrega entidade com lock pessimista (útil em fluxos concorrentes). */
    public Optional<Cartao> findByIdForUpdate(Long id) {
        if (id == null) return Optional.empty();
        var em = getEntityManager();
        Cartao c = em.find(Cartao.class, id, LockModeType.PESSIMISTIC_WRITE);
        return Optional.ofNullable(c);
    }

    // --------------------- Busca por atributos ---------------------

    public Optional<Cartao> findByNumero(String numero) {
        if (numero == null || numero.isBlank()) return Optional.empty();
        return find("numero = ?1", numero.trim()).firstResultOptional();
    }

    public boolean existsByNumero(String numero) {
        if (numero == null || numero.isBlank()) return false;
        return count("numero = ?1", numero.trim()) > 0;
    }

    public List<Cartao> listByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("usuario.id = ?1", usuarioId).list();
    }

    public Optional<Cartao> findByIdAndUsuario(Long cartaoId, Long usuarioId) {
        if (cartaoId == null || usuarioId == null) return Optional.empty();
        return find("id = ?1 and usuario.id = ?2", cartaoId, usuarioId).firstResultOptional();
    }

    /** Busca por “últimos 4 dígitos” usando função nativa RIGHT via function(). */
    public List<Cartao> listByLast4(String last4) {
        if (last4 == null || last4.length() != 4) return List.of();
        return find("function('right', numero, 4) = ?1", last4).list();
    }

    /** Variante por usuário + últimos 4 dígitos. */
    public Optional<Cartao> findByUsuarioAndLast4(Long usuarioId, String last4) {
        if (usuarioId == null || last4 == null || last4.length() != 4) return Optional.empty();
        return find("usuario.id = ?1 and function('right', numero, 4) = ?2", usuarioId, last4).firstResultOptional();
    }

    // --------------------- Vigência (validade) ---------------------

    /** Cartões vencidos antes de uma data (tipicamente today). */
    public List<Cartao> listarVencidosAntesDe(LocalDate data) {
        if (data == null) data = LocalDate.now();
        return find("validade < ?1", data).list();
    }

    /** Cartões válidos (validade >= data). */
    public List<Cartao> listarValidosAPartirDe(LocalDate data) {
        if (data == null) data = LocalDate.now();
        return find("validade >= ?1", data).list();
    }

    /** Próximos a vencer na janela [inicio, fim] (inclusive). */
    public List<Cartao> listarProximosAVencer(LocalDate inicio, LocalDate fim) {
        if (inicio == null) inicio = LocalDate.now();
        if (fim == null) fim = inicio.plusMonths(1);
        return find("validade >= ?1 and validade <= ?2", inicio, fim).list();
    }

    public long countProximosAVencer(LocalDate inicio, LocalDate fim) {
        if (inicio == null) inicio = LocalDate.now();
        if (fim == null) fim = inicio.plusMonths(1);
        return count("validade >= ?1 and validade <= ?2", inicio, fim);
    }

    // --------------------- JOIN FETCH úteis ---------------------

    /** Carrega cartão com o usuário (evita N+1 ao exibir dono). */
    public Optional<Cartao> findWithUsuario(Long id) {
        return find("from Cartao c left join fetch c.usuario where c.id = ?1", id).firstResultOptional();
    }

    /** Carrega cartões do usuário já com o usuário “fetched”. */
    public List<Cartao> listByUsuarioWithUsuario(Long usuarioId) {
        if (usuarioId == null) return List.of();
        return find("from Cartao c join fetch c.usuario u where u.id = ?1", usuarioId)
                .list();
    }

    // --------------------- Paginação & Busca simples ---------------------

    /**
     * Busca simples por nome impresso (like, case-insensitive) e opcionalmente por usuário.
     */
    public PanacheQuery<Cartao> queryByNomeImpresso(String termo, Long usuarioId, int page, int size) {
        String like = (termo == null || termo.isBlank()) ? null : "%" + termo.trim().toLowerCase() + "%";
        PanacheQuery<Cartao> q;
        if (like != null && usuarioId != null) {
            q = find("lower(nomeImpresso) like ?1 and usuario.id = ?2", like, usuarioId);
        } else if (like != null) {
            q = find("lower(nomeImpresso) like ?1", like);
        } else if (usuarioId != null) {
            q = find("usuario.id = ?1", usuarioId);
        } else {
            q = findAll();
        }
        return q.page(Page.of(page, size));
    }
}
