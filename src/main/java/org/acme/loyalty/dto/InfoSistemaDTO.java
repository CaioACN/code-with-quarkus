package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Informações de runtime do sistema para diagnósticos/observabilidade.
 * Pode ser usado numa rota tipo GET /system/info.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "InfoSistema", description = "Informações de versão, ambiente, uptime, memória e integrações")
public class InfoSistemaDTO {

    // ---- Identidade / versões
    @Schema(description = "Nome do aplicativo", example = "loyalty-service")
    public String appName;

    @Schema(description = "Versão do aplicativo", example = "1.0.0-SNAPSHOT")
    public String appVersion;

    @Schema(description = "Perfil/ambiente ativo (Quarkus profile)", example = "dev")
    public String environment;

    @Schema(description = "Versão do Quarkus", example = "3.25.0")
    public String quarkusVersion;

    @Schema(description = "Versão do Java em execução", example = "17.0.10")
    public String javaVersion;

    // ---- Tempo / uptime
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Instante de início da JVM")
    public LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Instante atual (no servidor)")
    public LocalDateTime now;

    @Schema(description = "Uptime em segundos")
    public Long uptimeSeconds;

    // ---- Build / Git (se disponíveis via propriedades do sistema/ENV)
    @Schema(description = "Informações de build")
    public BuildInfo build;

    // ---- Memória
    @Schema(description = "Uso de memória (heap / non-heap / process)")
    public MemoryInfo memory;

    // ---- Banco de dados (ex.: datasource principal)
    @Schema(description = "Informações do datasource")
    public DataSourceInfo datasource;

    // ---- Mensageria (Kafka/Rabbit etc.)
    @Schema(description = "Informações de mensageria")
    public MessagingInfo messaging;

    // ---- Health agregada (opcionalmente preenchida)
    @Schema(description = "Componentes de health checados")
    public List<HealthComponent> health;

    // ---- Métricas simples (chave/valor)
    @Schema(description = "Métricas adicionais")
    public Map<String, Object> metrics;

    // ===================== Tipos auxiliares =====================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BuildInfo {
        @Schema(description = "Commit/versão de origem", example = "abcdef1")
        public String commitId;

