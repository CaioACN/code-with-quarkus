package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Repository para Usuario (Panache).
 * Regras principais:
 * - Email é único
 * - Relacionamentos com cartões, transações e pontos
 * - Consultas por email e nome são frequentes
 */
@ApplicationScoped
public class UsuarioRepository implements PanacheRepository<Usuario> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public Usuario upsert(Usuario u) {
        if (u == null) return null;
        if (u.id == null) persist(u);
        return u;
    }

    // --------------------- Busca por atributos ---------------------

    public Optional<Usuario> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return find("email = ?1", email.trim().toLowerCase()).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return count("email = ?1", email.trim().toLowerCase()) > 0;
    }

    public List<Usuario> listByNome(String nome) {
        if (nome == null || nome.isBlank()) return List.of();
        return find("lower(nome) like ?1", "%" + nome.trim().toLowerCase() + "%").list();
    }

    public List<Usuario> listByNomeExato(String nome) {
        if (nome == null || nome.isBlank()) return List.of();
        return find("nome = ?1", nome.trim()).list();
    }

    // --------------------- JOIN FETCH úteis ---------------------

    /** Carrega usuário com cartões (evita N+1 ao exibir cartões). */
    public Optional<Usuario> findWithCartoes(Long id) {
        return find("from Usuario u left join fetch u.cartoes where u.id = ?1", id).firstResultOptional();
    }

    /** Carrega usuário com saldos de pontos (evita N+1 ao exibir saldos). */
    public Optional<Usuario> findWithSaldosPontos(Long id) {
        return find("from Usuario u left join fetch u.saldosPontos where u.id = ?1", id).firstResultOptional();
    }

    /** Carrega usuário com transações (evita N+1 ao exibir histórico). */
    public Optional<Usuario> findWithTransacoes(Long id) {
        return find("from Usuario u left join fetch u.transacoes where u.id = ?1", id).firstResultOptional();
    }

    // --------------------- Paginação & Busca simples ---------------------

    /**
     * Busca simples por nome (like, case-insensitive) e opcionalmente por email.
     */
    public List<Usuario> queryByNomeAndEmail(String termo, String email, int page, int size) {
        String like = (termo == null || termo.isBlank()) ? null : "%" + termo.trim().toLowerCase() + "%";
        String emailFilter = (email == null || email.isBlank()) ? null : email.trim().toLowerCase();
        
        if (like != null && emailFilter != null) {
            return find("lower(nome) like ?1 and email = ?2", like, emailFilter)
                    .page(Page.of(page, size)).list();
        } else if (like != null) {
            return find("lower(nome) like ?1", like)
                    .page(Page.of(page, size)).list();
        } else if (emailFilter != null) {
            return find("email = ?1", emailFilter)
                    .page(Page.of(page, size)).list();
        } else {
            return findAll().page(Page.of(page, size)).list();
        }
    }

    // --------------------- Validações de negócio ---------------------

    /** Verifica se o usuário pode ser excluído (sem relacionamentos ativos). */
    public boolean podeSerExcluido(Long id) {
        if (id == null) return false;
        
        // Verifica se tem cartões ativos
        long cartoesAtivos = count("usuario.id = ?1", id);
        if (cartoesAtivos > 0) return false;
        
        // Verifica se tem transações
        long transacoes = count("usuario.id = ?1", id);
        if (transacoes > 0) return false;
        
        // Verifica se tem movimentos de pontos
        long movimentos = count("usuario.id = ?1", id);
        if (movimentos > 0) return false;
        
        return true;
    }

    /** Conta usuários ativos (com cartões válidos). */
    public long countUsuariosAtivos() {
        return count("exists (select 1 from Cartao c where c.usuario.id = id and c.validade >= current_date)");
    }

    /** Lista usuários inativos (sem cartões válidos). */
    public List<Usuario> listarUsuariosInativos() {
        return find("not exists (select 1 from Cartao c where c.usuario.id = id and c.validade >= current_date)").list();
    }
}
