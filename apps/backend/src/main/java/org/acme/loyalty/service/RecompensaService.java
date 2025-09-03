package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.RecompensaRequestDTO;
import org.acme.loyalty.dto.RecompensaResponseDTO;
import org.acme.loyalty.dto.RecompensaUpdateDTO;
import org.acme.loyalty.entity.Recompensa;
import org.acme.loyalty.repository.RecompensaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
public class RecompensaService {

    @Inject
    RecompensaRepository recompensaRepository;

    // ===================== CRUD =====================

    @Transactional
    public RecompensaResponseDTO criarRecompensa(RecompensaRequestDTO req) {
        validarRecompensa(req);

        // unicidade por descrição (case-insensitive)
        if (recompensaRepository.existsByDescricaoIgnoringId(req.descricao, null)) {
            throw new IllegalArgumentException("Já existe recompensa com a mesma descrição");
        }

        Recompensa r = new Recompensa();
        r.tipo = req.tipo; // enum Recompensa.TipoRecompensa
        r.descricao = req.descricao;
        r.custoPontos = req.custoPontos;
        r.estoque = req.estoque;
        r.parceiroId = req.parceiroId;
        r.detalhes = req.detalhes;
        r.imagemUrl = req.imagemUrl;
        r.validadeRecompensa = req.validadeRecompensa;
        r.ativo = Boolean.TRUE;
        r.criadoEm = LocalDateTime.now();
        r.atualizadoEm = r.criadoEm;

        recompensaRepository.persist(r);
        return toDTO(r);
    }

