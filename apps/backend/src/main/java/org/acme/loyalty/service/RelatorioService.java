package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.loyalty.dto.*;
import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.entity.Resgate;
import org.acme.loyalty.entity.Transacao;
import org.acme.loyalty.repository.*;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Serviço de relatórios – versão compatível com os DTOs atuais e com Resgate.StatusResgate.
 */
@ApplicationScoped
public class RelatorioService {

    @Inject UsuarioRepository usuarioRepository;
    @Inject SaldoPontosRepository saldoPontosRepository;
    @Inject MovimentoPontosRepository movimentoPontosRepository;
    @Inject TransacaoRepository transacaoRepository;
    @Inject ResgateRepository resgateRepository;

    // =====================================================================================
    // PONTOS
    // =====================================================================================

    public RelatorioPontosDTO gerarRelatorioPontosAcumulados(LocalDate dataInicio,
                                                             LocalDate dataFim,
                                                             Long cartaoId,
                                                             Long usuarioId) {

        LocalDateTime inicio = nvlInicio(dataInicio);
        LocalDateTime fim    = nvlFim(dataFim);

        RelatorioPontosDTO dto = new RelatorioPontosDTO();
        dto.periodoInicio = inicio;
        dto.periodoFim    = fim;
        dto.usuarioId     = usuarioId;
        dto.cartaoId      = cartaoId;
        dto.granularidade = "diario";

        List<MovimentoPontos> movimentos;
        if (usuarioId != null) {
            movimentos = movimentoPontosRepository.listByUsuarioAndPeriodo(usuarioId, inicio, fim);
        } else if (cartaoId != null) {
            movimentos = movimentoPontosRepository.listByCartaoAndPeriodo(cartaoId, inicio, fim);
        } else {
            movimentos = movimentoPontosRepository.find("criadoEm between ?1 and ?2", inicio, fim).list();
        }

        for (MovimentoPontos m : movimentos) {
            RelatorioPontosDTO.TipoMovimento tipo = mapTipoMov(m.tipo);
            long pontos = (m.pontos == null ? 0L : m.pontos);
            Long uid = (m.usuario != null ? m.usuario.id : null);
            Long cid = (m.cartao  != null ? m.cartao.id  : null);

            dto.addMovimento(
                    m.criadoEm,
                    uid, null, null,
                    cid, null,
                    tipo,
                    pontos
            );
        }

        dto.recompute();
        return dto;
    }

    public RelatorioPontosDTO gerarRelatorioPontosExpirados(LocalDate dataInicio,
                                                            LocalDate dataFim,
                                                            Long cartaoId,
                                                            Long usuarioId) {

        RelatorioPontosDTO dto = new RelatorioPontosDTO();
        dto.periodoInicio = nvlInicio(dataInicio);
        dto.periodoFim    = nvlFim(dataFim);
        dto.usuarioId     = usuarioId;
        dto.cartaoId      = cartaoId;

        // Somente usa campos agregados de saldo (placeholders seguros)
        if (usuarioId != null) {
            var saldos = saldoPontosRepository.listByUsuarioWithCartao(usuarioId);
            for (var s : saldos) {
                dto.expiracaoResumo.dias30 += nvl(s.pontosExpirando30Dias);
                dto.expiracaoResumo.dias60 += nvl(s.pontosExpirando60Dias);
                dto.expiracaoResumo.dias90 += nvl(s.pontosExpirando90Dias);
            }
        } else if (cartaoId != null) {
            var saldos = saldoPontosRepository.listByCartaoId(cartaoId);
            for (var s : saldos) {
                dto.expiracaoResumo.dias30 += nvl(s.pontosExpirando30Dias);
                dto.expiracaoResumo.dias60 += nvl(s.pontosExpirando60Dias);
                dto.expiracaoResumo.dias90 += nvl(s.pontosExpirando90Dias);
            }
        } else {
            var saldos = saldoPontosRepository.findAll().list();
            for (var s : saldos) {
                dto.expiracaoResumo.dias30 += nvl(s.pontosExpirando30Dias);
                dto.expiracaoResumo.dias60 += nvl(s.pontosExpirando60Dias);
                dto.expiracaoResumo.dias90 += nvl(s.pontosExpirando90Dias);
            }
        }

        dto.recompute();
        return dto;
    }

    // =====================================================================================
    // TRANSAÇÕES
    // =====================================================================================

