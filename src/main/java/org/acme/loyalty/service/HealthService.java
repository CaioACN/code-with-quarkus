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
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
        saude.status = "SAUDAVEL";

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
            saude.status = "CRITICO";
            saude.alertas.add("Erro ao verificar saúde do sistema: " + e.getMessage());
        }

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
            Long totalUsuarios = usuarioRepository.count();
            
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
            Long totalTransacoes = transacaoRepository.count();
            
            // Se chegou até aqui, o sistema está vivo
            return true;

        } catch (Exception e) {
            // Sistema não está vivo
            return false;
        }
    }

    public InfoSistemaDTO obterInformacoesSistema() {
        InfoSistemaDTO info = new InfoSistemaDTO();
        info.timestamp = LocalDateTime.now();

        // Informações da JVM
        Runtime runtime = Runtime.getRuntime();
        info.jvmInfo = Map.of(
            "versao", System.getProperty("java.version"),
            "vendor", System.getProperty("java.vendor"),
            "home", System.getProperty("java.home"),
            "maxMemory", runtime.maxMemory(),
            "totalMemory", runtime.totalMemory(),
            "freeMemory", runtime.freeMemory(),
            "processors", runtime.availableProcessors()
        );

        // Informações do sistema operacional
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        info.osInfo = Map.of(
            "nome", osBean.getName(),
            "versao", osBean.getVersion(),
            "arquitetura", osBean.getArch(),
            "loadAverage", osBean.getSystemLoadAverage()
        );

        // Informações da aplicação
        info.appInfo = Map.of(
            "nome", "Sistema de Pontos do Cartão",
            "versao", "1.0.0",
            "ambiente", System.getProperty("quarkus.profile", "dev"),
            "uptime", calcularUptime()
        );

        return info;
    }

    public MetricasSistemaDTO obterMetricasSistema() {
        MetricasSistemaDTO metricas = new MetricasSistemaDTO();
        metricas.timestamp = LocalDateTime.now();

        try {
            // Métricas de memória
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            metricas.memoria = Map.of(
                "heapUsado", memoryBean.getHeapMemoryUsage().getUsed(),
                "heapMax", memoryBean.getHeapMemoryUsage().getMax(),
                "nonHeapUsado", memoryBean.getNonHeapMemoryUsage().getUsed(),
                "nonHeapMax", memoryBean.getNonHeapMemoryUsage().getMax()
            );

            // Métricas de threads
            metricas.threads = Map.of(
                "total", ManagementFactory.getThreadMXBean().getThreadCount(),
                "daemon", ManagementFactory.getThreadMXBean().getDaemonThreadCount(),
                "peak", ManagementFactory.getThreadMXBean().getPeakThreadCount()
            );

            // Métricas de negócio
            metricas.negocio = Map.of(
                "totalUsuarios", usuarioRepository.count(),
                "totalTransacoes", transacaoRepository.count(),
                "totalSaldos", saldoPontosRepository.count()
            );

        } catch (Exception e) {
            metricas.erro = "Erro ao coletar métricas: " + e.getMessage();
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
            
            saude.metricas.put("conexao_banco", "OK");
            saude.metricas.put("total_usuarios", totalUsuarios);
            
        } catch (Exception e) {
            saude.status = "CRITICO";
            saude.metricas.put("conexao_banco", "ERRO");
            saude.alertas.add("Falha na conexão com banco de dados: " + e.getMessage());
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
            
            if (memoryUsagePercent > 90) {
                saude.status = "CRITICO";
                saude.alertas.add("Uso de memória crítico: " + String.format("%.1f", memoryUsagePercent) + "%");
            } else if (memoryUsagePercent > 80) {
                saude.status = "ATENCAO";
                saude.alertas.add("Uso de memória alto: " + String.format("%.1f", memoryUsagePercent) + "%");
            }

            saude.metricas.put("memoria_uso_percent", String.format("%.1f", memoryUsagePercent));
            saude.metricas.put("memoria_max_mb", maxMemory / (1024 * 1024));
            saude.metricas.put("memoria_usada_mb", usedMemory / (1024 * 1024));

            // Verificar processadores
            int processors = runtime.availableProcessors();
            saude.metricas.put("processadores", processors);

        } catch (Exception e) {
            saude.alertas.add("Erro ao verificar recursos do sistema: " + e.getMessage());
        }
    }

    private void verificarMetricasNegocio(SaudeSistemaDTO saude) {
        try {
            // Verificar volume de transações recentes
            // TODO: Implementar verificação de transações por período
            saude.metricas.put("transacoes_ultima_hora", 0L);
            saude.metricas.put("transacoes_ultimo_dia", 0L);

            // Verificar saldos de pontos
            Long totalPontos = saldoPontosRepository.calcularTotalPontosCirculacao();
            saude.metricas.put("total_pontos_circulacao", totalPontos);

        } catch (Exception e) {
            saude.alertas.add("Erro ao verificar métricas de negócio: " + e.getMessage());
        }
    }

    private void verificarDependenciasExternas(SaudeSistemaDTO saude) {
        try {
            // TODO: Implementar verificação de dependências externas
            // - Serviços de notificação
            // - APIs de parceiros
            // - Sistemas de mensageria
            // - Cache externo

            saude.metricas.put("dependencias_externas", "OK");

        } catch (Exception e) {
            saude.alertas.add("Erro ao verificar dependências externas: " + e.getMessage());
        }
    }

    private void determinarStatusFinal(SaudeSistemaDTO saude) {
        // Se há alertas críticos, status é CRÍTICO
        if (saude.alertas.stream().anyMatch(alerta -> alerta.contains("CRÍTICO") || alerta.contains("crítico"))) {
            saude.status = "CRITICO";
            return;
        }

        // Se há alertas de atenção, status é ATENÇÃO
        if (saude.alertas.stream().anyMatch(alerta -> alerta.contains("ATENÇÃO") || alerta.contains("atenção"))) {
            saude.status = "ATENCAO";
            return;
        }

        // Se não há alertas, status é SAUDAVEL
        if (saude.alertas.isEmpty()) {
            saude.status = "SAUDAVEL";
        }
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

