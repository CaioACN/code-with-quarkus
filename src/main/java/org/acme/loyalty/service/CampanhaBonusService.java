package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.CampanhaBonusRequestDTO;
import org.acme.loyalty.dto.CampanhaBonusResponseDTO;
import org.acme.loyalty.dto.CampanhaBonusUpdateDTO;
import org.acme.loyalty.dto.PageRequestDTO;
import org.acme.loyalty.entity.CampanhaBonus;
import org.acme.loyalty.repository.CampanhaBonusRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CampanhaBonusService {

    @Inject
    CampanhaBonusRepository campanhaBonusRepository;

    @Transactional
    public CampanhaBonusResponseDTO criarCampanha(CampanhaBonusRequestDTO request) {
        // Validar dados da campanha
        validarCampanhaBonus(request);

        // Criar nova campanha
        CampanhaBonus campanha = new CampanhaBonus();
        campanha.nome = request.nome;
        campanha.descricao = request.descricao;
        campanha.multiplicadorExtra = request.multiplicadorExtra;
        campanha.vigenciaIni = request.vigenciaIni;
        campanha.vigenciaFim = request.vigenciaFim;
        campanha.segmento = request.segmento;
        campanha.prioridade = request.prioridade;
        campanha.teto = request.teto;
        campanha.ativo = true;
        campanha.criadoEm = LocalDateTime.now();
        campanha.atualizadoEm = LocalDateTime.now();

        // Persistir campanha
        campanhaBonusRepository.persist(campanha);

        return toCampanhaBonusResponseDTO(campanha);
    }

    public CampanhaBonusResponseDTO buscarCampanhaPorId(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        return toCampanhaBonusResponseDTO(campanha);
    }

    public List<CampanhaBonusResponseDTO> listarCampanhas(String nome, String segmento, 
                                                         Boolean ativo, Integer pagina, Integer tamanho) {
        
        // Construir filtros
        PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
        
        List<CampanhaBonus> campanhas = campanhaBonusRepository.findByFiltros(
            nome, segmento, ativo, paginacao.getOffset(), paginacao.getLimit()
        );

        return campanhas.stream()
                .map(this::toCampanhaBonusResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CampanhaBonusResponseDTO atualizarCampanha(Long id, CampanhaBonusUpdateDTO request) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        // Atualizar campos permitidos
        if (request.nome != null) {
            campanha.nome = request.nome;
        }
        if (request.descricao != null) {
            campanha.descricao = request.descricao;
        }
        if (request.multiplicadorExtra != null) {
            campanha.multiplicadorExtra = request.multiplicadorExtra;
        }
        if (request.vigenciaIni != null) {
            campanha.vigenciaIni = request.vigenciaIni;
        }
        if (request.vigenciaFim != null) {
            campanha.vigenciaFim = request.vigenciaFim;
        }
        if (request.segmento != null) {
            campanha.segmento = request.segmento;
        }
        if (request.prioridade != null) {
            campanha.prioridade = request.prioridade;
        }
        if (request.teto != null) {
            campanha.teto = request.teto;
        }

        campanha.atualizadoEm = LocalDateTime.now();

        // Persistir alterações
        campanhaBonusRepository.persist(campanha);

        return toCampanhaBonusResponseDTO(campanha);
    }

    @Transactional
    public void deletarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        // Verificar se campanha está sendo usada
        if (campanhaBonusRepository.isCampanhaEmUso(id)) {
            throw new IllegalStateException("Não é possível deletar campanha que está sendo utilizada");
        }

        campanhaBonusRepository.deleteById(id);
    }

    @Transactional
    public CampanhaBonusResponseDTO ativarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        campanha.ativo = true;
        campanha.atualizadoEm = LocalDateTime.now();

        campanhaBonusRepository.persist(campanha);

        return toCampanhaBonusResponseDTO(campanha);
    }

    @Transactional
    public CampanhaBonusResponseDTO desativarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        campanha.ativo = false;
        campanha.atualizadoEm = LocalDateTime.now();

        campanhaBonusRepository.persist(campanha);

        return toCampanhaBonusResponseDTO(campanha);
    }

    public List<CampanhaBonusResponseDTO> listarCampanhasAtivas() {
        List<CampanhaBonus> campanhas = campanhaBonusRepository.findCampanhasAtivas();

        return campanhas.stream()
                .map(this::toCampanhaBonusResponseDTO)
                .collect(Collectors.toList());
    }

    public Object consultarStatusCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        // TODO: Implementar status da campanha
        // - Total de usuários que se beneficiaram
        // - Total de pontos extras gerados
        // - Período de maior utilização
        // - Efetividade por segmento

        return null;
    }

    private void validarCampanhaBonus(CampanhaBonusRequestDTO request) {
        if (request.nome == null || request.nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da campanha é obrigatório");
        }

        if (request.multiplicadorExtra == null || request.multiplicadorExtra <= 0) {
            throw new IllegalArgumentException("Multiplicador extra deve ser maior que zero");
        }

        if (request.vigenciaIni != null && request.vigenciaFim != null) {
            if (request.vigenciaIni.isAfter(request.vigenciaFim)) {
                throw new IllegalArgumentException("Data de início deve ser anterior à data de fim");
            }
        }

        if (request.prioridade != null && request.prioridade < 0) {
            throw new IllegalArgumentException("Prioridade deve ser maior ou igual a zero");
        }

        if (request.teto != null && request.teto <= 0) {
            throw new IllegalArgumentException("Teto deve ser maior que zero");
        }
    }

    private CampanhaBonusResponseDTO toCampanhaBonusResponseDTO(CampanhaBonus campanha) {
        return new CampanhaBonusResponseDTO(
            campanha.id,
            campanha.nome,
            campanha.descricao,
            campanha.multiplicadorExtra,
            campanha.vigenciaIni,
            campanha.vigenciaFim,
            campanha.segmento,
            campanha.prioridade,
            campanha.teto,
            campanha.ativo,
            campanha.criadoEm,
            campanha.atualizadoEm
        );
    }
}

