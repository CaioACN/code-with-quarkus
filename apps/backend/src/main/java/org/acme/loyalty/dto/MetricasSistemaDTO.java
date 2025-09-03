package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.lang.management.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Snapshot de métricas do runtime da aplicação. DTO simples (sem dependência de Micrometer),
 * para ser preenchido por um serviço/collector e exposto em endpoints como /metrics/system.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "MetricasSistema", description = "Métricas de runtime, pool de conexões, HTTP, mensageria e timers do app")
public class MetricasSistemaDTO {

    // ====== Cabeçalho / tempo ======
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Instante de coleta")
    public LocalDateTime timestamp;

    @Schema(description = "Uptime da JVM em segundos")
    public Long uptimeSeconds;

    // ====== CPU / SO ======
    @Schema(description = "Carga média do sistema (1 min) se disponível")
    public Double systemLoadAverage;

    @Schema(description = "Carga de CPU do processo (0..1), se disponível")
    public Double processCpuLoad;

    @Schema(description = "Carga de CPU do sistema (0..1), se disponível")
    public Double systemCpuLoad;

    // ====== Threads ======
    @Schema(description = "Total de threads")
    public Integer threadCount;

    @Schema(description = "Threads daemon")
    public Integer daemonThreadCount;

    @Schema(description = "Pico de threads")
    public Integer peakThreadCount;

    // ====== Memória ======
    @Schema(description = "Uso de memória")
    public MemoryMetrics memory;

    // ====== HTTP (resumo) ======
    @Schema(description = "Requisições HTTP por status (ex.: 2xx, 4xx, 5xx)")
    public Map<String, Long> httpReqPorStatus;

    @Schema(description = "Requisições HTTP ativas no momento da coleta")
    public Integer httpAtivas;

    @Schema(description = "Latência de requisições HTTP (ms)")
    public TimerSummary httpLatency;

    // ====== Banco de Dados (pool) ======
    @Schema(description = "Pool de conexões principal")
    public PoolMetrics dbPool;

    // ====== Mensageria (Kafka/Rabbit) ======
    @Schema(description = "Lags por tópico/consumidor (se aplicável)")
    public Map<String, Long> kafkaLagPorTopico;

    @Schema(description = "Mensagens processadas por tópico no período (se aplicável)")
    public Map<String, Long> kafkaMsgsPorTopico;

    // ====== Contadores / Medidas genéricas ======
    @Schema(description = "Contadores arbitrários (chave -> valor)")
    public Map<String, Long> counters;

    @Schema(description = "Medidas (gauges) arbitrárias (chave -> valor)")
    public Map<String, Double> gauges;

    // ====== Timers por nome (ex.: motor de pontos, resgates, etc.) ======
    @Schema(description = "Timers (latências) arbitrários por nome")
    public Map<String, TimerSummary> timers;

    public MetricasSistemaDTO() {}

    // =====================================================================================
    // Tipos auxiliares
    // =====================================================================================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Resumo de um timer (latência em ms)")
    public static class TimerSummary {
        @Schema(description = "Quantidade de medições")
        public long count;

        @Schema(description = "Soma das durações (ms)")
        public long sumMs;

        @Schema(description = "Menor duração (ms)")
        public long minMs = Long.MAX_VALUE;

        @Schema(description = "Maior duração (ms)")
        public long maxMs = Long.MIN_VALUE;

        // Campos opcionais para quantis (se o coletor calcular externamente)
        @Schema(description = "p50 (ms, opcional)")
        public Long p50ms;

        @Schema(description = "p95 (ms, opcional)")
        public Long p95ms;

        @Schema(description = "p99 (ms, opcional)")
        public Long p99ms;

        public void record(long durationMs) {
            count++;
            sumMs += durationMs;
            if (durationMs < minMs) minMs = durationMs;
            if (durationMs > maxMs) maxMs = durationMs;
        }

