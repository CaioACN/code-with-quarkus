package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.acme.loyalty.dto.DashboardDTO;
import org.acme.loyalty.dto.EstatisticasDTO;
import org.acme.loyalty.dto.AjustePontosDTO;
import org.acme.loyalty.dto.AuditoriaUsuarioDTO;
import org.acme.loyalty.dto.SaudeSistemaDTO;
import org.acme.loyalty.entity.Usuario;
import org.acme.loyalty.entity.SaldoPontos;
import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.repository.UsuarioRepository;
import org.acme.loyalty.repository.SaldoPontosRepository;
import org.acme.loyalty.repository.MovimentoPontosRepository;
import org.acme.loyalty.repository.TransacaoRepository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminService {

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    SaldoPontosRepository saldoPontosRepository;

    @Inject
    MovimentoPontosRepository movimentoPontosRepository;

    @Inject
    TransacaoRepository transacaoRepository;

    public DashboardDTO consultarDashboard() {
        // TODO: Implementar dashboard administrativo
        // - Total de usuários ativos
        // - Total de pontos em circulação
        // - Volume de transações (hoje, semana, mês)
        // - Resgates pendentes
        // - Alertas de sistema

        DashboardDTO dashboard = new DashboardDTO();
        dashboard.totalUsuarios = usuarioRepository.countUsuariosAtivos();
        dashboard.totalPontosCirculacao = saldoPontosRepository.calcularTotalPontosCirculacao();
        dashboard.transacoesHoje = transacaoRepository.countTransacoesPorData(LocalDate.now());
        dashboard.resgatesPendentes = 0L; // TODO: Implementar

        return dashboard;
    }

    public EstatisticasDTO consultarEstatisticas(String periodo, LocalDate dataInicio, LocalDate dataFim) {
        // TODO: Implementar estatísticas detalhadas
        // - Volume de transações por período
        // - Pontos acumulados vs resgatados
        // - Top categorias/MCCs
        // - Performance por parceiro
        // - Taxa de conversão de resgates

        EstatisticasDTO estatisticas = new EstatisticasDTO();
        estatisticas.periodo = periodo;
        estatisticas.dataInicio = dataInicio;
        estatisticas.dataFim = dataFim;

        // Calcular estatísticas básicas
        if (dataInicio != null && dataFim != null) {
            estatisticas.totalTransacoes = transacaoRepository.countTransacoesPorPeriodo(dataInicio, dataFim);
            estatisticas.pontosAcumulados = movimentoPontosRepository.calcularPontosAcumulados(dataInicio, dataFim);
            estatisticas.pontosResgatados = movimentoPontosRepository.calcularPontosResgatados(dataInicio, dataFim);
        }

        return estatisticas;
    }

    @Transactional
    public void ajustarPontos(AjustePontosDTO ajuste) {
        // Validar dados do ajuste
        validarAjustePontos(ajuste);

        // Buscar usuário
        Usuario usuario = usuarioRepository.findByIdOptional(ajuste.usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado: " + ajuste.usuarioId));

        // Buscar saldo do cartão
        SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(ajuste.usuarioId, ajuste.cartaoId)
                .orElseThrow(() -> new NotFoundException("Saldo não encontrado para o cartão: " + ajuste.cartaoId));

        // Aplicar ajuste
        saldo.saldo += ajuste.pontos;
        saldo.atualizadoEm = LocalDateTime.now();

        // Persistir alteração
        saldoPontosRepository.persist(saldo);

        // Registrar movimento de ajuste
        MovimentoPontos movimento = new MovimentoPontos();
        movimento.usuario = usuario;
        movimento.cartao = saldo.cartao;
        movimento.tipo = "AJUSTE";
        movimento.pontos = ajuste.pontos;
        movimento.observacao = ajuste.motivo;
        movimento.criadoEm = LocalDateTime.now();
        movimento.jobId = ajuste.jobId;

        movimentoPontosRepository.persist(movimento);

        // TODO: Publicar evento PontosAjustados
        // eventPublisherService.publishEvent(new PontosAjustadosEvent(...));
    }

    @Transactional
    public void estornarPontos(Long movimentoId, String motivo) {
        MovimentoPontos movimento = movimentoPontosRepository.findByIdOptional(movimentoId)
                .orElseThrow(() -> new NotFoundException("Movimento não encontrado: " + movimentoId));

        // Validar se movimento pode ser estornado
        if (!"ACUMULO".equals(movimento.tipo)) {
            throw new IllegalArgumentException("Apenas movimentos de acúmulo podem ser estornados");
        }

        // Buscar saldo
        SaldoPontos saldo = saldoPontosRepository.findByUsuarioAndCartao(
            movimento.usuario.id, movimento.cartao.id
        ).orElseThrow(() -> new NotFoundException("Saldo não encontrado"));

        // Aplicar estorno
        saldo.saldo -= movimento.pontos;
        saldo.atualizadoEm = LocalDateTime.now();

        saldoPontosRepository.persist(saldo);

        // Registrar movimento de estorno
        MovimentoPontos estorno = new MovimentoPontos();
        estorno.usuario = movimento.usuario;
        estorno.cartao = movimento.cartao;
        estorno.tipo = "ESTORNO";
        estorno.pontos = -movimento.pontos;
        estorno.refTransacaoId = movimento.refTransacaoId;
        estorno.observacao = "Estorno: " + motivo;
        estorno.criadoEm = LocalDateTime.now();

        movimentoPontosRepository.persist(estorno);

        // TODO: Publicar evento PontosEstornados
        // eventPublisherService.publishEvent(new PontosEstornadosEvent(...));
    }

    public List<AuditoriaUsuarioDTO> consultarAuditoriaUsuario(Long usuarioId, LocalDate dataInicio, LocalDate dataFim) {
        // TODO: Implementar auditoria de usuário
        // - Histórico de transações
        // - Movimentos de pontos
        // - Resgates realizados
        // - Ajustes aplicados
        // - Logs de acesso

        List<AuditoriaUsuarioDTO> auditoria = List.of(); // Placeholder

        return auditoria;
    }

    public void executarManutencao(String tipo, Map<String, Object> parametros) {
        // TODO: Implementar operações de manutenção
        // - Limpeza de logs antigos
        // - Reindexação de tabelas
        // - Validação de integridade
        // - Backup de dados
        // - Limpeza de cache

        switch (tipo) {
            case "LIMPEZA_LOGS":
                // executarLimpezaLogs(parametros);
                break;
            case "VALIDACAO_INTEGRIDADE":
                // executarValidacaoIntegridade(parametros);
                break;
            case "REINDEXACAO":
                // executarReindexacao(parametros);
                break;
            default:
                throw new IllegalArgumentException("Tipo de manutenção não suportado: " + tipo);
        }
    }

    public SaudeSistemaDTO consultarSaudeSistema() {
        // TODO: Implementar verificação de saúde do sistema
        // - Status das conexões de banco
        // - Uso de memória e CPU
        // - Latência das operações
        // - Taxa de erros
        // - Status dos serviços externos

        SaudeSistemaDTO saude = new SaudeSistemaDTO();
        saude.status = "SAUDAVEL";
        saude.timestamp = LocalDateTime.now();

        // Verificações básicas
        try {
            // Testar conexão com banco
            Long totalUsuarios = usuarioRepository.count();
            saude.metricas.put("total_usuarios", totalUsuarios);
            saude.metricas.put("conexao_banco", "OK");
        } catch (Exception e) {
            saude.status = "CRITICO";
            saude.metricas.put("conexao_banco", "ERRO");
            saude.alertas.add("Falha na conexão com banco de dados: " + e.getMessage());
        }

        return saude;
    }

    public Map<String, Object> consultarMetricas() {
        // TODO: Implementar métricas detalhadas
        // - Throughput de transações
        // - Latência média/p95/p99
        // - Taxa de erro por endpoint
        // - Uso de recursos do sistema
        // - Métricas de negócio

        Map<String, Object> metricas = Map.of(
            "timestamp", LocalDateTime.now(),
            "status", "COLETANDO"
        );

        return metricas;
    }

    private void validarAjustePontos(AjustePontosDTO ajuste) {
        if (ajuste.usuarioId == null) {
            throw new IllegalArgumentException("ID do usuário é obrigatório");
        }

        if (ajuste.cartaoId == null) {
            throw new IllegalArgumentException("ID do cartão é obrigatório");
        }

        if (ajuste.pontos == null) {
            throw new IllegalArgumentException("Quantidade de pontos é obrigatória");
        }

        if (ajuste.motivo == null || ajuste.motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Motivo do ajuste é obrigatório");
        }

        if (ajuste.jobId == null || ajuste.jobId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID do job é obrigatório");
        }
    }
}

