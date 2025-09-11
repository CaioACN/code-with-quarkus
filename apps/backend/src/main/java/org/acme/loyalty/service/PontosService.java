package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.ExtratoPontosDTO;
import org.acme.loyalty.dto.MovimentoPontosDTO;
import org.acme.loyalty.dto.SaldoPontosDTO;
import org.acme.loyalty.dto.SaldoUsuarioDTO;
import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.entity.SaldoPontos;
import org.acme.loyalty.repository.MovimentoPontosRepository;
import org.acme.loyalty.repository.SaldoPontosRepository;
import org.acme.loyalty.repository.UsuarioRepository;

import io.quarkus.hibernate.orm.panache.PanacheQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class PontosService {

    @Inject UsuarioRepository usuarioRepository;
    @Inject SaldoPontosRepository saldoPontosRepository;
    @Inject MovimentoPontosRepository movimentoPontosRepository;

    // ---------------------- SALDO ----------------------

    public SaldoUsuarioDTO consultarSaldo(Long usuarioId) {
        // valida existência do usuário
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // lista saldos do usuário
        List<SaldoPontos> saldos = saldoPontosRepository.listByUsuarioId(usuarioId);

        List<SaldoPontosDTO> itens = saldos.stream()
                .map(this::toSaldoPontosDTO)
                .collect(Collectors.toList());

        return new SaldoUsuarioDTO(usuarioId, itens);
    }

    public SaldoPontosDTO consultarSaldoCartao(Long usuarioId, Long cartaoId) {
        // valida existência do usuário
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(usuarioId, cartaoId)
                .orElseThrow(() -> new NotFoundException("Saldo não encontrado para o cartão: " + cartaoId));

        return toSaldoPontosDTO(saldo);
    }

    // ---------------------- EXTRATO ----------------------

    public ExtratoPontosDTO consultarExtrato(Long usuarioId,
                                             Long cartaoId,
                                             String dataInicio,
                                             String dataFim,
                                             String tipoMovimento,
                                             String categoria,     // ignorado: não há esse filtro em MovimentoPontos
                                             Long parceiroId,      // ignorado: não há esse filtro em MovimentoPontos
                                             Integer pagina,
                                             Integer tamanho) {
        // valida existência do usuário
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        LocalDate ini = parseDate(dataInicio);
        LocalDate fim = parseDate(dataFim);
        LocalDateTime de  = toStartOfDay(ini);
        LocalDateTime ate = toEndOfDay(fim);

        MovimentoPontos.TipoMovimento tipoEnum = parseTipoMovimento(tipoMovimento);

        // paginação Panache é 0-based
        int pageIndex = Math.max(0, (pagina == null ? 1 : pagina) - 1);
        int size = (tamanho == null ? 20 : tamanho);

        PanacheQuery<MovimentoPontos> q = movimentoPontosRepository.queryExtrato(
                usuarioId, cartaoId, de, ate, tipoEnum, pageIndex, size
        );

        List<MovimentoPontos> movimentos = q.list();
        long total = q.count();

        List<MovimentoPontosDTO> itens = movimentos.stream()
                .map(this::toMovimentoPontosDTO)
                .collect(Collectors.toList());

        ExtratoPontosDTO extrato = new ExtratoPontosDTO(
                usuarioId,
                cartaoId,
                de,
                ate,
                itens,
                (pagina == null ? 1 : pagina),
                size,
                total
        );

        Long saldoInicial = calcularSaldoInicial(usuarioId, cartaoId, ini);
        extrato.calcularSaldos(saldoInicial);

        return extrato;
    }

    public List<MovimentoPontosDTO> consultarMovimentos(Long usuarioId,
                                                         Long cartaoId,
                                                         String tipo,
                                                         String dataInicio,
                                                         String dataFim,
                                                         Integer pagina,
                                                         Integer tamanho) {
        // valida existência do usuário
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        LocalDate ini = parseDate(dataInicio);
        LocalDate fim = parseDate(dataFim);
        LocalDateTime de  = toStartOfDay(ini);
        LocalDateTime ate = toEndOfDay(fim);
        MovimentoPontos.TipoMovimento tipoEnum = parseTipoMovimento(tipo);

        int pageIndex = Math.max(0, (pagina == null ? 1 : pagina) - 1);
        int size = (tamanho == null ? 20 : tamanho);

        PanacheQuery<MovimentoPontos> q = movimentoPontosRepository.queryExtrato(
                usuarioId, cartaoId, de, ate, tipoEnum, pageIndex, size
        );

        return q.list().stream()
                .map(this::toMovimentoPontosDTO)
                .collect(Collectors.toList());
    }

    // ---------------------- RESUMO (placeholder) ----------------------

    public Object consultarResumo(Long usuarioId) {
        // valida existência do usuário
        usuarioRepository.findByIdOptional(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + usuarioId));

        // Implementar resumo consolidado quando necessário
        return null;
    }

    // ---------------------- MAPEADORES ----------------------

    private SaldoPontosDTO toSaldoPontosDTO(SaldoPontos s) {
        Long cartaoId = (s.cartao != null ? s.cartao.id : null);
        String status = safeStatusSaldo(s);
        return new SaldoPontosDTO(
                cartaoId,
                nvl(s.saldo),
                s.atualizadoEm,
                nvl(s.pontosExpirando30Dias),
                nvl(s.pontosExpirando60Dias),
                nvl(s.pontosExpirando90Dias),
                status
        );
    }

    private MovimentoPontosDTO toMovimentoPontosDTO(MovimentoPontos m) {
        Long usuarioId = (m.usuario != null ? m.usuario.id : null);
        Long cartaoId  = (m.cartao  != null ? m.cartao.id  : null);
        MovimentoPontos.TipoMovimento tipo = m.tipo;
        Integer pontos = (m.pontos  != null ? m.pontos : 0);
    
        return new MovimentoPontosDTO(
                m.id,
                usuarioId,
                cartaoId,
                tipo,
                pontos,                 // <- agora Integer
                m.refTransacaoId,
                m.observacao,
                m.criadoEm,
                m.jobId,
                m.regraAplicada,
                m.campanhaAplicada
        );
    }
    

    private String safeStatusSaldo(SaldoPontos s) {
        try {
            String st = s.getStatusSaldo();
            return (st == null || st.isBlank()) ? "SEM_PONTOS" : st;
        } catch (Exception e) {
            Long saldo = nvl(s.saldo);
            if (saldo <= 0) return "SEM_PONTOS";
            if (nvl(s.pontosExpirando30Dias) > 0) return "PONTOS_EXPIRANDO";
            if (saldo > 10000) return "ALTO";
            if (saldo > 1000)  return "MEDIO";
            return "BAIXO";
        }
    }

    // ---------------------- REGRAS AUXILIARES ----------------------

    private Long calcularSaldoInicial(Long usuarioId, Long cartaoId, LocalDate dataInicio) {
        if (dataInicio == null) return 0L;
        // Implementar cálculo real com somatório de movimentos anteriores à data quando necessário
        return 0L;
    }

    private MovimentoPontos.TipoMovimento parseTipoMovimento(String tipo) {
        if (tipo == null || tipo.isBlank()) return null;
        try {
            return MovimentoPontos.TipoMovimento.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null; // ignora valor inválido e não aplica filtro de tipo
        }
    }

    // ---------------------- PARSERS / HELPERS ----------------------

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato de data inválido (esperado yyyy-MM-dd): " + s);
        }
    }

    private LocalDateTime toStartOfDay(LocalDate d) {
        return (d == null) ? null : d.atStartOfDay();
    }

    private LocalDateTime toEndOfDay(LocalDate d) {
        return (d == null) ? null : d.atTime(23, 59, 59, 999_000_000);
    }

    private static Long nvl(Long v) { return v == null ? 0L : v; }
}
