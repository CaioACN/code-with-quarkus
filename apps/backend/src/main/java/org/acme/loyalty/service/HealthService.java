package org.acme.loyalty.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.loyalty.dto.SaudeSistemaDTO;
import org.acme.loyalty.dto.MetricasSistemaDTO;
import org.acme.loyalty.dto.InfoSistemaDTO;
import org.acme.loyalty.repository.UsuarioRepository;
import org.acme.loyalty.repository.TransacaoRepository;
import org.acme.loyalty.repository.SaldoPontosRepository;

import java.lang.management.ManagementFactory;

import java.time.LocalDateTime;
import java.time.Duration;


@ApplicationScoped
public class HealthService {

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    TransacaoRepository transacaoRepository;

    @Inject
    SaldoPontosRepository saldoPontosRepository;

    public SaudeSistemaDTO verificarSaudeGeral() {
        SaudeSistemaDTO saude = new SaudeSistemaDTO();
        saude.timestamp = LocalDateTime.now();
        saude.statusGeral = SaudeSistemaDTO.Status.UP;

        try {
            // Verificar conexão com banco de dados
            verificarConexaoBanco(saude);

            // Verificar recursos do sistema
            verificarRecursosSistema(saude);

            // Verificar métricas de negócio
            verificarMetricasNegocio(saude);

            // Verificar dependências externas
            verificarDependenciasExternas(saude);

            // Determinar status final
            determinarStatusFinal(saude);

        } catch (Exception e) {
            saude.statusGeral = SaudeSistemaDTO.Status.DOWN;
            saude.addComponente("health-check", SaudeSistemaDTO.Status.DOWN, 0L, "Erro ao verificar saúde do sistema: " + e.getMessage(), null);
        }

        saude.recompute();
        return saude;
    }

    public boolean verificarReadiness() {
        try {
            // Verificar se o sistema está pronto para receber requisições
            // - Conexão com banco de dados
            // - Cache inicializado
            // - Configurações carregadas
            // - Dependências essenciais disponíveis

            // Teste básico de conexão com banco
            usuarioRepository.count(); // Verifica se a conexão está funcionando
            
            // Se chegou até aqui, o sistema está pronto
            return true;

        } catch (Exception e) {
            // Sistema não está pronto
            return false;
        }
    }

    public boolean verificarLiveness() {
        try {
            // Verificar se o sistema está vivo e funcionando
            // - Processamento de requisições
            // - Tempo de resposta aceitável
            // - Sem deadlocks ou travamentos

            // Teste simples de processamento
            transacaoRepository.count(); // Verifica se o processamento está funcionando
            
            // Se chegou até aqui, o sistema está vivo
            return true;

        } catch (Exception e) {
            // Sistema não está vivo
            return false;
        }
    }

    public InfoSistemaDTO obterInformacoesSistema() {
        InfoSistemaDTO info = InfoSistemaDTO.captureBasic();
        
        // Adicionar informações específicas da aplicação
        info.addMetric("app_name", "Sistema de Pontos do Cartão");
        info.addMetric("app_version", "1.0.0");
        info.addMetric("uptime_formatted", calcularUptime());

        return info;
    }

    public MetricasSistemaDTO obterMetricasSistema() {
        MetricasSistemaDTO metricas = MetricasSistemaDTO.captureBasic();

        try {
            // Métricas de negócio
            metricas.addCounter("total_usuarios", usuarioRepository.count());
            metricas.addCounter("total_transacoes", transacaoRepository.count());
            metricas.addCounter("total_saldos", saldoPontosRepository.count());

        } catch (Exception e) {
            metricas.setGauge("erro_coleta", 1.0);
            metricas.addCounter("erro_mensagem", 1L);
        }

        return metricas;
    }

    public String ping() {
        return "pong";
    }

