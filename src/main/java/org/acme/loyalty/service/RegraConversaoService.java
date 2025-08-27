package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.RegraConversaoRequestDTO;
import org.acme.loyalty.dto.RegraConversaoResponseDTO;
import org.acme.loyalty.dto.RegraConversaoUpdateDTO;
import org.acme.loyalty.dto.PageRequestDTO;
import org.acme.loyalty.entity.RegraConversao;
import org.acme.loyalty.repository.RegraConversaoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class RegraConversaoService {

    @Inject
    RegraConversaoRepository regraConversaoRepository;

    @Transactional
    public RegraConversaoResponseDTO criarRegra(RegraConversaoRequestDTO request) {
        // Validar dados da regra
        validarRegraConversao(request);

        // Criar nova regra
        RegraConversao regra = new RegraConversao();
        regra.nome = request.nome;
        regra.multiplicador = request.multiplicador;
        regra.mccRegex = request.mccRegex;
        regra.categoria = request.categoria;
        regra.parceiroId = request.parceiroId;
        regra.vigenciaIni = request.vigenciaIni;
        regra.vigenciaFim = request.vigenciaFim;
        regra.prioridade = request.prioridade;
        regra.tetoMensal = request.tetoMensal;
        regra.ativo = true;
        regra.criadoEm = LocalDateTime.now();
        regra.atualizadoEm = LocalDateTime.now();

        // Persistir regra
        regraConversaoRepository.persist(regra);

        return toRegraConversaoResponseDTO(regra);
    }

    public RegraConversaoResponseDTO buscarRegraPorId(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));

        return toRegraConversaoResponseDTO(regra);
    }

    public List<RegraConversaoResponseDTO> listarRegras(String nome, String categoria, 
                                                       Long parceiroId, Boolean ativo, 
                                                       Integer pagina, Integer tamanho) {
        
        // Construir filtros
        PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
        
        List<RegraConversao> regras = regraConversaoRepository.findByFiltros(
            nome, categoria, parceiroId, ativo, 
            paginacao.getOffset(), paginacao.getLimit()
        );

        return regras.stream()
                .map(this::toRegraConversaoResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RegraConversaoResponseDTO atualizarRegra(Long id, RegraConversaoUpdateDTO request) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));

        // Atualizar campos permitidos
        if (request.nome != null) {
            regra.nome = request.nome;
        }
        if (request.multiplicador != null) {
            regra.multiplicador = request.multiplicador;
        }
        if (request.mccRegex != null) {
            regra.mccRegex = request.mccRegex;
        }
        if (request.categoria != null) {
            regra.categoria = request.categoria;
        }
        if (request.parceiroId != null) {
            regra.parceiroId = request.parceiroId;
        }
        if (request.vigenciaIni != null) {
            regra.vigenciaIni = request.vigenciaIni;
        }
        if (request.vigenciaFim != null) {
            regra.vigenciaFim = request.vigenciaFim;
        }
        if (request.prioridade != null) {
            regra.prioridade = request.prioridade;
        }
        if (request.tetoMensal != null) {
            regra.tetoMensal = request.tetoMensal;
        }

        regra.atualizadoEm = LocalDateTime.now();

        // Persistir alterações
        regraConversaoRepository.persist(regra);

        return toRegraConversaoResponseDTO(regra);
    }

    @Transactional
    public void deletarRegra(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));

        // Verificar se regra está sendo usada
        if (regraConversaoRepository.isRegraEmUso(id)) {
            throw new IllegalStateException("Não é possível deletar regra que está sendo utilizada");
        }

        regraConversaoRepository.deleteById(id);
    }

    @Transactional
    public RegraConversaoResponseDTO ativarRegra(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));

        regra.ativo = true;
        regra.atualizadoEm = LocalDateTime.now();

        regraConversaoRepository.persist(regra);

        return toRegraConversaoResponseDTO(regra);
    }

    @Transactional
    public RegraConversaoResponseDTO desativarRegra(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));

        regra.ativo = false;
        regra.atualizadoEm = LocalDateTime.now();

        regraConversaoRepository.persist(regra);

        return toRegraConversaoResponseDTO(regra);
    }

    public Object consultarDetalhesAplicacao(Long id) {
        RegraConversao regra = regraConversaoRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Regra de conversão não encontrada: " + id));

        // TODO: Implementar detalhes de aplicação
        // - Total de transações que aplicaram esta regra
        // - Total de pontos gerados
        // - Período de maior utilização
        // - Efetividade por categoria/MCC

        return null;
    }

    private void validarRegraConversao(RegraConversaoRequestDTO request) {
        if (request.nome == null || request.nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da regra é obrigatório");
        }

        if (request.multiplicador == null || request.multiplicador <= 0) {
            throw new IllegalArgumentException("Multiplicador deve ser maior que zero");
        }

        if (request.vigenciaIni != null && request.vigenciaFim != null) {
            if (request.vigenciaIni.isAfter(request.vigenciaFim)) {
                throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
            }
        }

        if (request.prioridade != null && request.prioridade < 0) {
            throw new IllegalArgumentException("Prioridade deve ser maior ou igual a zero");
        }

        if (request.tetoMensal != null && request.tetoMensal <= 0) {
            throw new IllegalArgumentException("Teto mensal deve ser maior que zero");
        }
    }

    private RegraConversaoResponseDTO toRegraConversaoResponseDTO(RegraConversao regra) {
        return new RegraConversaoResponseDTO(
            regra.id,
            regra.nome,
            regra.multiplicador,
            regra.mccRegex,
            regra.categoria,
            regra.parceiroId,
            regra.vigenciaIni,
            regra.vigenciaFim,
            regra.prioridade,
            regra.tetoMensal,
            regra.ativo,
            regra.criadoEm,
            regra.atualizadoEm
        );
    }
}