        @Schema(description = "Branch do repositório", example = "main")
        public String branch;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Instante de build (se informado via propriedades)")
        public LocalDateTime time;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MemoryInfo {
        @Schema(description = "Heap usado em bytes")
        public Long heapUsed;

        @Schema(description = "Heap máximo em bytes")
        public Long heapMax;

        @Schema(description = "Non-heap usado em bytes")
        public Long nonHeapUsed;

        @Schema(description = "Memória do processo usada (Runtime.totalMemory - freeMemory) em bytes")
        public Long processUsed;

        @Schema(description = "Memória total do processo (Runtime.totalMemory) em bytes")
        public Long processTotal;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataSourceInfo {
        @Schema(description = "Nome lógico do datasource", example = "default")
        public String name;

        @Schema(description = "URL mascarada", example = "jdbc:postgresql://db:5432/loyalty?user=app&password=***")
        public String urlMasked;

        @Schema(description = "Conexões ativas no pool (se disponível)")
        public Integer poolActive;

        @Schema(description = "Tamanho máximo do pool (se disponível)")
        public Integer poolMax;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessagingInfo {
        @Schema(description = "Kafka habilitado?")
        public Boolean kafkaEnabled;

        @Schema(description = "RabbitMQ habilitado?")
        public Boolean rabbitEnabled;

        @Schema(description = "Tópicos/filas relevantes")
        public List<String> endpoints;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HealthComponent {
        @Schema(description = "Nome do componente", example = "database")
        public String name;

        @Schema(description = "Status do componente", example = "UP")
        public String status;

        @Schema(description = "Detalhes (opcional)")
        public Map<String, Object> details;
    }

    public InfoSistemaDTO() {}

    // ===================== Factories =====================

    /**
     * Coleta informações básicas diretamente da JVM/SO.
     * Você pode complementar depois (datasource/messaging/health/metrics).
     */
    public static InfoSistemaDTO captureBasic() {
        InfoSistemaDTO dto = new InfoSistemaDTO();

        dto.appName = sysOrEnv("app.name", "APP_NAME", "loyalty-service");
        dto.appVersion = sysOrEnv("app.version", "APP_VERSION", null);
        dto.environment = sysOrEnv("quarkus.profile", "QUARKUS_PROFILE", null);
        dto.quarkusVersion = sysOrEnv("quarkus.version", "QUARKUS_VERSION", null);
        dto.javaVersion = System.getProperty("java.version");

        long startMs = ManagementFactory.getRuntimeMXBean().getStartTime();
        dto.startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startMs), ZoneId.systemDefault());
        dto.now = LocalDateTime.now();
        dto.uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        dto.build = new BuildInfo();
        dto.build.commitId = sysOrEnv("git.commit.id", "GIT_COMMIT", null);
        dto.build.branch = sysOrEnv("git.branch", "GIT_BRANCH", null);
        String buildTime = sysOrEnv("build.time", "BUILD_TIME", null);
        dto.build.time = parseIsoLocalDateTime(buildTime);

        dto.memory = readMemory();

        // Campos opcionais permanecem null até serem preenchidos no serviço.
        return dto;
    }

    /**
     * Versão estendida: permite preencher datasource e mensageria.
     */
    public static InfoSistemaDTO captureWithIntegrations(String appName,
                                                         String appVersion,
                                                         String environment,
                                                         String quarkusVersion,
                                                         String jdbcUrl,
                                                         Integer poolActive,
                                                         Integer poolMax,
                                                         boolean kafkaEnabled,
                                                         boolean rabbitEnabled,
                                                         Collection<String> messagingEndpoints) {
        InfoSistemaDTO dto = captureBasic();
        if (appName != null) dto.appName = appName;
        if (appVersion != null) dto.appVersion = appVersion;
        if (environment != null) dto.environment = environment;
        if (quarkusVersion != null) dto.quarkusVersion = quarkusVersion;

        if (jdbcUrl != null) {
            dto.datasource = new DataSourceInfo();
            dto.datasource.name = "default";
            dto.datasource.urlMasked = maskJdbcUrl(jdbcUrl);
            dto.datasource.poolActive = poolActive;
            dto.datasource.poolMax = poolMax;
        }

        dto.messaging = new MessagingInfo();
        dto.messaging.kafkaEnabled = kafkaEnabled;
        dto.messaging.rabbitEnabled = rabbitEnabled;
        dto.messaging.endpoints = messagingEndpoints == null ? List.of() : new ArrayList<>(messagingEndpoints);

        return dto;
    }

    // ===================== Helpers =====================

    private static String sysOrEnv(String sysKey, String envKey, String fallback) {
        String val = System.getProperty(sysKey);
        if (val == null || val.isBlank()) {
            val = System.getenv(envKey);
        }
        return (val == null || val.isBlank()) ? fallback : val;
    }

    private static LocalDateTime parseIsoLocalDateTime(String s) {
        try {
            return (s == null || s.isBlank()) ? null : LocalDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static MemoryInfo readMemory() {
        MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mx.getHeapMemoryUsage();
        MemoryUsage non = mx.getNonHeapMemoryUsage();
        Runtime rt = Runtime.getRuntime();

        MemoryInfo mi = new MemoryInfo();
        mi.heapUsed = heap != null ? heap.getUsed() : null;
        mi.heapMax = heap != null ? heap.getMax() : null;
        mi.nonHeapUsed = non != null ? non.getUsed() : null;
        mi.processTotal = rt.totalMemory();
        mi.processUsed = rt.totalMemory() - rt.freeMemory();
        return mi;
    }

    /**
     * Mascara a senha (se houver) em URLs JDBC do tipo:
     * jdbc:postgresql://host:5432/db?user=app&password=secret&ssl=true
     */
    public static String maskJdbcUrl(String url) {
        if (url == null) return null;
        // mascara password=...
        String masked = url.replaceAll("(?i)(password=)([^&]+)", "$1***");
        // opcional: mascarar tokens embutidos no path (não usual em postgres)
        return masked;
    }

    // Atalhos para adicionar health/metrics no service sem expor mutáveis demais

    public InfoSistemaDTO addHealth(String name, String status, Map<String, Object> details) {
        if (this.health == null) this.health = new ArrayList<>();
        HealthComponent hc = new HealthComponent();
        hc.name = name;
        hc.status = status;
        hc.details = details == null ? Map.of() : new LinkedHashMap<>(details);
        this.health.add(hc);
        return this;
    }

    public InfoSistemaDTO addMetric(String key, Object value) {
        if (this.metrics == null) this.metrics = new LinkedHashMap<>();
        this.metrics.put(key, value);
        return this;
    }
}
