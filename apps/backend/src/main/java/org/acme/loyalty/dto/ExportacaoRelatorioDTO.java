package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * DTO genérico para solicitar e/ou devolver uma exportação de relatórios.
 * Pode ser usado para:
 *  - receber os parâmetros de exportação (tipo, período, formato, filtros)
 *  - devolver metadados e o conteúdo base64 do arquivo gerado
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ExportacaoRelatorio", description = "Parâmetros e resultado de exportação de relatórios")
public class ExportacaoRelatorioDTO {

    // ---- Parâmetros da requisição ----

    /** Alias de compatibilidade com o service (RelatorioService.exportarRelatorio). */
    @Schema(description = "Tipo de relatório (alias usado pelo service)")
    public String tipoRelatorio;

    @NotBlank
    @Schema(
        description = "Tipo de relatório a exportar",
        example = "pontos-acumulados",
        enumeration = {
            "pontos-acumulados",
            "pontos-expirando",
            "transacoes-volume",
            "resgates-status",
            "regras-efetividade",
            "campanhas-performance",
            "usuarios-ranking",
            "dashboard-executivo"
        }
    )
    public String tipo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora inicial do período (inclusiva). Aceita também 'yyyy-MM-dd' (assume 00:00:00).", example = "2025-08-01T00:00:00")
    public LocalDateTime dataInicio;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora final do período (inclusiva). Aceita também 'yyyy-MM-dd' (assume 23:59:59).", example = "2025-08-31T23:59:59")
    public LocalDateTime dataFim;

    @NotBlank
    @Pattern(regexp = "^(?i)(csv|xlsx|json|pdf)$")
    @Schema(description = "Formato de exportação", example = "csv", enumeration = {"csv", "xlsx", "json", "pdf"})
    public String formato;

    @Schema(description = "Timezone IANA para formatação de datas", example = "America/Sao_Paulo")
    public String timezone;

    /** Filtros específicos por relatório (ex.: usuarioId, cartaoId, regraId, campanhaId, status, agrupamento, limite, criterio, dias etc). */
    @Schema(description = "Filtros adicionais específicos do relatório")
    public Map<String, Object> filtros;

    // Opções de CSV
    @Schema(description = "Incluir cabeçalho no CSV?", example = "true")
    public Boolean incluirCabecalho;

    @Schema(description = "Separador CSV (por padrão ',')", example = ";")
    public String separador;

    @Schema(description = "Padrão de data para campos com data (default: yyyy-MM-dd)", example = "dd/MM/yyyy")
    public String datePattern;

    @Schema(description = "Padrão de data/hora para campos com timestamp (default: yyyy-MM-dd'T'HH:mm:ss)", example = "dd/MM/yyyy HH:mm:ss")
    public String datetimePattern;

    // ---- Metadados/resultado da resposta ----

    @Schema(description = "Nome do arquivo gerado", example = "relatorio-pontos-acumulados-20250827-101530.csv")
    public String fileName;

    @Schema(description = "MIME type do conteúdo", example = "text/csv")
    public String contentType;

    @Schema(description = "Tamanho do conteúdo em bytes", example = "123456")
    public Long contentLength;

    /** URL gerada/armazenada para download do arquivo (compatível com o service). */
    @Schema(description = "URL para download do arquivo gerado", example = "/relatorios/pontos-acumulados_1693140930000.csv")
    public String arquivoUrl;

    /** Status do processamento da exportação (ex.: PROCESSANDO, CONCLUIDO, ERRO). */
    @Schema(description = "Status do processamento da exportação", example = "CONCLUIDO")
    public String status;

    /** Timestamp de solicitação da exportação. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Data/hora da solicitação")
    public LocalDateTime solicitadoEm;

    /** Mensagem de erro quando status = ERRO. */
    @Schema(description = "Mensagem de erro (quando aplicável)")
    public String erro;

    /** Conteúdo do arquivo em Base64 (útil quando a API retorna inline). Alternativa a stream/binário. */
    @Schema(description = "Conteúdo do arquivo, em Base64 (opcional quando a API retorna bytes/stream).")
    public String conteudoBase64;

    public ExportacaoRelatorioDTO() {
        this.formato = "csv";                       // default seguro para toLowerCase()
        this.status = "PROCESSANDO";                // estado inicial padrão
        this.filtros = new java.util.HashMap<>();   // evita NPE ao adicionar chaves
        this.solicitadoEm = java.time.LocalDateTime.now();
    }

    // ======================== Helpers de construção ========================

