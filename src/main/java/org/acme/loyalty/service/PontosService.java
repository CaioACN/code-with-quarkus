package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.SaldoUsuarioDTO;
import org.acme.loyalty.dto.SaldoPontosDTO;
import org.acme.loyalty.dto.ExtratoPontosDTO;
import org.acme.loyalty.dto.ExtratoFilterDTO;
import org.acme.loyalty.dto.PageRequestDTO;
import org.acme.loyalty.dto.MovimentoPontosDTO;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.entity.SaldoPontos;
import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.repository.UsuarioRepository;
import org.acme.loyalty.repository.SaldoPontosRepository;
import org.acme.loyalty.repository.MovimentoPontosRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class PontosService {

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    SaldoPontosRepository saldoPontosRepository;

    @Inject
    MovimentoPontosRepository movimentoPontosRepository;

    public SaldoUsuarioDTO consultarSaldo(Long usuarioId) {
        // Validar se usuário existe
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // Buscar saldos de todos os cartões do usuário
        List<SaldoPontos> saldos = saldoPontosRepository.findByUsuarioId(usuarioId);

        // Converter para DTOs
        List<SaldoPontosDTO> saldosDTO = saldos.stream()
                .map(this::toSaldoPontosDTO)
                .collect(Collectors.toList());

        return new SaldoUsuarioDTO(usuarioId, saldosDTO);
    }

    public ExtratoPontosDTO consultarExtrato(Long usuarioId, Long cartaoId, 
                                           String dataInicio, String dataFim,
                                           String tipoMovimento, String categoria,
                                           Long parceiroId, Integer pagina, Integer tamanho) {
        
        // Validar se usuário existe
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // Construir filtros
        ExtratoFilterDTO filtros = new ExtratoFilterDTO(
            parseDate(dataInicio), parseDate(dataFim), cartaoId, 
            tipoMovimento, categoria, parceiroId
        );

        // Buscar movimentos com paginação
        PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
        
        List<MovimentoPontos> movimentos = movimentoPontosRepository.findByFiltros(
            usuarioId, filtros, paginacao.getOffset(), paginacao.getLimit()
        );

        // Contar total de registros
        Long totalRegistros = movimentoPontosRepository.countByFiltros(usuarioId, filtros);

        // Converter para DTOs
        List<MovimentoPontosDTO> movimentosDTO = movimentos.stream()
                .map(this::toMovimentoPontosDTO)
                .collect(Collectors.toList());

        // Criar extrato
        ExtratoPontosDTO extrato = new ExtratoPontosDTO(
            usuarioId, cartaoId, 
            parseDateTime(dataInicio), parseDateTime(dataFim),
            movimentosDTO, pagina, tamanho, totalRegistros
        );

        // Calcular saldos inicial e final
        Long saldoInicial = calcularSaldoInicial(usuarioId, cartaoId, parseDate(dataInicio));
        extrato.calcularSaldos(saldoInicial);

        return extrato;
    }

    public SaldoPontosDTO consultarSaldoCartao(Long usuarioId, Long cartaoId) {
        // Validar se usuário existe
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // Buscar saldo específico do cartão
        SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(usuarioId, cartaoId)
                .orElseThrow(() -> new NotFoundException("Saldo não encontrado para o cartão: " + cartaoId));

        return toSaldoPontosDTO(saldo);
    }

    public List<MovimentoPontosDTO> consultarMovimentos(Long usuarioId, Long cartaoId,
                                                       String tipo, String dataInicio, 
                                                       String dataFim, Integer pagina, Integer tamanho) {
        
        // Validar se usuário existe
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // Construir filtros
        ExtratoFilterDTO filtros = new ExtratoFilterDTO(
            parseDate(dataInicio), parseDate(dataFim), cartaoId, tipo, null, null
        );

        // Buscar movimentos
        PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
        
        List<MovimentoPontos> movimentos = movimentoPontosRepository.findByFiltros(
            usuarioId, filtros, paginacao.getOffset(), paginacao.getLimit()
        );

        // Converter para DTOs
        return movimentos.stream()
                .map(this::toMovimentoPontosDTO)
                .collect(Collectors.toList());
    }

    public Object consultarResumo(Long usuarioId) {
        // Validar se usuário existe
        Usuario usuario = usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // TODO: Implementar resumo consolidado
        // - Total de pontos por cartão
        // - Pontos expirando nos próximos 30/60/90 dias
        // - Histórico de acúmulo vs resgate
        // - Ranking de categorias mais utilizadas

        return null;
    }

    private SaldoPontosDTO toSaldoPontosDTO(SaldoPontos saldo) {
        return new SaldoPontosDTO(
            saldo.cartao.id,
            saldo.saldo,
            saldo.atualizadoEm,
            saldo.pontosExpirando30Dias,
            saldo.pontosExpirando60Dias,
            saldo.pontosExpirando90Dias,
            calcularStatusSaldo(saldo)
        );
    }

    private MovimentoPontosDTO toMovimentoPontosDTO(MovimentoPontos movimento) {
        return new MovimentoPontosDTO(
            movimento.id,
            movimento.usuario.id,
            movimento.cartao.id,
            movimento.tipo.name(),
            movimento.pontos,
            movimento.refTransacaoId,
            movimento.observacao,
            movimento.criadoEm,
            movimento.jobId,
            movimento.regraAplicada,
            movimento.campanhaAplicada
        );
    }

    private String calcularStatusSaldo(SaldoPontos saldo) {
        if (saldo.saldo == null || saldo.saldo <= 0) {
            return "SEM_PONTOS";
        }

        if (saldo.pontosExpirando30Dias != null && saldo.pontosExpirando30Dias > 0) {
            return "PONTOS_EXPIRANDO";
        }

        if (saldo.saldo > 10000) {
            return "ALTO_SALDO";
        } else if (saldo.saldo > 1000) {
            return "MEDIO_SALDO";
        } else {
            return "BAIXO_SALDO";
        }
    }

    private Long calcularSaldoInicial(Long usuarioId, Long cartaoId, LocalDate dataInicio) {
        if (dataInicio == null) {
            return 0L;
        }

        // TODO: Implementar cálculo do saldo inicial na data especificada
        // Buscar saldo anterior à data de início
        return 0L;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de data inválido: " + dateStr);
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            if (dateTimeStr.contains("T")) {
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                return LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atStartOfDay();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de data inválido: " + dateTimeStr);
        }
    }
}