    public RelatorioTransacoesDTO gerarRelatorioVolumeTransacoes(LocalDate dataInicio,
                                                                 LocalDate dataFim,
                                                                 String categoria,
                                                                 String mcc,
                                                                 Long parceiroId) {

        LocalDateTime inicio = nvlInicio(dataInicio);
        LocalDateTime fim    = nvlFim(dataFim);

        RelatorioTransacoesDTO dto = new RelatorioTransacoesDTO();
        dto.periodoInicio = inicio;
        dto.periodoFim    = fim;
        dto.agrupamento   = "diario";

        StringBuilder where = new StringBuilder("dataEvento >= ?1 and dataEvento <= ?2");
        List<Object> params = new ArrayList<>(List.of(inicio, fim));

        if (categoria != null && !categoria.isBlank()) {
            where.append(" and lower(categoria) like ?").append(params.size() + 1);
            params.add("%" + categoria.trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (mcc != null && !mcc.isBlank()) {
            where.append(" and mcc = ?").append(params.size() + 1);
            params.add(mcc.trim());
        }
        if (parceiroId != null) {
            where.append(" and parceiroId = ?").append(params.size() + 1);
            params.add(parceiroId);
        }

        List<Transacao> transacoes = transacaoRepository.find(where.toString(), params.toArray()).list();

        for (Transacao t : transacoes) {
            dto.addRegistro(
                    t.dataEvento,
                    t.moeda,
                    t.valor,
                    t.pontosGerados,
                    t.mcc,
                    t.categoria,
                    t.parceiroId,
                    (t.usuario != null ? t.usuario.id : null),
                    null,
                    mapTransacaoStatus(t.status)
            );
        }

        dto.recomputeAndTrim();
        return dto;
    }

    // =====================================================================================
    // RESGATES (usa Resgate.StatusResgate)
    // =====================================================================================

    public RelatorioResgatesDTO gerarRelatorioStatusResgates(LocalDate dataInicio,
                                                             LocalDate dataFim,
                                                             String status,
                                                             Long recompensaId) {

        LocalDateTime inicio = nvlInicio(dataInicio);
        LocalDateTime fim    = nvlFim(dataFim);

        RelatorioResgatesDTO dto = new RelatorioResgatesDTO();
        dto.periodoInicio = inicio;
        dto.periodoFim    = fim;

        StringBuilder where = new StringBuilder("criadoEm between ?1 and ?2");
        List<Object> params = new ArrayList<>(List.of(inicio, fim));

        // Converte string -> enum; só aplica filtro se for válido
        Resgate.StatusResgate parsed = tryEnum(StatusWrapper.RESGATE, status);
        if (parsed != null) {
            where.append(" and status = ?").append(params.size() + 1);
            params.add(parsed);
        }
        if (recompensaId != null) {
            where.append(" and recompensa.id = ?").append(params.size() + 1);
            params.add(recompensaId);
        }

        List<Resgate> resgates = resgateRepository.find(where.toString(), params.toArray()).list();

        for (Resgate r : resgates) {
            dto.addRegistro(
                    r.criadoEm,
                    r.aprovadoEm,
                    r.concluidoEm,
                    mapResgateStatus(r.status),
                    (r.pontosUtilizados == null ? 0L : r.pontosUtilizados),
                    (r.recompensa != null ? r.recompensa.id : null),
                    (r.recompensa != null ? r.recompensa.descricao : null),
                    (r.recompensa != null && r.recompensa.tipo != null ? r.recompensa.tipo.name() : null),
                    (r.recompensa != null ? r.recompensa.parceiroId : null),
                    (r.usuario != null ? r.usuario.id : null),
                    null
            );
        }

        dto.recomputeAndTrim();
        return dto;
    }

    // =====================================================================================
    // EFETIVIDADE / RANKING – esqueleto
    // =====================================================================================

    public RelatorioEfetividadeDTO gerarRelatorioEfetividadeRegras(LocalDate dataInicio,
                                                                   LocalDate dataFim,
                                                                   Long regraId,
                                                                   Long campanhaId) {
        LocalDateTime inicio = nvlInicio(dataInicio);
        LocalDateTime fim    = nvlFim(dataFim);
        return RelatorioEfetividadeDTO.fromRegra(null, inicio, fim, "diario");
    }

    public RelatorioEfetividadeDTO gerarRelatorioEfetividadeCampanhas(LocalDate dataInicio,
                                                                      LocalDate dataFim,
                                                                      Long campanhaId,
                                                                      String segmento) {
        RelatorioEfetividadeDTO dto = new RelatorioEfetividadeDTO();
        dto.periodoInicio = nvlInicio(dataInicio);
        dto.periodoFim    = nvlFim(dataFim);
        dto.granularidade = "diario";
        return dto;
    }

    public RelatorioRankingDTO gerarRelatorioRankingUsuarios(LocalDate dataInicio,
                                                             LocalDate dataFim,
                                                             String criterio,
                                                             Integer limite) {
        RelatorioRankingDTO dto = new RelatorioRankingDTO();
        dto.periodoInicio = nvlInicio(dataInicio);
        dto.periodoFim    = nvlFim(dataFim);
        dto.limite        = limite;
        try {
            if (criterio != null && !criterio.isBlank()) {
                dto.criterio = RelatorioRankingDTO.Criterio.valueOf(criterio.trim().toUpperCase(Locale.ROOT));
            }
        } catch (Exception ignored) {}
        dto.finalizeRanking();
        return dto;
    }

    // =====================================================================================
    // Exportação / Listagens
    // =====================================================================================

    public ExportacaoRelatorioDTO exportarRelatorio(String tipoRelatorio,
                                                    Map<String, Object> filtros,
                                                    String formato) {
        ExportacaoRelatorioDTO exportacao = new ExportacaoRelatorioDTO();
        exportacao.tipoRelatorio = tipoRelatorio;
        exportacao.formato = formato;
        exportacao.filtros = filtros;
        exportacao.solicitadoEm = LocalDateTime.now();
        exportacao.status = "PROCESSANDO";

        try {
            switch (String.valueOf(tipoRelatorio).toUpperCase(Locale.ROOT)) {
                case "PONTOS_ACUMULADOS":
                case "PONTOS_EXPIRADOS":
                case "VOLUME_TRANSACOES":
                case "STATUS_RESGATES":
                case "EFETIVIDADE_REGRAS":
                case "EFETIVIDADE_CAMPANHAS":
                case "RANKING_USUARIOS":
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de relatório não suportado: " + tipoRelatorio);
            }
            exportacao.status = "CONCLUIDO";
            exportacao.arquivoUrl = "/relatorios/" + tipoRelatorio.toLowerCase(Locale.ROOT) + "_"
                    + System.currentTimeMillis() + "." + formato.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            exportacao.status = "ERRO";
            exportacao.erro = e.getMessage();
        }

        return exportacao;
    }

    public List<String> listarTiposRelatorio() {
        return List.of(
                "PONTOS_ACUMULADOS",
                "PONTOS_EXPIRADOS",
                "VOLUME_TRANSACOES",
                "STATUS_RESGATES",
                "EFETIVIDADE_REGRAS",
                "EFETIVIDADE_CAMPANHAS",
                "RANKING_USUARIOS"
        );
    }

    public List<String> listarFormatosExportacao() {
        return List.of("CSV", "EXCEL", "PDF");
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private static LocalDateTime nvlInicio(LocalDate d) {
        return (d == null ? LocalDate.now().minusDays(30) : d).atStartOfDay();
    }

    private static LocalDateTime nvlFim(LocalDate d) {
        LocalDate ref = (d == null ? LocalDate.now() : d);
        return LocalDateTime.of(ref, LocalTime.of(23, 59, 59));
    }

    private static long nvl(Long v) { return v == null ? 0L : v; }

    private static RelatorioPontosDTO.TipoMovimento mapTipoMov(MovimentoPontos.TipoMovimento t) {
        if (t == null) return RelatorioPontosDTO.TipoMovimento.AJUSTE;
        try {
            return RelatorioPontosDTO.TipoMovimento.valueOf(t.name());
        } catch (Exception ignore) {
            return RelatorioPontosDTO.TipoMovimento.AJUSTE;
        }
    }

    private static RelatorioTransacoesDTO.StatusTransacao mapTransacaoStatus(Transacao.StatusTransacao st) {
        if (st == null) return RelatorioTransacoesDTO.StatusTransacao.PENDENTE;
        try {
            return RelatorioTransacoesDTO.StatusTransacao.valueOf(st.name());
        } catch (Exception ignore) {
            return RelatorioTransacoesDTO.StatusTransacao.PENDENTE;
        }
    }

    /** Wrapper só para diferenciar sobrecargas do tryEnum. */
    private enum StatusWrapper { RESGATE }

    private static Resgate.StatusResgate tryEnum(StatusWrapper unused, String v) {
        if (v == null || v.isBlank()) return null;
    
        // Normaliza/remova acentos e padroniza para UPPER
        String norm = Normalizer.normalize(v, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toUpperCase(Locale.ROOT);
    
        // Aceita alguns sinônimos/comuns
        switch (norm) {
            case "PENDENTE":
                return Resgate.StatusResgate.PENDENTE;
            case "APROVADO":
            case "APROVADA":
                return Resgate.StatusResgate.APROVADO;
            case "CONCLUIDO":
            case "CONCLUIDA":
                return Resgate.StatusResgate.CONCLUIDO;
            case "NEGADO":
            case "NEGADA":
            case "REJEITADO":
            case "REJEITADA":
                return Resgate.StatusResgate.NEGADO;
            case "CANCELADO":
            case "CANCELADA":
                return Resgate.StatusResgate.CANCELADO;
            default:
                // fallback: tenta casar exatamente com o enum após normalização
                try {
                    return Resgate.StatusResgate.valueOf(norm);
                } catch (Exception ex) {
                    return null;
                }
        }
    }

    private static RelatorioResgatesDTO.Status mapResgateStatus(Resgate.StatusResgate st) {
        if (st == null) return RelatorioResgatesDTO.Status.PENDENTE;
        try {
            return RelatorioResgatesDTO.Status.valueOf(st.name());
        } catch (Exception ignore) {
            return RelatorioResgatesDTO.Status.PENDENTE;
        }
    }
}