        @Schema(description = "Média (ms) calculada on-the-fly")
        public double avgMs() {
            return count == 0 ? 0.0 : (double) sumMs / (double) count;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Métricas do pool de conexões")
    public static class PoolMetrics {
        @Schema(description = "Conexões ativas")
        public Integer active;

        @Schema(description = "Conexões ociosas")
        public Integer idle;

        @Schema(description = "Máximo do pool")
        public Integer max;

        @Schema(description = "Mínimo do pool")
        public Integer min;

        @Schema(description = "Solicitações aguardando conexão")
        public Integer pending;

        @Schema(description = "Percentual de uso (active / max)")
        public Double usage() {
            if (active == null || max == null || max == 0) return 0.0;
            return (double) active / (double) max;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Uso de memória")
    public static class MemoryMetrics {
        @Schema(description = "Heap usado (bytes)")
        public Long heapUsed;

        @Schema(description = "Heap máximo (bytes)")
        public Long heapMax;

        @Schema(description = "Non-heap usado (bytes)")
        public Long nonHeapUsed;

        @Schema(description = "Memória do processo usada (bytes)")
        public Long processUsed;

        @Schema(description = "Memória total do processo (bytes)")
        public Long processTotal;
    }

    // =====================================================================================
    // Métodos utilitários (para preencher/atualizar o DTO)
    // =====================================================================================

    public MetricasSistemaDTO addCounter(String key, long delta) {
        if (counters == null) counters = new LinkedHashMap<>();
        counters.merge(key, delta, Long::sum);
        return this;
    }

    public MetricasSistemaDTO setGauge(String key, double value) {
        if (gauges == null) gauges = new LinkedHashMap<>();
        gauges.put(key, value);
        return this;
    }

    public MetricasSistemaDTO recordTimer(String name, long durationMs) {
        if (timers == null) timers = new LinkedHashMap<>();
        timers.computeIfAbsent(name, k -> new TimerSummary()).record(durationMs);
        return this;
    }

    public MetricasSistemaDTO addHttpStatus(String statusClass, long inc) {
        if (httpReqPorStatus == null) httpReqPorStatus = new LinkedHashMap<>();
        httpReqPorStatus.merge(statusClass, inc, Long::sum);
        return this;
    }

    // =====================================================================================
    // Captura básica (JVM/threads/memória/SO) — sem dependências externas
    // =====================================================================================

    /**
     * Coleta métricas básicas da JVM e SO. Campos de pool/HTTP/mensageria permanecem nulos.
     */
    public static MetricasSistemaDTO captureBasic() {
        MetricasSistemaDTO dto = new MetricasSistemaDTO();

        final RuntimeMXBean rtMx = ManagementFactory.getRuntimeMXBean();
        final OperatingSystemMXBean osMx = ManagementFactory.getOperatingSystemMXBean();
        final ThreadMXBean thMx = ManagementFactory.getThreadMXBean();
        final MemoryMXBean memMx = ManagementFactory.getMemoryMXBean();

        dto.timestamp = LocalDateTime.now();
        dto.uptimeSeconds = rtMx.getUptime() / 1000;

        // SO / CPU
        try {
            dto.systemLoadAverage = osMx.getSystemLoadAverage();
        } catch (Throwable ignored) {}

        // Tenta acessar com.sun.management.OperatingSystemMXBean via reflexão para CPU load (portable-ish)
        try {
            Class<?> sunOsClazz = Class.forName("com.sun.management.OperatingSystemMXBean");
            if (sunOsClazz.isInstance(osMx)) {
                Object sunOs = sunOsClazz.cast(osMx);
                dto.processCpuLoad = (Double) sunOsClazz.getMethod("getProcessCpuLoad").invoke(sunOs);
                dto.systemCpuLoad  = (Double) sunOsClazz.getMethod("getSystemCpuLoad").invoke(sunOs);
            }
        } catch (Throwable ignored) {}

        // Threads
        dto.threadCount = thMx.getThreadCount();
        dto.daemonThreadCount = thMx.getDaemonThreadCount();
        dto.peakThreadCount = thMx.getPeakThreadCount();

        // Memória
        MemoryUsage heap = memMx.getHeapMemoryUsage();
        MemoryUsage non  = memMx.getNonHeapMemoryUsage();
        Runtime rt = Runtime.getRuntime();

        dto.memory = new MemoryMetrics();
        dto.memory.heapUsed = heap != null ? heap.getUsed() : null;
        dto.memory.heapMax = heap != null ? heap.getMax() : null;
        dto.memory.nonHeapUsed = non != null ? non.getUsed() : null;
        dto.memory.processTotal = rt.totalMemory();
        dto.memory.processUsed = rt.totalMemory() - rt.freeMemory();

        return dto;
    }

    /**
     * Variante que também preenche HTTP e Pool de banco (valores fornecidos pelo chamador).
     */
    public static MetricasSistemaDTO captureWithHttpAndDb(
            Integer httpAtivas,
            Map<String, Long> httpPorStatus,
            PoolMetrics pool) {

        MetricasSistemaDTO dto = captureBasic();
        dto.httpAtivas = httpAtivas;
        dto.httpReqPorStatus = httpPorStatus == null ? Map.of() : new LinkedHashMap<>(httpPorStatus);
        dto.dbPool = pool;
        return dto;
    }

    // =====================================================================================
    // Helpers estáticos opcionais
    // =====================================================================================

    public static PoolMetrics pool(Integer active, Integer idle, Integer max, Integer min, Integer pending) {
        PoolMetrics p = new PoolMetrics();
        p.active = active;
        p.idle = idle;
        p.max = max;
        p.min = min;
        p.pending = pending;
        return p;
    }

    /** Constrói um TimerSummary preenchido a partir de uma coleção de durações (ms). */
    public static TimerSummary timerFromDurations(Collection<Long> durationsMs) {
        TimerSummary t = new TimerSummary();
        if (durationsMs == null || durationsMs.isEmpty()) return t;
        ArrayList<Long> list = new ArrayList<>(durationsMs);
        Collections.sort(list);
        for (Long d : list) t.record(d);
        // Percentis simples (aproximação por posição)
        t.p50ms = percentile(list, 0.50);
        t.p95ms = percentile(list, 0.95);
        t.p99ms = percentile(list, 0.99);
        return t;
    }

    private static Long percentile(List<Long> sorted, double p) {
        if (sorted == null || sorted.isEmpty()) return 0L;
        int idx = (int) Math.ceil(p * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

    /** Converte epochMillis para LocalDateTime no fuso local. */
    public static LocalDateTime toLocalDateTime(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
