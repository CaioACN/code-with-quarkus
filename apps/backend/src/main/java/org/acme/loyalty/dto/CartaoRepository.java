package org.acme.loyalty.dto;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.acme.loyalty.entity.Cartao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CartaoRepository implements PanacheRepository<Cartao> {

    // ---- Leituras básicas ----

    public Optional<Cartao> findByNumero(String numero) {
        return find("numero", numero).firstResultOptional();
    }

    public boolean existsByNumero(String numero) {
        return count("numero", numero) > 0;
    }

    public List<Cartao> listByUsuarioId(Long usuarioId) {
        return list("usuario.id = ?1", usuarioId);
    }

    public List<Cartao> listByUsuarioId(Long usuarioId, int page, int size) {
        return find("usuario.id = ?1", Sort.ascending("id"), usuarioId)
                .page(Page.of(page, size))
                .list();
    }

    public List<Cartao> listProximosAoVencimento(int diasAntes) {
        LocalDate limite = LocalDate.now().plusDays(diasAntes);
        // validade entre hoje (>=) e hoje+diasAntes, e ainda não vencidos
        return list("validade >= ?1 and validade <= ?2", LocalDate.now(), limite);
    }

    public List<Cartao> listVencidos() {
        return list("validade < ?1", LocalDate.now());
    }

    // ---- Escritas ----

    @Transactional
    public Cartao persistir(Cartao cartao) {
        persist(cartao);
        return cartao;
    }

    @Transactional
    public Cartao atualizar(Cartao existente, Cartao dados) {
        if (dados.numero != null) existente.numero = dados.numero;
        if (dados.nomeImpresso != null) existente.nomeImpresso = dados.nomeImpresso;
        if (dados.validade != null) existente.validade = dados.validade;
        if (dados.limite != null) existente.limite = dados.limite;
        if (dados.usuario != null) existente.usuario = dados.usuario;
        return existente;
    }

    @Transactional
    public void remover(Cartao cartao) {
        delete(cartao);
    }
}
