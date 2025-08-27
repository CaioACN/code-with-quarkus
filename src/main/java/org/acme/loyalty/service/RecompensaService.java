package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.RecompensaRequestDTO;
import org.acme.loyalty.dto.RecompensaResponseDTO;
import org.acme.loyalty.dto.RecompensaUpdateDTO;
import org.acme.loyalty.dto.PageRequestDTO;
import org.acme.loyalty.entity.Recompensa;
import org.acme.loyalty.repository.RecompensaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class RecompensaService {

    @Inject
    RecompensaRepository recompensaRepository;

    @Transactional
    public RecompensaResponseDTO criarRecompensa(RecompensaRequestDTO request) {
        // Validar dados da recompensa
        validarRecompensa(request);

        // Criar nova recompensa
        Recompensa recompensa = new Recompensa();
        recompensa.tipo = request.tipo;
        recompensa.descricao = request.descricao;
        recompensa.custoPontos = request.custoPontos;
        recompensa.estoque = request.estoque;
        recompensa.estoqueMinimo = request.estoqueMinimo;
        recompensa.parceiroId = request.parceiroId;
        recompensa.ativo = true;
        recompensa.criadoEm = LocalDateTime.now();
        recompensa.atualizadoEm = LocalDateTime.now();

        // Persistir recompensa
        recompensaRepository.persist(recompensa);

        return toRecompensaResponseDTO(recompensa);
    }

    public RecompensaResponseDTO buscarRecompensaPorId(Long id) {
        Recompensa recompensa = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        return toRecompensaResponseDTO(recompensa);
    }

    public List<RecompensaResponseDTO> listarRecompensas(String tipo, String descricao, 
                                                        Long parceiroId, Boolean ativo, 
                                                        Integer pagina, Integer tamanho) {
        
        // Construir filtros
        PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
        
        List<Recompensa> recompensas = recompensaRepository.findByFiltros(
            tipo, descricao, parceiroId, ativo, 
            paginacao.getOffset(), paginacao.getLimit()
        );

        return recompensas.stream()
                .map(this::toRecompensaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public RecompensaResponseDTO atualizarRecompensa(Long id, RecompensaUpdateDTO request) {
        Recompensa recompensa = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        // Atualizar campos permitidos
        if (request.tipo != null) {
            recompensa.tipo = request.tipo;
        }
        if (request.descricao != null) {
            recompensa.descricao = request.descricao;
        }
        if (request.custoPontos != null) {
            recompensa.custoPontos = request.custoPontos;
        }
        if (request.estoque != null) {
            recompensa.estoque = request.estoque;
        }
        if (request.estoqueMinimo != null) {
            recompensa.estoqueMinimo = request.estoqueMinimo;
        }
        if (request.parceiroId != null) {
            recompensa.parceiroId = request.parceiroId;
        }

        recompensa.atualizadoEm = LocalDateTime.now();

        // Persistir alterações
        recompensaRepository.persist(recompensa);

        return toRecompensaResponseDTO(recompensa);
    }

    @Transactional
    public void deletarRecompensa(Long id) {
        Recompensa recompensa = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        // Verificar se recompensa está sendo usada
        if (recompensaRepository.isRecompensaEmUso(id)) {
            throw new IllegalStateException("Não é possível deletar recompensa que está sendo utilizada");
        }

        recompensaRepository.deleteById(id);
    }

    @Transactional
    public RecompensaResponseDTO ativarRecompensa(Long id) {
        Recompensa recompensa = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        recompensa.ativo = true;
        recompensa.atualizadoEm = LocalDateTime.now();

        recompensaRepository.persist(recompensa);

        return toRecompensaResponseDTO(recompensa);
    }

    @Transactional
    public RecompensaResponseDTO desativarRecompensa(Long id) {
        Recompensa recompensa = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        recompensa.ativo = false;
        recompensa.atualizadoEm = LocalDateTime.now();

        recompensaRepository.persist(recompensa);

        return toRecompensaResponseDTO(recompensa);
    }

    @Transactional
    public RecompensaResponseDTO ajustarEstoque(Long id, Integer quantidade, String motivo) {
        Recompensa recompensa = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        // Ajustar estoque
        recompensa.estoque += quantidade;
        
        if (recompensa.estoque < 0) {
            throw new IllegalArgumentException("Estoque não pode ficar negativo");
        }

        recompensa.atualizadoEm = LocalDateTime.now();

        // Persistir alterações
        recompensaRepository.persist(recompensa);

        // TODO: Registrar movimento de estoque
        // registrarMovimentoEstoque(recompensa, quantidade, motivo);

        return toRecompensaResponseDTO(recompensa);
    }

    public List<RecompensaResponseDTO> listarRecompensasDisponiveis() {
        List<Recompensa> recompensas = recompensaRepository.findRecompensasDisponiveis();

        return recompensas.stream()
                .map(this::toRecompensaResponseDTO)
                .collect(Collectors.toList());
    }

    public List<RecompensaResponseDTO> listarRecompensasPorParceiro(Long parceiroId) {
        List<Recompensa> recompensas = recompensaRepository.findByParceiroId(parceiroId);

        return recompensas.stream()
                .map(this::toRecompensaResponseDTO)
                .collect(Collectors.toList());
    }

    private void validarRecompensa(RecompensaRequestDTO request) {
        if (request.tipo == null || request.tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo da recompensa é obrigatório");
        }

        if (request.descricao == null || request.descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da recompensa é obrigatória");
        }

        if (request.custoPontos == null || request.custoPontos <= 0) {
            throw new IllegalArgumentException("Custo em pontos deve ser maior que zero");
        }

        if (request.estoque == null || request.estoque < 0) {
            throw new IllegalArgumentException("Estoque deve ser maior ou igual a zero");
        }

        if (request.estoqueMinimo != null && request.estoqueMinimo < 0) {
            throw new IllegalArgumentException("Estoque mínimo deve ser maior ou igual a zero");
        }
    }

    private RecompensaResponseDTO toRecompensaResponseDTO(Recompensa recompensa) {
        return new RecompensaResponseDTO(
            recompensa.id,
            recompensa.tipo,
            recompensa.descricao,
            recompensa.custoPontos,
            recompensa.estoque,
            recompensa.estoqueMinimo,
            recompensa.parceiroId,
            recompensa.ativo,
            recompensa.criadoEm,
            recompensa.atualizadoEm
        );
    }
}

