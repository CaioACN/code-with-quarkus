package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.RegraConversaoRequestDTO;
import org.acme.loyalty.dto.RegraConversaoResponseDTO;
import org.acme.loyalty.dto.RegraConversaoUpdateDTO;
import org.acme.loyalty.entity.RegraConversao;
import org.acme.loyalty.repository.RegraConversaoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@ApplicationScoped
public class RegraConversaoService {

    @Inject
    RegraConversaoRepository regraConversaoRepository;

    // ===================== CRUD =====================

    @Transactional
    public RegraConversaoResponseDTO criarRegra(RegraConversaoRequestDTO request) {
        validarRegraConversao(request);

        // Unicidade por nome (case-insensitive)
        if (regraConversaoRepository.existsByNomeIgnoringId(request.nome, null)) {
            throw new IllegalArgumentException("Já existe uma regra com esse nome");
        }

        // (Opcional) Bloquear sobreposição exata MCC/Categoria/Vigência
        if (request.mccRegex == null && request.categoria != null && request.vigenciaIni != null) {
            // LocalDate ini = request.vigenciaIni.toLocalDate();
            // LocalDate fim = (request.vigenciaFim != null ? request.vigenciaFim.toLocalDate() : null);
            // Implementar verificação de sobreposição quando necessário
            // regraConversaoRepository.existsByMccAndCategoriaAndVigencia(...);
        }

        RegraConversao regra = request.toEntity();
        regra.criadoEm = LocalDateTime.now();
        regra.atualizadoEm = regra.criadoEm;

        regraConversaoRepository.persist(regra);
        return toDTO(regra);
    }

    public RegraConversaoResponseDTO buscarRegraPorId(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));
        return toDTO(regra);
    }

    /**
     * Lista regras com filtros simples.
     * O repositório expõe queryAvancada(nome, mcc, categoria, parceiroId, prioridade, vigIni, vigFim, page, size).
     * Aqui não há filtro nativo por "ativo" no repo, então aplicamos em memória se informado.
     */
    public List<RegraConversaoResponseDTO> listarRegras(String nome,
                                                        String categoria,
                                                        Long parceiroId,
                                                        Boolean ativo,
                                                        Integer pagina,
                                                        Integer tamanho) {
        final int pageIndex = (pagina == null || pagina < 1) ? 0 : (pagina - 1);
        final int pageSize  = (tamanho == null || tamanho < 1) ? 20 : tamanho;

        List<RegraConversao> lista = regraConversaoRepository
                .queryAvancada(
                        nome,
                        null,                 // mcc (não exposto neste endpoint)
                        categoria,
                        parceiroId,
                        null,                 // prioridade
                        null,                 // vigenciaIni (LocalDate)
                        null,                 // vigenciaFim  (LocalDate)
                        pageIndex,
                        pageSize
                )
                .list();

        if (ativo != null) {
            lista = lista.stream()
                    .filter(r -> Objects.equals(r.ativo, ativo))
                    .collect(Collectors.toList());
        }

        return lista.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public RegraConversaoResponseDTO atualizarRegra(Long id, RegraConversaoUpdateDTO request) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));

        if (request.nome != null && !request.nome.isBlank()
                && !request.nome.equalsIgnoreCase(regra.nome)) {
            if (regraConversaoRepository.existsByNomeIgnoringId(request.nome, id)) {
                throw new IllegalArgumentException("Já existe uma regra com esse nome");
            }
            regra.nome = request.nome;
        }
        if (request.multiplicador != null)  regra.multiplicador = request.multiplicador;
        if (request.mccRegex != null)       regra.mccRegex      = request.mccRegex;
        if (request.categoria != null)      regra.categoria     = request.categoria;
        if (request.parceiroId != null)     regra.parceiroId    = request.parceiroId;
        if (request.vigenciaIni != null)    regra.vigenciaIni   = request.vigenciaIni;
        if (request.vigenciaFim != null)    regra.vigenciaFim   = request.vigenciaFim;
        if (request.prioridade != null)     regra.prioridade    = request.prioridade;
        if (request.tetoMensal != null)     regra.tetoMensal    = request.tetoMensal;
        if (request.ativo != null)          regra.ativo         = request.ativo;

        // Revalida combinação temporal se ambos presentes
        if (regra.vigenciaIni != null && regra.vigenciaFim != null
                && regra.vigenciaFim.isBefore(regra.vigenciaIni)) {
            throw new IllegalArgumentException("vigenciaFim deve ser maior que vigenciaIni");
        }

        regra.atualizadoEm = LocalDateTime.now();
        regraConversaoRepository.persist(regra);

        return toDTO(regra);
    }

    @Transactional
    public void deletarRegra(Long id) {
        boolean ok = regraConversaoRepository.deleteById(id);
        if (!ok) {
            throw new NotFoundException("Regra de conversão não encontrada: " + id);
        }
    }

    // ===================== Ativação =====================

    @Transactional
    public RegraConversaoResponseDTO ativarRegra(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));
        regra.ativo = Boolean.TRUE;
        regra.atualizadoEm = LocalDateTime.now();
        regraConversaoRepository.persist(regra);
        return toDTO(regra);
    }

    @Transactional
    public RegraConversaoResponseDTO desativarRegra(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));
        regra.ativo = Boolean.FALSE;
        regra.atualizadoEm = LocalDateTime.now();
        regraConversaoRepository.persist(regra);
        return toDTO(regra);
    }

    // ===================== Detalhes (placeholder) =====================

    public Object consultarDetalhesAplicacao(Long id) {
        regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));
        // Agregar métricas de aplicação da regra (transações, pontos, etc.) quando necessário
        return null;
    }

    // ===================== Validações / Helpers =====================

    private void validarRegraConversao(RegraConversaoRequestDTO req) {
        if (req.nome == null || req.nome.isBlank()) {
            throw new IllegalArgumentException("Nome da regra é obrigatório");
        }
        if (req.multiplicador == null || req.multiplicador.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Multiplicador deve ser maior que zero");
        }
        if (req.prioridade == null || req.prioridade < 0) {
            throw new IllegalArgumentException("Prioridade deve ser maior ou igual a zero");
        }
        if (req.vigenciaIni == null) {
            throw new IllegalArgumentException("vigenciaIni é obrigatória");
        }
        if (req.vigenciaFim != null && req.vigenciaFim.isBefore(req.vigenciaIni)) {
            throw new IllegalArgumentException("vigenciaFim deve ser maior que vigenciaIni");
        }
        if (req.tetoMensal != null && req.tetoMensal <= 0) {
            throw new IllegalArgumentException("Teto mensal deve ser maior que zero");
        }
    }

    private RegraConversaoResponseDTO toDTO(RegraConversao r) {
        RegraConversaoResponseDTO dto = new RegraConversaoResponseDTO();
        dto.id            = r.id;
        dto.nome          = r.nome;
        dto.multiplicador = r.multiplicador;
        dto.mccRegex      = r.mccRegex;
        dto.categoria     = r.categoria;
        dto.parceiroId    = r.parceiroId;
        dto.vigenciaIni   = r.vigenciaIni;
        dto.vigenciaFim   = r.vigenciaFim;
        dto.prioridade    = r.prioridade;
        dto.tetoMensal    = r.tetoMensal;
        dto.ativo         = r.ativo;
        dto.criadoEm      = r.criadoEm;
        dto.atualizadoEm  = r.atualizadoEm;
        return dto;
    }
}