    /**
     * Constrói o DTO a partir de strings de query (como em RelatorioResource).
     * Aceita datas nos formatos:
     *  - yyyy-MM-dd           -> assume 00:00:00 para início e 23:59:59 para fim
     *  - yyyy-MM-dd'T'HH:mm:ss
     */
    public static ExportacaoRelatorioDTO fromQuery(String tipo, String dataInicioStr, String dataFimStr, String formato) {
        ExportacaoRelatorioDTO dto = new ExportacaoRelatorioDTO();
        dto.tipo = tipo;
        dto.tipoRelatorio = tipo; // mantém ambos consistentes
        dto.formato = formato;
        dto.dataInicio = parseInicio(dataInicioStr);
        dto.dataFim = parseFim(dataFimStr);
        dto.incluirCabecalho = Boolean.TRUE; // padrão para CSV
        dto.separador = ",";                 // padrão CSV
        dto.datePattern = "yyyy-MM-dd";
        dto.datetimePattern = "yyyy-MM-dd'T'HH:mm:ss";
        // filename/content-type podem ser resolvidos depois:
        dto.fileName = defaultFileName(dto.tipo, dto.formato);
        dto.contentType = mediaTypeFor(dto.formato);
        return dto;
    }

    // ======================== Validações simples ========================

    @AssertTrue(message = "dataFim deve ser maior ou igual a dataInicio")
    public boolean isPeriodoValido() {
        if (dataInicio == null || dataFim == null) return true;
        return !dataFim.isBefore(dataInicio);
    }

    // ======================== Utilidades estáticas ========================

    /** Tenta parsear 'yyyy-MM-dd' ou 'yyyy-MM-dd'T'HH:mm:ss' para INÍCIO (00:00:00 se vier só data). */
    public static LocalDateTime parseInicio(String text) {
        if (text == null || text.isBlank()) return null;
        LocalDateTime dt = tryParseDateTime(text);
        if (dt != null) return dt;
        LocalDate d = tryParseDate(text);
        return d != null ? d.atStartOfDay() : null;
    }

    /** Tenta parsear 'yyyy-MM-dd' ou 'yyyy-MM-dd'T'HH:mm:ss' para FIM (23:59:59 se vier só data). */
    public static LocalDateTime parseFim(String text) {
        if (text == null || text.isBlank()) return null;
        LocalDateTime dt = tryParseDateTime(text);
        if (dt != null) return dt;
        LocalDate d = tryParseDate(text);
        return d != null ? d.atTime(LocalTime.of(23, 59, 59)) : null;
    }

    private static LocalDateTime tryParseDateTime(String s) {
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDate tryParseDate(String s) {
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String defaultFileName(String tipo, String formato) {
        String safeTipo = (tipo == null || tipo.isBlank()) ? "relatorio" : tipo.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\-]+", "-");
        String ext = extensionFor(formato);
        String ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).format(LocalDateTime.now());
        return safeTipo + "-" + ts + "." + ext;
    }

    public static String mediaTypeFor(String formato) {
        if (formato == null) return "application/octet-stream";
        String lowerFormato = formato.toLowerCase(Locale.ROOT);
        if ("csv".equals(lowerFormato)) {
            return "text/csv";
        } else if ("xlsx".equals(lowerFormato)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if ("json".equals(lowerFormato)) {
            return "application/json";
        } else if ("pdf".equals(lowerFormato)) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }

    public static String extensionFor(String formato) {
        if (formato == null) return "bin";
        String lowerFormato = formato.toLowerCase(Locale.ROOT);
        if ("csv".equals(lowerFormato)) {
            return "csv";
        } else if ("xlsx".equals(lowerFormato)) {
            return "xlsx";
        } else if ("json".equals(lowerFormato)) {
            return "json";
        } else if ("pdf".equals(lowerFormato)) {
            return "pdf";
        } else {
            return "bin";
        }
    }

    // ======================== Conveniências ========================

    /** Define contentType e fileName coerentes com o formato atual (sem sobrescrever se já existirem). */
    public void ensureMetadata() {
        if (this.contentType == null) this.contentType = mediaTypeFor(this.formato);
        if (this.fileName == null) this.fileName = defaultFileName(
                (this.tipo != null ? this.tipo : this.tipoRelatorio), this.formato);
    }

    /** Normaliza separador e cabeçalho para CSV. */
    public void normalizeCsvDefaults() {
        if (!"csv".equalsIgnoreCase(this.formato)) return;
        if (this.separador == null || this.separador.isBlank()) this.separador = ",";
        if (this.incluirCabecalho == null) this.incluirCabecalho = Boolean.TRUE;
        if (this.datePattern == null) this.datePattern = "yyyy-MM-dd";
        if (this.datetimePattern == null) this.datetimePattern = "yyyy-MM-dd'T'HH:mm:ss";
    }

    /** Testa se contém filtro simples por chave (ex.: "usuarioId"). */
    public boolean hasFiltro(String key) {
        return this.filtros != null && this.filtros.containsKey(key) && this.filtros.get(key) != null;
    }

    /** Lê um filtro long (retorna null se não existir ou não for conversível). */
    public Long getFiltroLong(String key) {
        if (this.filtros == null) return null;
        Object v = this.filtros.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(Objects.toString(v));
        } catch (Exception e) {
            return null;
        }
    }
}
