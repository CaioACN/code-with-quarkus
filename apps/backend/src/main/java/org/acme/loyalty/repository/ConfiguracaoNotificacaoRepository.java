package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.ConfiguracaoNotificacao;

import java.util.Optional;

@ApplicationScoped
public class ConfiguracaoNotificacaoRepository implements PanacheRepository<ConfiguracaoNotificacao> {

    /**
     * Busca configuração de notificação por ID do usuário
     */
    public Optional<ConfiguracaoNotificacao> findByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return Optional.empty();
        return find("usuario.id = ?1", usuarioId).firstResultOptional();
    }

    /**
     * Cria ou atualiza configuração de notificação
     */
    public ConfiguracaoNotificacao upsert(ConfiguracaoNotificacao config) {
        if (config == null) return null;
        if (config.id == null) {
            persist(config);
        }
        return config;
    }

    /**
     * Remove configuração de notificação por ID do usuário
     */
    public boolean deleteByUsuarioId(Long usuarioId) {
        if (usuarioId == null) return false;
        return delete("usuario.id = ?1", usuarioId) > 0;
    }
}