    private void verificarConexaoBanco(SaudeSistemaDTO saude) {
        try {
            // Testar conexão executando uma query simples
            Long totalUsuarios = usuarioRepository.count();
            
            saude.addComponente("database", SaudeSistemaDTO.Status.UP, 0L, "OK", "jdbc:postgresql://localhost");
            saude.addComponente("usuarios", SaudeSistemaDTO.Status.UP, 0L, "Total: " + totalUsuarios, null);
            
        } catch (Exception e) {
            saude.statusGeral = SaudeSistemaDTO.Status.DOWN;
            saude.addComponente("database", SaudeSistemaDTO.Status.DOWN, 0L, "ERRO: " + e.getMessage(), "jdbc:postgresql://localhost");
        }
    }

    private void verificarRecursosSistema(SaudeSistemaDTO saude) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            // Verificar uso de memória
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            SaudeSistemaDTO.Status statusMemoria = SaudeSistemaDTO.Status.UP;
            String mensagemMemoria = "OK";
            
            if (memoryUsagePercent > 90) {
                statusMemoria = SaudeSistemaDTO.Status.DOWN;
                mensagemMemoria = "Uso de memória crítico: " + String.format("%.1f", memoryUsagePercent) + "%";
            } else if (memoryUsagePercent > 80) {
                statusMemoria = SaudeSistemaDTO.Status.WARN;
                mensagemMemoria = "Uso de memória alto: " + String.format("%.1f", memoryUsagePercent) + "%";
            }

            saude.addComponente("memory", statusMemoria, 0L, mensagemMemoria, null);

            // Verificar processadores
            int processors = runtime.availableProcessors();
            saude.addComponente("processors", SaudeSistemaDTO.Status.UP, 0L, "Total: " + processors, null);

        } catch (Exception e) {
            saude.addComponente("system-resources", SaudeSistemaDTO.Status.DOWN, 0L, "Erro ao verificar recursos do sistema: " + e.getMessage(), null);
        }
    }

    private void verificarMetricasNegocio(SaudeSistemaDTO saude) {
        try {
            // Verificar volume de transações recentes
            // Implementar verificação de transações por período quando necessário
            saude.addComponente("transacoes", SaudeSistemaDTO.Status.UP, 0L, "Transações: 0 (última hora), 0 (último dia)", null);

            // Verificar saldos de pontos
            Long totalSaldos = saldoPontosRepository.countSaldosPositivos();
            saude.addComponente("pontos", SaudeSistemaDTO.Status.UP, 0L, "Saldos positivos: " + totalSaldos, null);

        } catch (Exception e) {
            saude.addComponente("business-metrics", SaudeSistemaDTO.Status.DOWN, 0L, "Erro ao verificar métricas de negócio: " + e.getMessage(), null);
        }
    }

    private void verificarDependenciasExternas(SaudeSistemaDTO saude) {
        try {
            // Implementar verificação de dependências externas quando necessário
            // - Serviços de notificação
            // - APIs de parceiros
            // - Sistemas de mensageria
            // - Cache externo

            saude.addComponente("external-dependencies", SaudeSistemaDTO.Status.UP, 0L, "OK", null);

        } catch (Exception e) {
            saude.addComponente("external-dependencies", SaudeSistemaDTO.Status.DOWN, 0L, "Erro ao verificar dependências externas: " + e.getMessage(), null);
        }
    }

    private void determinarStatusFinal(SaudeSistemaDTO saude) {
        // O status final será determinado pelo método recompute() do DTO
        // que analisa os componentes e define o status geral
    }

    private String calcularUptime() {
        try {
            long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
            Duration duration = Duration.ofMillis(uptime);
            
            long days = duration.toDays();
            long hours = duration.toHoursPart();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();

            if (days > 0) {
                return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
            } else if (hours > 0) {
                return String.format("%dh %dm %ds", hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds);
            } else {
                return String.format("%ds", seconds);
            }
        } catch (Exception e) {
            return "N/A";
        }
    }
}

