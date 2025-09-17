package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.*;

/**
 * DTO para expor a SAÚDE do sistema (health/ready).
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 *
 * Pode ser usado por um endpoint /saude (ou mapeado a partir do SmallRye Health),
 * agregando o status geral e o detalhamento por componente (DB, Kafka, Cache, etc.).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SaudeSistema", description = "Status de saúde do serviço e de suas dependências")
public class SaudeSistemaDTO {

    // ================== Cabeçalho do relatório ==================

    @Schema(description = "Nome lógico do serviço", example = "loyalty-service")
    public String servico;

    @Schema(description = "Versão do serviço (semântico ou hash)", example = "1.0.0-SNAPSHOT")
    public String versao;

    @Schema(description = "Hash do commit/artefato (opcional)", example = "a1b2c3d")
    public String buildCommit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora do build (opcional)")
    public LocalDateTime buildTime;

    @Schema(description = "Ambiente de execução", example = "dev | staging | prod")
    public String ambiente;

    @Schema(description = "Região/Zona (opcional)", example = "sa-east-1")
    public String regiao;

    @Schema(description = "Identificador da instância (opcional)", example = "ip-10-0-0-12")
    public String instanciaId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Timestamp da medição")
    public LocalDateTime timestamp = LocalDateTime.now();

    @Min(0)
    @Schema(description = "Uptime do processo, em segundos")
    public Long uptimeSegundos;

    // ================== Status agregado ==================

    @Schema(
        description = "Status agregado do sistema",
        enumeration = {"UP","WARN","DOWN"}
    )
    public Status statusGeral = Status.UP;

    @Schema(description = "Mensagem/observação geral (opcional)")
    public String mensagem;

    @Schema(description = "Quantidade total de componentes avaliados")
    public Integer totalComponentes;

    @Schema(description = "Componentes UP")
    public Integer componentesUp;

    @Schema(description = "Componentes em WARN (degradado)")
    public Integer componentesWarn;

    @Schema(description = "Componentes DOWN")
    public Integer componentesDown;

    @Schema(description = "Nome do componente mais lento (se disponível)")
    public String componenteMaisLento;

    @Min(0)
    @Schema(description = "Maior latência observada entre componentes (ms)")
    public Long maiorLatenciaMs;

    // ================== Detalhes por componente ==================

    @Schema(description = "Lista de componentes e seus estados")
    public List<Componente> componentes = new ArrayList<>();

    // ================== Tipos auxiliares ==================

    /** Severidade: DOWN (2) > WARN (1) > UP (0). */
    public enum Status {
        UP, WARN, DOWN;
        public int severity() {
            if (this == UP) return 0;
            if (this == WARN) return 1;
            if (this == DOWN) return 2;
            return 0; // default
        }
        public static Status worst(Status a, Status b) {
            return (a.severity() >= b.severity()) ? a : b;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Estado de um componente/dependência")
    public static class Componente {
        @Schema(description = "Nome do componente", example = "database")
        public String nome;

        @Schema(description = "Status do componente", enumeration = {"UP","WARN","DOWN"})
        public Status status = Status.UP;

        @Min(0)
        @Schema(description = "Latência medida (ms) do check (quando aplicável)")
        public Long latenciaMs;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Última verificação")
        public LocalDateTime verificadoEm;

        @Schema(description = "Endpoint/target verificado (opcional)", example = "jdbc:postgresql://…")
        public String alvo;

        @Schema(description = "Mensagem curta (opcional)")
        public String mensagem;

        @Schema(description = "Detalhes adicionais do check")
        public Map<String, Object> detalhes = new LinkedHashMap<>();
    }

    // ================== API de montagem ==================

    /** Adiciona um componente com dados mínimos. */
    public Componente addComponente(String nome, Status status) {
        Componente c = new Componente();
        c.nome = nome;
        c.status = status == null ? Status.UP : status;
        c.verificadoEm = LocalDateTime.now();
        componentes.add(c);
        return c;
    }

    /** Adiciona um componente com latência e mensagem. */
    public Componente addComponente(String nome, Status status, Long latenciaMs, String mensagem, String alvo) {
        Componente c = addComponente(nome, status);
        c.latenciaMs = latenciaMs;
        c.mensagem = mensagem;
        c.alvo = alvo;
        return c;
    }

    /** Recalcula agregados (status geral, contagens, mais lento). Chame no final. */
    public void recompute() {
        int up = 0, warn = 0, down = 0;
        Status worst = Status.UP;
        long maxLat = -1;
        String slowest = null;

        if (componentes != null) {
            for (Componente c : componentes) {
                if (c == null) continue;
                // contagens
                if (c.status == Status.UP) up++;
                else if (c.status == Status.WARN) warn++;
                else if (c.status == Status.DOWN) down++;

                // pior status
                worst = Status.worst(worst, c.status);

                // latência
                if (c.latenciaMs != null && c.latenciaMs >= 0 && c.latenciaMs > maxLat) {
                    maxLat = c.latenciaMs;
                    slowest = c.nome;
                }
            }
        }

        totalComponentes = (componentes == null) ? 0 : componentes.size();
        componentesUp = up;
        componentesWarn = warn;
        componentesDown = down;

        statusGeral = worst;
        maiorLatenciaMs = (maxLat < 0) ? null : maxLat;
        componenteMaisLento = slowest;

        // mensagem padrão quando não fornecida
        if (mensagem == null || mensagem.isBlank()) {
            if (statusGeral == Status.UP) mensagem = "Todos os componentes operacionais";
            else if (statusGeral == Status.WARN) mensagem = "Serviço degradado";
            else mensagem = "Falha em dependências críticas";
        }
    }

    // ================== Builders utilitários ==================

    public SaudeSistemaDTO withServico(String s) { this.servico = s; return this; }
    public SaudeSistemaDTO withVersao(String v) { this.versao = v; return this; }
    public SaudeSistemaDTO withBuildCommit(String c) { this.buildCommit = c; return this; }
    public SaudeSistemaDTO withBuildTime(LocalDateTime t) { this.buildTime = t; return this; }
    public SaudeSistemaDTO withAmbiente(String a) { this.ambiente = a; return this; }
    public SaudeSistemaDTO withRegiao(String r) { this.regiao = r; return this; }
    public SaudeSistemaDTO withInstanciaId(String i) { this.instanciaId = i; return this; }
    public SaudeSistemaDTO withUptime(Long s) { this.uptimeSegundos = s; return this; }
    public SaudeSistemaDTO withMensagem(String m) { this.mensagem = m; return this; }
}
