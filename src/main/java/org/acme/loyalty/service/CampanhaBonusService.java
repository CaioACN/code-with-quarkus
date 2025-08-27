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


import java.time.LocalDate;
import java.math.BigDecimal;
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
        CampanhaBonus campanha = new CampanhaBonus(
            request.nome,
            request.multiplicadorExtra,
            request.vigenciaIni,
            request.vigenciaFim,
            request.segmento,
            request.prioridade,
            request.teto
        );

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
        
        // Usar métodos que existem no repository
        List<CampanhaBonus> campanhas;
        if (ativo != null && ativo) {
            campanhas = campanhaBonusRepository.listarVigentes(LocalDate.now());
        } else {
            campanhas = campanhaBonusRepository.findAll().list();
        }
        
        // Aplicar filtros adicionais se necessário
        if (nome != null && !nome.trim().isEmpty()) {
            campanhas = campanhas.stream()
                .filter(c -> c.nome.toLowerCase().contains(nome.toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (segmento != null && !segmento.trim().isEmpty()) {
            campanhas = campanhas.stream()
                .filter(c -> c.aplicaParaSegmento(segmento))
                .collect(Collectors.toList());
        }

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

        // Persistir alterações
        campanhaBonusRepository.persist(campanha);

        return toCampanhaBonusResponseDTO(campanha);
    }

    @Transactional
    public void deletarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        // Verificar se campanha está sendo usada (simplificado por enquanto)
        // TODO: Implementar verificação de uso da campanha

        campanhaBonusRepository.deleteById(id);
    }

    @Transactional
    public CampanhaBonusResponseDTO ativarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        // Como a entidade não tem campo ativo, vamos usar a vigência
        // Ativar = definir vigência futura
        if (campanha.vigenciaIni.isAfter(LocalDate.now())) {
            campanha.vigenciaIni = LocalDate.now();
        }

        campanhaBonusRepository.persist(campanha);

        return toCampanhaBonusResponseDTO(campanha);
    }

    @Transactional
    public CampanhaBonusResponseDTO desativarCampanha(Long id) {
        CampanhaBonus campanha = campanhaBonusRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Campanha de bônus não encontrada: " + id));

        // Como a entidade não tem campo ativo, vamos usar a vigência
        // Desativar = definir vigência passada
        campanha.vigenciaFim = LocalDate.now().minusDays(1);

        campanhaBonusRepository.persist(campanha);

        return toCampanhaBonusResponseDTO(campanha);
    }

    public List<CampanhaBonusResponseDTO> listarCampanhasAtivas() {
        List<CampanhaBonus> campanhas = campanhaBonusRepository.listarVigentes(LocalDate.now());

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

        if (request.multiplicadorExtra == null || request.multiplicadorExtra.compareTo(BigDecimal.ZERO) <= 0) {
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
        return CampanhaBonusResponseDTO.fromEntity(campanha);
    }
}

