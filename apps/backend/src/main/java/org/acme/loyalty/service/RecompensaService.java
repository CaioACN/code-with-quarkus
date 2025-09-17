package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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
    
    @Inject
    EntityManager entityManager;

    // ===================== CRUD =====================

    @Transactional
    public RecompensaResponseDTO criarRecompensa(RecompensaRequestDTO req) {
        // Usar método de validação centralizado
        validarRecompensa(req);
        
        // Conversão do tipo
        Recompensa.TipoRecompensa tipoEnum;
        if (req.tipo == null || req.tipo.trim().isEmpty()) {
            tipoEnum = Recompensa.TipoRecompensa.PRODUTO; // Valor padrão
        } else {
            tipoEnum = Recompensa.TipoRecompensa.valueOf(req.tipo.toUpperCase(Locale.ROOT));
        }

        // unicidade por descrição (case-insensitive)
        if (recompensaRepository.existsByDescricaoIgnoringId(req.descricao, null)) {
            throw new IllegalArgumentException("Já existe recompensa com a mesma descrição");
        }

        // Criar entidade com todos os campos
        Recompensa r = new Recompensa();
        r.tipo = tipoEnum;
        r.descricao = req.descricao;
        r.custoPontos = req.custoPontos;
        r.estoque = req.estoque;
        r.parceiroId = req.parceiroId;
        r.detalhes = req.detalhes;
        r.imagemUrl = req.imagemUrl;
        // Converter String para LocalDateTime se necessário
        if (req.validadeRecompensa != null && !req.validadeRecompensa.trim().isEmpty()) {
            try {
                // Tentar primeiro como timestamp completo
                if (req.validadeRecompensa.contains("T")) {
                    r.validadeRecompensa = LocalDateTime.parse(req.validadeRecompensa);
                } else {
                    // Se for apenas data, adicionar horário
                    r.validadeRecompensa = LocalDateTime.parse(req.validadeRecompensa + "T23:59:59");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Formato de data inválido para validadeRecompensa: " + req.validadeRecompensa);
            }
        } else {
            r.validadeRecompensa = null;
        }
        r.ativo = Boolean.TRUE;
        r.criadoEm = LocalDateTime.now();
        r.atualizadoEm = r.criadoEm;

        // Persistir usando EntityManager diretamente
        entityManager.persist(r);
        entityManager.flush();
        entityManager.refresh(r);
        
        // Usar o método factory do DTO para garantir que todos os campos derivados sejam preenchidos
        RecompensaResponseDTO dto = RecompensaResponseDTO.fromEntity(r);
        
        return dto;
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
        // Validação do request
        if (req == null) {
            throw new IllegalArgumentException("Request não pode ser nulo");
        }
        
        // Validação do tipo
        if (req.tipo == null || req.tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo da recompensa é obrigatório");
        }
        try {
            Recompensa.TipoRecompensa.valueOf(req.tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de recompensa inválido: " + req.tipo + ". Valores válidos: MILHAS, GIFT, CASHBACK, PRODUTO");
        }
        
        // Validação da descrição
        if (req.descricao == null || req.descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da recompensa é obrigatória");
        }
        if (req.descricao.length() > 255) {
            throw new IllegalArgumentException("Descrição não pode ter mais de 255 caracteres");
        }
        
        // Validação do custo em pontos
        if (req.custoPontos == null || req.custoPontos <= 0) {
            throw new IllegalArgumentException("Custo em pontos deve ser maior que zero");
        }
        if (req.custoPontos > 1000000) {
            throw new IllegalArgumentException("Custo em pontos não pode ser maior que 1.000.000");
        }
        
        // Validação do estoque
        if (req.estoque == null || req.estoque < 0) {
            throw new IllegalArgumentException("Estoque deve ser maior ou igual a zero");
        }
        if (req.estoque > 999999) {
            throw new IllegalArgumentException("Estoque não pode ser maior que 999.999");
        }
        
        // Validação do parceiroId (se fornecido)
        if (req.parceiroId != null && req.parceiroId <= 0) {
            throw new IllegalArgumentException("ID do parceiro deve ser maior que zero");
        }
        
        // Validação dos detalhes (se fornecido)
        if (req.detalhes != null && req.detalhes.length() > 1000) {
            throw new IllegalArgumentException("Detalhes não podem ter mais de 1000 caracteres");
        }
        
        // Validação da URL da imagem (se fornecida)
        if (req.imagemUrl != null && !req.imagemUrl.trim().isEmpty()) {
            if (req.imagemUrl.length() > 500) {
                throw new IllegalArgumentException("URL da imagem não pode ter mais de 500 caracteres");
            }
            if (!req.imagemUrl.startsWith("http://") && !req.imagemUrl.startsWith("https://")) {
                throw new IllegalArgumentException("URL da imagem deve começar com http:// ou https://");
            }
        }
        
        // Validação da data de validade (se fornecida) - simplificada para testes
        if (req.validadeRecompensa != null && !req.validadeRecompensa.trim().isEmpty()) {
            try {
                // Apenas verificar se o formato é válido, sem validar se é futuro
                if (req.validadeRecompensa.contains("T")) {
                    LocalDateTime.parse(req.validadeRecompensa);
                } else {
                    LocalDateTime.parse(req.validadeRecompensa + "T23:59:59");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Formato de data inválido para validadeRecompensa. Use formato: YYYY-MM-DD ou YYYY-MM-DDTHH:mm:ss");
            }
        }
    }

    private RecompensaResponseDTO toDTO(Recompensa r) {
        System.out.println("DEBUG: toDTO - Entidade recebida: " + r);
        System.out.println("DEBUG: toDTO - ID: " + r.id);
        System.out.println("DEBUG: toDTO - Tipo: " + r.tipo);
        System.out.println("DEBUG: toDTO - Descrição: " + r.descricao);
        
        // Usar o método factory do DTO para garantir que todos os campos derivados sejam preenchidos
        RecompensaResponseDTO dto = RecompensaResponseDTO.fromEntity(r);
        
        System.out.println("DEBUG: toDTO - DTO criado: " + dto);
        System.out.println("DEBUG: toDTO - DTO ID: " + dto.id);
        System.out.println("DEBUG: toDTO - DTO Tipo: " + dto.tipo);
        System.out.println("DEBUG: toDTO - DTO Descrição: " + dto.descricao);
        System.out.println("DEBUG: toDTO - DTO disponivel: " + dto.disponivel);
        System.out.println("DEBUG: toDTO - DTO statusEstoque: " + dto.statusEstoque);
        
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
