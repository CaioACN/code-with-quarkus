package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import org.acme.loyalty.dto.CampanhaBonusRequestDTO;
import org.acme.loyalty.dto.CampanhaBonusResponseDTO;
import org.acme.loyalty.dto.CampanhaBonusUpdateDTO;
import org.acme.loyalty.entity.CampanhaBonus;
import org.acme.loyalty.repository.CampanhaBonusRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class CampanhaBonusService {

    @Inject
    CampanhaBonusRepository campanhaBonusRepository;

    // =========================================================
    // CRUD / Consulta
    // =========================================================

    @Transactional
    public CampanhaBonusResponseDTO criarCampanha(CampanhaBonusRequestDTO request) {
        validarCampanhaBonus(request);

        CampanhaBonus campanha = new CampanhaBonus(
            request.nome,
            request.multiplicadorExtra,
            request.vigenciaIni,
            request.vigenciaFim,
            request.segmento,
            request.prioridade,
            request.teto
        );

        campanhaBonusRepository.persist(campanha);
        return toCampanhaBonusResponseDTO(campanha);
    }

    public CampanhaBonusResponseDTO buscarCampanhaPorId(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));
        return toCampanhaBonusResponseDTO(campanha);
    }

  /**
 * Listagem com filtros simples e paginação em memória (caso o repositório ainda não tenha query paginada).
 * Filtros: nome (contains), segmento (match por regra da entidade), ativo (todas as campanhas), vigente (vigente hoje).
 */
