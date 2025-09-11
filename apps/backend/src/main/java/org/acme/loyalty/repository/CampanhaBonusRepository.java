package org.acme.loyalty.repository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.loyalty.entity.CampanhaBonus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository de CampanhaBonus (Panache).
 * Regras principais:
 * - "Vigente" => vigenciaIni <= data AND (vigenciaFim IS NULL OR vigenciaFim >= data)
 * - "Aplicável ao segmento" => segmento IS NULL/blank OU equalsIgnoreCase(segmentoUsuario)
 * - Ordenação padrão: prioridade DESC, vigenciaIni DESC, id ASC
 */
@ApplicationScoped
public class CampanhaBonusRepository implements PanacheRepository<CampanhaBonus> {

    // private static final Sort DEFAULT_SORT =
    //         Sort.by("prioridade").descending()
    //             .and("vigenciaIni").descending()
    //             .and("id").ascending();

    // --------------------- Consultas básicas ---------------------

    /** Todas as campanhas vigentes na data informada. */
    public List<CampanhaBonus> listarVigentes(LocalDate data) {
        return find(vigenciaPredicate(), data, data).list();
    }

    /** Campanhas vigentes e aplicáveis ao segmento (segmento null/blank => aplica a todos). */
    public List<CampanhaBonus> listarAplicaveis(String segmentoUsuario, LocalDate data) {
        return find(vigenciaPredicate() + " and (" + segmentoPredicate() + ")", data, data, nvl(segmentoUsuario))
                .list();
    }

    /** Primeira (mais prioritária) campanha aplicável ao segmento na data. */
    public Optional<CampanhaBonus> selecionarMaisPrioritaria(String segmentoUsuario, LocalDate data) {
        return find(vigenciaPredicate() + " and (" + segmentoPredicate() + ")",
                    data, data, nvl(segmentoUsuario))
                .firstResultOptional();
    }

    /** Consulta paginada de vigentes (com sort padrão). */
    public PanacheQuery<CampanhaBonus> queryVigentes(LocalDate data, int pageIndex, int pageSize) {
        return find(vigenciaPredicate(), data, data)
                .page(Page.of(pageIndex, pageSize));
    }

    // --------------------- Janelas de vigência ---------------------

    /**
     * Lista campanhas que tenham QUALQUER sobreposição com a janela [inicio, fim].
     * Se fim == null, considera todas com início <= inicio (útil p/ auditoria/histórico).
     */
    public List<CampanhaBonus> listarPorJanela(LocalDate inicio, LocalDate fim) {
        if (fim == null) {
            // vigenciaIni <= inicio
            return find("vigenciaIni <= ?1", inicio).list();
        }
        // Sobreposição: vigenciaIni <= fim AND (vigenciaFim IS NULL OR vigenciaFim >= inicio)
        return find("vigenciaIni <= ?1 and (vigenciaFim is null or vigenciaFim >= ?2)", fim, inicio)
                .list();
    }

    // --------------------- Utilidades ---------------------

    /** Upsert simples: persiste se id null, senão apenas retorna a entidade (JPA gerencia). */
    public CampanhaBonus upsert(CampanhaBonus c) {
        if (c == null) return null;
        if (c.id == null) {
            persist(c);
        }
        return c;
    }

    /** True se existe outra campanha com mesmo nome (case-insensitive), exceto o id informado. */
    public boolean existsByNomeIgnoringId(String nome, Long exceptId) {
        if (nome == null || nome.isBlank()) return false;
        if (exceptId == null) {
            return count("lower(nome) = ?1", nome.trim().toLowerCase()) > 0;
        }
        return count("lower(nome) = ?1 and id <> ?2", nome.trim().toLowerCase(), exceptId) > 0;
    }

    // --------------------- Predicados / helpers ---------------------

    private static String vigenciaPredicate() {
        // :1 = data, :2 = data (ordem posicional usada em find)
        return "vigenciaIni <= ?1 and (vigenciaFim is null or vigenciaFim >= ?2)";
    }

    private static String segmentoPredicate() {
        // :3 = segmento normalizado
        return "segmento is null or trim(segmento) = '' or lower(segmento) = lower(?3)";
    }

    private static String nvl(String s) {
        return (s == null) ? "" : s.trim();
    }
}
