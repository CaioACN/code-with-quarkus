package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.RegraConversao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para RegraConversao (Panache).
 * Regras principais:
 * - Prioridade determina ordem de aplicação
 * - Vigência controla período de aplicação
 * - MCC e categoria são filtros opcionais
 */
@ApplicationScoped
public class RegraConversaoRepository implements PanacheRepository<RegraConversao> {

    // --------------------- CRUD helpers ---------------------

    /** Persiste se id == null; caso contrário retorna a entidade gerenciada. */
    public RegraConversao upsert(RegraConversao rc) {
        if (rc == null) return null;
        if (rc.id == null) persist(rc);
        return rc;
    }

    // --------------------- Busca por atributos ---------------------

    public Optional<RegraConversao> findByNome(String nome) {
        if (nome == null || nome.isBlank()) return Optional.empty();
        return find("nome = ?1", nome.trim()).firstResultOptional();
    }

    public List<RegraConversao> listByMcc(String mcc) {
        if (mcc == null || mcc.isBlank()) return List.of();
        return find("mcc = ?1", mcc.trim()).list();
    }

    public List<RegraConversao> listByCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) return List.of();
        return find("lower(categoria) like ?1", "%" + categoria.trim().toLowerCase() + "%").list();
    }

    public List<RegraConversao> listByParceiroId(Long parceiroId) {
        if (parceiroId == null) return List.of();
        return find("parceiroId = ?1", parceiroId).list();
    }

    public List<RegraConversao> listByPrioridade(Integer prioridade) {
        if (prioridade == null) return List.of();
        return find("prioridade = ?1", prioridade).list();
    }

    // --------------------- Vigência ---------------------

    /** Regras vigentes na data informada. */
    public List<RegraConversao> listarVigentes(LocalDate data) {
        if (data == null) data = LocalDate.now();
        return find("vigenciaIni <= ?1 and (vigenciaFim is null or vigenciaFim >= ?1)", data, data).list();
    }

    /** Regras vencidas antes da data. */
    public List<RegraConversao> listarVencidasAntesDe(LocalDate data) {
        if (data == null) data = LocalDate.now();
        return find("vigenciaFim < ?1", data).list();
    }

    /** Regras que vencem na janela [inicio, fim]. */
    public List<RegraConversao> listarVencendoNaJanela(LocalDate inicio, LocalDate fim) {
        if (inicio == null) inicio = LocalDate.now();
        if (fim == null) fim = inicio.plusMonths(1);
        return find("vigenciaFim >= ?1 and vigenciaFim <= ?2", inicio, fim).list();
    }

    // --------------------- Regras aplicáveis conforme regra 17.4 ---------------------
    
    /**
     * Busca regras aplicáveis para transação conforme regra 17.4:
     * - Seleção por vigência e prioridade (maior primeiro)
     * - Escopo por mcc_regex / categoria / parceiro_id
     */
    public List<RegraConversao> listarAplicaveisParaTransacao(String mcc, String categoria, Long parceiroId, LocalDate data) {
        if (data == null) data = LocalDate.now();
        
        StringBuilder query = new StringBuilder();
        var params = new java.util.ArrayList<>();
        int paramIndex = 1;

        // Vigência obrigatória
        query.append("ativo = true and vigenciaIni <= ?").append(paramIndex++);
        params.add(data);
        query.append(" and (vigenciaFim is null or vigenciaFim >= ?").append(paramIndex++);
        params.add(data);
        query.append(")");

        // Filtros opcionais
        if (mcc != null && !mcc.isBlank()) {
            query.append(" and (mccRegex is null or mccRegex = '' or mcc ~ mccRegex)");
        }
        
        if (categoria != null && !categoria.isBlank()) {
            query.append(" and (categoria is null or categoria = '' or lower(categoria) = lower(?").append(paramIndex++);
            params.add(categoria.trim());
            query.append("))");
        }
        
        if (parceiroId != null) {
            query.append(" and (parceiroId is null or parceiroId = ?").append(paramIndex++);
            params.add(parceiroId);
            query.append(")");
        }

        // Ordenação por prioridade (maior primeiro) e especificidade
        query.append(" order by prioridade desc, " +
                    "case when parceiroId is not null then 4 " +
                    "     when categoria is not null and categoria != '' then 3 " +
                    "     when mccRegex is not null and mccRegex != '' then 2 " +
                    "     else 1 end desc, id asc");

        return find(query.toString(), params.toArray()).list();
    }
    
    /**
     * Seleciona a regra mais prioritária conforme regra 17.4:
     * - Maior prioridade primeiro
     * - Empate → a mais específica (parceiro_id > categoria > mcc_regex > geral)
     */
    public Optional<RegraConversao> selecionarRegraMaisPrioritaria(String mcc, String categoria, Long parceiroId, LocalDate data) {
        List<RegraConversao> aplicaveis = listarAplicaveisParaTransacao(mcc, categoria, parceiroId, data);
        return aplicaveis.stream().findFirst();
    }

    /** Regras aplicáveis para MCC e categoria específicos na data. */
    public List<RegraConversao> listarAplicaveis(String mcc, String categoria, LocalDate data) {
        if (data == null) data = LocalDate.now();
        
        StringBuilder query = new StringBuilder();
        var params = new java.util.ArrayList<>();
        int paramIndex = 1;

        // Vigência
        query.append("vigenciaIni <= ?").append(paramIndex++);
        params.add(data);
        query.append(" and (vigenciaFim is null or vigenciaFim >= ?").append(paramIndex++);
        params.add(data);
        query.append(")");

        // MCC (se informado)
        if (mcc != null && !mcc.isBlank()) {
            query.append(" and (mcc is null or mcc = ?").append(paramIndex++);
            params.add(mcc.trim());
            query.append(")");
        }

        // Categoria (se informada)
        if (categoria != null && !categoria.isBlank()) {
            query.append(" and (categoria is null or lower(categoria) = lower(?").append(paramIndex++);
            params.add(categoria.trim());
            query.append("))");
        }

        return find(query.toString(), params.toArray()).list();
    }

    /** Primeira regra aplicável (mais prioritária) para MCC e categoria. */
    public Optional<RegraConversao> selecionarMaisPrioritaria(String mcc, String categoria, LocalDate data) {
        if (data == null) data = LocalDate.now();
        
        StringBuilder query = new StringBuilder();
        var params = new java.util.ArrayList<>();
        int paramIndex = 1;

        // Vigência
        query.append("vigenciaIni <= ?").append(paramIndex++);
        params.add(data);
        query.append(" and (vigenciaFim is null or vigenciaFim >= ?").append(paramIndex++);
        params.add(data);
        query.append(")");

        // MCC (se informado)
        if (mcc != null && !mcc.isBlank()) {
            query.append(" and (mcc is null or mcc = ?").append(paramIndex++);
            params.add(mcc.trim());
            query.append(")");
        }

        // Categoria (se informada)
        if (categoria != null && !categoria.isBlank()) {
            query.append(" and (categoria is null or lower(categoria) = lower(?").append(paramIndex++);
            params.add(categoria.trim());
            query.append("))");
        }

        // Ordenação por prioridade (maior primeiro)
        query.append(" order by prioridade desc, id asc");

        return find(query.toString(), params.toArray()).firstResultOptional();
    }

    // --------------------- Validações de negócio ---------------------

    /** Verifica se existe regra com mesmo nome (case-insensitive), exceto o id informado. */
    public boolean existsByNomeIgnoringId(String nome, Long exceptId) {
        if (nome == null || nome.isBlank()) return false;
        if (exceptId == null) {
            return count("lower(nome) = ?1", nome.trim().toLowerCase()) > 0;
        }
        return count("lower(nome) = ?1 and id <> ?2", nome.trim().toLowerCase(), exceptId) > 0;
    }

    /** Verifica se existe regra com mesmo MCC e categoria na mesma vigência. */
    public boolean existsByMccAndCategoriaAndVigencia(String mcc, String categoria, LocalDate inicio, LocalDate fim) {
        if (mcc == null || mcc.isBlank()) return false;
        if (categoria == null || categoria.isBlank()) return false;
        
        StringBuilder query = new StringBuilder();
        var params = new java.util.ArrayList<>();
        int paramIndex = 1;

        query.append("mcc = ?").append(paramIndex++);
        params.add(mcc.trim());
        query.append(" and lower(categoria) = lower(?").append(paramIndex++);
        params.add(categoria.trim());
        query.append(")");

        if (inicio != null) {
            query.append(" and vigenciaIni <= ?").append(paramIndex++);
            params.add(fim != null ? fim : inicio);
        }

        if (fim != null) {
            query.append(" and (vigenciaFim is null or vigenciaFim >= ?").append(paramIndex++);
            params.add(inicio != null ? inicio : LocalDate.now());
            query.append(")");
        }

        return count(query.toString(), params.toArray()) > 0;
    }

    // --------------------- Estatísticas e agregações ---------------------

    public long countRegrasVigentes(LocalDate data) {
        if (data == null) data = LocalDate.now();
        return count("vigenciaIni <= ?1 and (vigenciaFim is null or vigenciaFim >= ?1)", data, data);
    }

    public long countRegrasPorPrioridade(Integer prioridade) {
        if (prioridade == null) return 0;
        return count("prioridade = ?1", prioridade);
    }

    public BigDecimal calcularMultiplicadorMedio() {
        Object result = find("select avg(multiplicador) from RegraConversao").firstResult();
        return result != null ? (BigDecimal) result : BigDecimal.ZERO;
    }

    // --------------------- Paginação & Busca avançada ---------------------

    /**
     * Busca avançada com filtros opcionais e paginação.
     */
    public PanacheQuery<RegraConversao> queryAvancada(String nome, String mcc, String categoria, 
                                                      Long parceiroId, Integer prioridade,
                                                      LocalDate vigenciaIni, LocalDate vigenciaFim,
                                                      int page, int size) {
        StringBuilder query = new StringBuilder();
        var params = new java.util.ArrayList<>();
        int paramIndex = 1;

        if (nome != null && !nome.isBlank()) {
            query.append("lower(nome) like ?").append(paramIndex++);
            params.add("%" + nome.trim().toLowerCase() + "%");
        }

        if (mcc != null && !mcc.isBlank()) {
            if (query.length() > 0) query.append(" and ");
            query.append("mcc = ?").append(paramIndex++);
            params.add(mcc.trim());
        }

        if (categoria != null && !categoria.isBlank()) {
            if (query.length() > 0) query.append(" and ");
            query.append("lower(categoria) like ?").append(paramIndex++);
            params.add("%" + categoria.trim().toLowerCase() + "%");
        }

        if (parceiroId != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("parceiroId = ?").append(paramIndex++);
            params.add(parceiroId);
        }

        if (prioridade != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("prioridade = ?").append(paramIndex++);
            params.add(prioridade);
        }

        if (vigenciaIni != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("vigenciaIni >= ?").append(paramIndex++);
            params.add(vigenciaIni);
        }

        if (vigenciaFim != null) {
            if (query.length() > 0) query.append(" and ");
            query.append("vigenciaFim <= ?").append(paramIndex++);
            params.add(vigenciaFim);
        }

        String finalQuery = query.length() > 0 ? query.toString() : null;
        PanacheQuery<RegraConversao> panacheQuery;
        
        if (finalQuery != null) {
            panacheQuery = find(finalQuery, params.toArray());
        } else {
            panacheQuery = findAll();
        }

        return panacheQuery.page(Page.of(page, size));
    }
}