    public RecompensaResponseDTO buscarRecompensaPorId(Long id) {
        Recompensa r = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));
        return toDTO(r);
    }

    /**
     * Lista recompensas com filtros simples.
     * - tipo: string → enum (ignora se inválido)
     * - descricao: se for o único filtro, usa busca paginada por LIKE no repositório; caso contrário,
     *              usa a query de catálogo e filtra a descrição em memória (pós-paginação).
     * - parceiroId/ativo: passados à query de catálogo.
     */
    public List<RecompensaResponseDTO> listarRecompensas(String tipo,
                                                         String descricao,
                                                         Long parceiroId,
                                                         Boolean ativo,
                                                         Integer pagina,
                                                         Integer tamanho) {
        // paginação (index de página baseado em 0)
        final int pageIndex = (pagina == null || pagina < 1) ? 0 : (pagina - 1);
        final int pageSize  = (tamanho == null || tamanho < 1) ? 20 : tamanho;

        Recompensa.TipoRecompensa tipoEnum = parseTipo(tipo);

        boolean apenasDescricao =
                (tipoEnum == null && parceiroId == null && ativo == null
                        && descricao != null && !descricao.isBlank());

        if (apenasDescricao) {
            return recompensaRepository
                    .searchByDescricao(descricao, pageIndex, pageSize)
                    .list()
                    .stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }

        // Consulta de catálogo (não tem filtro por descrição nativo)
        List<Recompensa> base = recompensaRepository
                .queryCatalogo(
                        tipoEnum,
                        parceiroId,
                        null,      // somenteDisponiveis
                        null, null,// minCusto, maxCusto
                        ativo,     // apenasAtivas
                        null,      // agora
                        pageIndex, pageSize
                )
                .list();

        if (descricao != null && !descricao.isBlank()) {
            String like = descricao.trim().toLowerCase(Locale.ROOT);
            base = base.stream()
                    .filter(r -> r.descricao != null && r.descricao.toLowerCase(Locale.ROOT).contains(like))
                    .collect(Collectors.toList());
        }

        return base.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public RecompensaResponseDTO atualizarRecompensa(Long id, RecompensaUpdateDTO req) {
        Recompensa r = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        if (req.descricao != null && !req.descricao.equalsIgnoreCase(r.descricao)) {
            if (recompensaRepository.existsByDescricaoIgnoringId(req.descricao, id)) {
                throw new IllegalArgumentException("Já existe recompensa com a mesma descrição");
            }
            r.descricao = req.descricao;
        }
        if (req.tipo != null)               r.tipo = req.tipo;
        if (req.custoPontos != null)        r.custoPontos = req.custoPontos;
        if (req.estoque != null)            r.estoque = req.estoque;
        if (req.parceiroId != null)         r.parceiroId = req.parceiroId;
        if (req.detalhes != null)           r.detalhes = req.detalhes;
        if (req.imagemUrl != null)          r.imagemUrl = req.imagemUrl;
        if (req.validadeRecompensa != null) r.validadeRecompensa = req.validadeRecompensa;
        if (req.ativo != null)              r.ativo = req.ativo;

        r.atualizadoEm = LocalDateTime.now();
        recompensaRepository.persist(r);

        return toDTO(r);
    }

    @Transactional
    public void deletarRecompensa(Long id) {
        boolean ok = recompensaRepository.deleteById(id);
        if (!ok) {
            throw new NotFoundException("Recompensa não encontrada: " + id);
        }
    }

    // ===================== Ativação =====================

    @Transactional
    public RecompensaResponseDTO ativarRecompensa(Long id) {
        Recompensa r = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));
        recompensaRepository.ativar(id);
        r.ativo = true;
        r.atualizadoEm = LocalDateTime.now();
        return toDTO(r);
    }

    @Transactional
    public RecompensaResponseDTO desativarRecompensa(Long id) {
        Recompensa r = recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));
        recompensaRepository.desativar(id);
        r.ativo = false;
        r.atualizadoEm = LocalDateTime.now();
        return toDTO(r);
    }

    // ===================== Estoque =====================

    /**
     * Ajusta estoque:
     * - quantidade > 0 → repor (incrementa)
     * - quantidade < 0 → reservar (decrementa) se houver saldo suficiente
     */
    @Transactional
    public RecompensaResponseDTO ajustarEstoque(Long id, Long quantidade, String motivo) {
        if (quantidade == null || quantidade == 0) {
            throw new IllegalArgumentException("Quantidade deve ser diferente de zero");
        }
        // garante que existe
        recompensaRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Recompensa não encontrada: " + id));

        boolean ok;
        if (quantidade > 0) {
            ok = recompensaRepository.reporEstoque(id, quantidade);
        } else {
            ok = recompensaRepository.reservarEstoque(id, Math.abs(quantidade));
        }
        if (!ok) {
            if (quantidade < 0) {
                throw new IllegalStateException("Estoque insuficiente para baixa");
            }
            throw new IllegalStateException("Falha ao ajustar estoque");
        }
        // retorna estado atual
        Recompensa r = recompensaRepository.findByIdOptional(id).orElseThrow();
        return toDTO(r);
    }

    // ===================== Consultas específicas =====================

    public List<RecompensaResponseDTO> listarRecompensasDisponiveis() {
        return recompensaRepository.listDisponiveis(LocalDateTime.now())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<RecompensaResponseDTO> listarRecompensasPorParceiro(Long parceiroId) {
        return recompensaRepository.listByParceiroId(parceiroId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ===================== Helpers =====================

    private void validarRecompensa(RecompensaRequestDTO req) {
        if (req.tipo == null) {
            throw new IllegalArgumentException("Tipo da recompensa é obrigatório");
        }
        if (req.descricao == null || req.descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição da recompensa é obrigatória");
        }
        if (req.custoPontos == null || req.custoPontos <= 0) {
            throw new IllegalArgumentException("Custo em pontos deve ser maior que zero");
        }
        if (req.estoque == null || req.estoque < 0) {
            throw new IllegalArgumentException("Estoque deve ser maior ou igual a zero");
        }
    }

    private RecompensaResponseDTO toDTO(Recompensa r) {
        RecompensaResponseDTO dto = new RecompensaResponseDTO();
        dto.id = r.id;
        dto.tipo = r.tipo;
        dto.descricao = r.descricao;
        dto.custoPontos = r.custoPontos;
        dto.estoque = r.estoque;
        dto.parceiroId = r.parceiroId;
        dto.ativo = r.ativo;
        dto.detalhes = r.detalhes;
        dto.imagemUrl = r.imagemUrl;
        dto.validadeRecompensa = r.validadeRecompensa;
        dto.criadoEm = r.criadoEm;
        dto.atualizadoEm = r.atualizadoEm;
        if (r.getStatusEstoque() != null) {
            dto.statusEstoque = r.getStatusEstoque();
        }
        return dto;
    }

    private Recompensa.TipoRecompensa parseTipo(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String norm = texto.trim().toUpperCase(Locale.ROOT);
        try {
            return Recompensa.TipoRecompensa.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            // tipo inválido → ignora filtro
            return null;
        }
    }
}