public List<CampanhaBonusResponseDTO> listarCampanhas(String nome, String segmento,
Boolean ativo, Boolean vigente, Integer pagina, Integer tamanho) {
// defaults de paginação (evita NPE e valores inválidos)
final int pageIndex = (pagina == null || pagina < 0) ? 0 : pagina;
final int pageSize  = (tamanho == null || tamanho <= 0) ? 20 : tamanho;

// base: aplicar filtro de vigência se especificado
List<CampanhaBonus> base;
if (Boolean.TRUE.equals(vigente)) {
    // Filtro vigente=true: apenas campanhas vigentes hoje
    base = campanhaBonusRepository.listarVigentes(LocalDate.now());
} else if (Boolean.FALSE.equals(vigente)) {
    // Filtro vigente=false: apenas campanhas não vigentes hoje
    LocalDate hoje = LocalDate.now();
    base = campanhaBonusRepository.findAll().list().stream()
        .filter(c -> !c.estaVigenteEm(hoje))
        .collect(Collectors.toList());
} else {
    // Sem filtro de vigência: todas as campanhas
    base = campanhaBonusRepository.findAll().list();
}

// Aplicar filtro de ativo se especificado
// Como não temos campo 'ativo' na entidade, consideramos:
// ativo=true: campanhas que têm vigenciaFim null ou vigenciaFim >= hoje (não expiradas definitivamente)
// ativo=false: campanhas que têm vigenciaFim < hoje (expiradas definitivamente)
if (Boolean.TRUE.equals(ativo)) {
    LocalDate hoje = LocalDate.now();
    base = base.stream()
        .filter(c -> c.vigenciaFim == null || !c.vigenciaFim.isBefore(hoje))
        .collect(Collectors.toList());
} else if (Boolean.FALSE.equals(ativo)) {
    LocalDate hoje = LocalDate.now();
    base = base.stream()
        .filter(c -> c.vigenciaFim != null && c.vigenciaFim.isBefore(hoje))
        .collect(Collectors.toList());
}

// filtros em memória
if (nome != null && !nome.isBlank()) {
String p = nome.toLowerCase();
base = base.stream()
.filter(c -> c.nome != null && c.nome.toLowerCase().contains(p))
.collect(Collectors.toList());
}
if (segmento != null && !segmento.isBlank()) {
String seg = segmento.trim();
base = base.stream()
.filter(c -> c.aplicaParaSegmento(seg))
.collect(Collectors.toList());
}

// ordena por prioridade (menor número = maior prioridade), depois por vigência (nulls last)
base = base.stream()
.sorted(java.util.Comparator
.comparing((CampanhaBonus c) -> c.prioridade == null ? Integer.MAX_VALUE : c.prioridade)
.thenComparing(c -> c.vigenciaIni,
java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
.collect(Collectors.toList());

// paginação em memória
int from = Math.min(pageIndex * pageSize, base.size());
int to   = Math.min(from + pageSize, base.size());
if (from >= to) {
return List.of();
}

return base.subList(from, to).stream()
.map(this::toCampanhaBonusResponseDTO)
.collect(Collectors.toList());
}


    @Transactional
    public CampanhaBonusResponseDTO atualizarCampanha(Long id, CampanhaBonusUpdateDTO request) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        if (request.nome != null) campanha.nome = request.nome;
        if (request.multiplicadorExtra != null) campanha.multiplicadorExtra = request.multiplicadorExtra;
        if (request.vigenciaIni != null) campanha.vigenciaIni = request.vigenciaIni;
        if (request.vigenciaFim != null) campanha.vigenciaFim = request.vigenciaFim;
        if (request.segmento != null) campanha.segmento = request.segmento;
        if (request.prioridade != null) campanha.prioridade = request.prioridade;
        if (request.teto != null) campanha.teto = request.teto;

        // valida consistência de datas ao atualizar
        if (campanha.vigenciaIni != null && campanha.vigenciaFim != null
                && campanha.vigenciaIni.isAfter(campanha.vigenciaFim)) {
            throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
        }

        campanhaBonusRepository.persist(campanha);
        return toCampanhaBonusResponseDTO(campanha);
    }

    @Transactional
    public void deletarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));
        // Validar referência em regras/execuções antes de excluir (se necessário)
        campanhaBonusRepository.delete(campanha);
    }

    @Transactional
    public CampanhaBonusResponseDTO ativarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        LocalDate hoje = LocalDate.now();
        
        // Ajustar data de início se necessário
        if (campanha.vigenciaIni == null || campanha.vigenciaIni.isAfter(hoje)) {
            campanha.vigenciaIni = hoje;
        }
        
        // Definir data de fim como hoje ao ativar a campanha
        campanha.vigenciaFim = hoje;
        
        campanhaBonusRepository.persist(campanha);
        return toCampanhaBonusResponseDTO(campanha);
    }

    @Transactional
    public CampanhaBonusResponseDTO desativarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        campanha.vigenciaFim = LocalDate.now().minusDays(1);
        campanhaBonusRepository.persist(campanha);
        return toCampanhaBonusResponseDTO(campanha);
    }

    public List<CampanhaBonusResponseDTO> listarCampanhasAtivas() {
        return campanhaBonusRepository.listarVigentes(LocalDate.now()).stream()
                .map(this::toCampanhaBonusResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Status resumido da campanha: se está vigente hoje, dias restantes, janela de vigência etc.
     * (Sem métricas históricas de “uso” porque não há vínculo direto com movimentos no modelo atual.)
     */
    public Map<String, Object> consultarStatusCampanha(Long id) {
        CampanhaBonus c = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        boolean vigenteHoje = vigente(c, LocalDate.now());
        Integer diasRestantes = null;
        if (vigenteHoje && c.vigenciaFim != null) {
            diasRestantes = Math.max(0, (int) (c.vigenciaFim.toEpochDay() - LocalDate.now().toEpochDay()));
        }

        return Map.of(
                "id", c.id,
                "nome", c.nome,
                "vigenteHoje", vigenteHoje,
                "vigenciaIni", c.vigenciaIni,
                "vigenciaFim", c.vigenciaFim,
                "segmento", c.segmento,
                "prioridade", c.prioridade,
                "teto", c.teto,
                "multiplicadorExtra", c.multiplicadorExtra,
                "diasRestantes", diasRestantes
        );
    }

    // =========================================================
    // Regras de negócio (seleção/aplicação de bônus)
    // =========================================================

    /**
     * Retorna a MELHOR campanha vigente para o segmento e data informados.
     * Critério: campanhas vigentes na data, cujo segmento “bate” (aplicaParaSegmento),
     * ordenadas por prioridade ASC (menor número = maior prioridade); se empatar, pega a de maior multiplicador.
     */
    public CampanhaBonus escolherMelhorCampanha(String segmento, LocalDate dataRef) {
        LocalDate data = (dataRef == null ? LocalDate.now() : dataRef);
        String seg = segmento == null ? null : segmento.trim();

        List<CampanhaBonus> candidatas = campanhaBonusRepository.listarVigentes(data).stream()
                .filter(c -> seg == null || seg.isBlank() || c.aplicaParaSegmento(seg))
                .collect(Collectors.toList());

        return candidatas.stream()
                .sorted(Comparator
                        .comparing((CampanhaBonus c) -> c.prioridade == null ? Integer.MAX_VALUE : c.prioridade)
                        .thenComparing(c -> c.multiplicadorExtra == null ? BigDecimal.ZERO : c.multiplicadorExtra, Comparator.reverseOrder())
                )
                .findFirst()
                .orElse(null);
    }

    /**
     * Fator efetivo de pontos: 1.0 + multiplicadorExtra da melhor campanha vigente (se houver).
     * Ex.: se multiplicadorExtra = 0.5 → fator = 1.5
     */
    public BigDecimal fatorBonusEfetivo(String segmento, LocalDate dataRef) {
        CampanhaBonus melhor = escolherMelhorCampanha(segmento, dataRef);
        if (melhor == null || melhor.multiplicadorExtra == null) {
            return BigDecimal.ONE;
        }
        return BigDecimal.ONE.add(melhor.multiplicadorExtra);
    }

    /**
     * Aplica o fator de bônus sobre pontos base, respeitando o teto da campanha (se houver).
     * Retorna os PONTOS FINAIS (já com bônus). Clampa para Integer (modelo atual).
     */
    public int aplicarBonusEmPontos(int pontosBase, String segmento, LocalDate dataRef) {
        if (pontosBase <= 0) return 0;

        CampanhaBonus melhor = escolherMelhorCampanha(segmento, dataRef);
        if (melhor == null || melhor.multiplicadorExtra == null) {
            return pontosBase; // sem bônus
        }

        // fator = 1 + extra
        BigDecimal fator = BigDecimal.ONE.add(melhor.multiplicadorExtra);
        BigDecimal bruto = fator.multiply(BigDecimal.valueOf(pontosBase));

        long finalLong = bruto.setScale(0, java.math.RoundingMode.HALF_UP).longValue();

        // aplica teto por transação se houver (modelo simplificado; teto_mensal exigiria outro modelo/consulta)
        if (melhor.teto != null && melhor.teto > 0) {
            finalLong = Math.min(finalLong, melhor.teto.longValue());
        }

        if (finalLong > Integer.MAX_VALUE) finalLong = Integer.MAX_VALUE;
        return (int) finalLong;
    }

    /**
     * Retorna campanhas vigentes por segmento (hoje), ordenadas por prioridade.
     */
    public List<CampanhaBonusResponseDTO> listarCampanhasVigentesPorSegmento(String segmento) {
        String seg = segmento == null ? null : segmento.trim();
        return campanhaBonusRepository.listarVigentes(LocalDate.now()).stream()
                .filter(c -> seg == null || seg.isBlank() || c.aplicaParaSegmento(seg))
                .sorted(Comparator.comparing(c -> c.prioridade == null ? Integer.MAX_VALUE : c.prioridade))
                .map(this::toCampanhaBonusResponseDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // Validação / Helpers
    // =========================================================

    private void validarCampanhaBonus(CampanhaBonusRequestDTO request) {
        if (request == null) throw new IllegalArgumentException("Dados da campanha são obrigatórios");

        if (request.nome == null || request.nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da campanha é obrigatório");
        }
        if (request.multiplicadorExtra == null || request.multiplicadorExtra.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Multiplicador extra deve ser maior que zero");
        }

        if (request.vigenciaIni != null && request.vigenciaFim != null
                && request.vigenciaIni.isAfter(request.vigenciaFim)) {
            throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
        }

        if (request.prioridade != null && request.prioridade < 0) {
            throw new IllegalArgumentException("Prioridade deve ser maior ou igual a zero");
        }

        if (request.teto != null && request.teto <= 0) {
            throw new IllegalArgumentException("Teto deve ser maior que zero");
        }
    }

    private CampanhaBonusResponseDTO toCampanhaBonusResponseDTO(CampanhaBonus campanha) {
        return CampanhaBonusResponseDTO.fromEntity(campanha);
    }

    /** Vigente se data ∈ [ini, fim] considerando null como aberto. */
    private boolean vigente(CampanhaBonus c, LocalDate data) {
        if (c == null) return false;
        LocalDate d = (data == null ? LocalDate.now() : data);
        boolean geIni = (c.vigenciaIni == null) || !d.isBefore(c.vigenciaIni);
        boolean leFim = (c.vigenciaFim == null) || !d.isAfter(c.vigenciaFim);
        return geIni && leFim;
    }
}
