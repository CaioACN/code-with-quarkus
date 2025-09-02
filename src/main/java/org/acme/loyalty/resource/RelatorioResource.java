package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.*;
import org.acme.loyalty.service.RelatorioService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/relatorios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Relatórios", description = "APIs para geração de relatórios analíticos do sistema de pontos")
public class RelatorioResource {

    private static final Logger LOG = Logger.getLogger(RelatorioResource.class);
    
    @Inject
    RelatorioService relatorioService;

    @GET
    @Path("/pontos/acumulados")
    @Operation(summary = "Relatório de pontos acumulados", 
               description = "Gera relatório detalhado de pontos acumulados por período, usuário e cartão")
    @APIResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                 content = @Content(schema = @Schema(implementation = RelatorioPontosDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response relatorioPontosAcumulados(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "ID do usuário para filtrar")
            @QueryParam("usuarioId") Long usuarioId,
            
            @Parameter(description = "ID do cartão para filtrar")
            @QueryParam("cartaoId") Long cartaoId,
            
            @Parameter(description = "Formato de saída", schema = @Schema(type = SchemaType.STRING, enumeration = {"json", "csv", "excel"}))
            @QueryParam("formato") @DefaultValue("json") String formato) {
        
        try {
            LOG.info("Gerando relatório de pontos acumulados - período: " + dataInicio + " a " + dataFim);
            
            LocalDate inicio = parseDate(dataInicio);
            LocalDate fim = parseDate(dataFim);
            
            RelatorioPontosDTO relatorio = relatorioService.gerarRelatorioPontosAcumulados(
                inicio, fim, cartaoId, usuarioId);
            
            if ("json".equalsIgnoreCase(formato)) {
                return Response.ok(SuccessResponseDTO.ok("Relatório de pontos acumulados gerado com sucesso", relatorio)).build();
            } else {
                // Para outros formatos, retornar URL de download
                ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(
                    "PONTOS_ACUMULADOS", 
                    Map.of("dataInicio", inicio, "dataFim", fim, "usuarioId", usuarioId, "cartaoId", cartaoId),
                    formato
                );
                return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
            }
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao gerar relatório de pontos acumulados: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/pontos/expirando")
    @Operation(summary = "Relatório de pontos expirando", 
               description = "Gera relatório de pontos que estão próximos do vencimento")
    @APIResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                 content = @Content(schema = @Schema(implementation = RelatorioPontosDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response relatorioPontosExpirando(
            @Parameter(description = "Horizonte de dias para expiração", example = "30")
            @QueryParam("dias") @DefaultValue("30") @Min(1) Integer dias,
            
            @Parameter(description = "ID do usuário para filtrar")
            @QueryParam("usuarioId") Long usuarioId,
            
            @Parameter(description = "ID do cartão para filtrar")
            @QueryParam("cartaoId") Long cartaoId,
            
            @Parameter(description = "Formato de saída", schema = @Schema(type = SchemaType.STRING, enumeration = {"json", "csv", "excel"}))
            @QueryParam("formato") @DefaultValue("json") String formato) {
        
        try {
            LOG.info("Gerando relatório de pontos expirando - horizonte: " + dias + " dias");
            
            LocalDate inicio = LocalDate.now();
            LocalDate fim = inicio.plusDays(dias);
            
            RelatorioPontosDTO relatorio = relatorioService.gerarRelatorioPontosExpirados(
                inicio, fim, cartaoId, usuarioId);
            
            if ("json".equalsIgnoreCase(formato)) {
                return Response.ok(SuccessResponseDTO.ok("Relatório de pontos expirando gerado com sucesso", relatorio)).build();
            } else {
                ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(
                    "PONTOS_EXPIRADOS", 
                    Map.of("dias", dias, "usuarioId", usuarioId, "cartaoId", cartaoId),
                    formato
                );
                return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
            }
        } catch (Exception e) {
            LOG.error("Erro ao gerar relatório de pontos expirando: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/transacoes/volume")
    @Operation(summary = "Relatório de volume de transações", 
               description = "Gera relatório de volume, valor e métricas de transações com séries temporais e rankings")
    @APIResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                 content = @Content(schema = @Schema(implementation = RelatorioTransacoesDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response relatorioVolumeTransacoes(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Agrupamento temporal", schema = @Schema(type = SchemaType.STRING, enumeration = {"diario", "semanal", "mensal"}))
            @QueryParam("agrupamento") @DefaultValue("diario") String agrupamento,
            
            @Parameter(description = "Categoria para filtrar")
            @QueryParam("categoria") String categoria,
            
            @Parameter(description = "MCC para filtrar")
            @QueryParam("mcc") String mcc,
            
            @Parameter(description = "ID do parceiro para filtrar")
            @QueryParam("parceiroId") Long parceiroId,
            
            @Parameter(description = "Formato de saída", schema = @Schema(type = SchemaType.STRING, enumeration = {"json", "csv", "excel"}))
            @QueryParam("formato") @DefaultValue("json") String formato) {
        
        try {
            LOG.info("Gerando relatório de volume de transações - período: " + dataInicio + " a " + dataFim);
            
            LocalDate inicio = parseDate(dataInicio);
            LocalDate fim = parseDate(dataFim);
            
            RelatorioTransacoesDTO relatorio = relatorioService.gerarRelatorioVolumeTransacoes(
                inicio, fim, categoria, mcc, parceiroId);
            
            if ("json".equalsIgnoreCase(formato)) {
                return Response.ok(SuccessResponseDTO.ok("Relatório de volume de transações gerado com sucesso", relatorio)).build();
            } else {
                ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(
                    "VOLUME_TRANSACOES", 
                    Map.of("dataInicio", inicio, "dataFim", fim, "categoria", categoria, "mcc", mcc, "parceiroId", parceiroId),
                    formato
                );
                return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
            }
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao gerar relatório de volume de transações: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/resgates/status")
    @Operation(summary = "Relatório de status de resgates", 
               description = "Gera relatório de resgates por status, período e recompensa")
    @APIResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                 content = @Content(schema = @Schema(implementation = RelatorioResgatesDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response relatorioStatusResgates(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Status do resgate", schema = @Schema(type = SchemaType.STRING, enumeration = {"PENDENTE", "APROVADO", "CONCLUIDO", "NEGADO", "CANCELADO"}))
            @QueryParam("status") String status,
            
            @Parameter(description = "ID da recompensa para filtrar")
            @QueryParam("recompensaId") Long recompensaId,
            
            @Parameter(description = "Formato de saída", schema = @Schema(type = SchemaType.STRING, enumeration = {"json", "csv", "excel"}))
            @QueryParam("formato") @DefaultValue("json") String formato) {
        
        try {
            LOG.info("Gerando relatório de status de resgates - período: " + dataInicio + " a " + dataFim);
            
            LocalDate inicio = parseDate(dataInicio);
            LocalDate fim = parseDate(dataFim);
            
            RelatorioResgatesDTO relatorio = relatorioService.gerarRelatorioStatusResgates(
                inicio, fim, status, recompensaId);
            
            if ("json".equalsIgnoreCase(formato)) {
                return Response.ok(SuccessResponseDTO.ok("Relatório de status de resgates gerado com sucesso", relatorio)).build();
            } else {
                ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(
                    "STATUS_RESGATES", 
                    Map.of("dataInicio", inicio, "dataFim", fim, "status", status, "recompensaId", recompensaId),
                    formato
                );
                return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
            }
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao gerar relatório de status de resgates: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/regras/efetividade")
    @Operation(summary = "Relatório de efetividade das regras", 
               description = "Gera relatório de performance e efetividade das regras de conversão de pontos")
    @APIResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                 content = @Content(schema = @Schema(implementation = RelatorioEfetividadeDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response relatorioEfetividadeRegras(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "ID da regra específica para analisar")
            @QueryParam("regraId") Long regraId,
            
            @Parameter(description = "ID da campanha para filtrar")
            @QueryParam("campanhaId") Long campanhaId,
            
            @Parameter(description = "Formato de saída", schema = @Schema(type = SchemaType.STRING, enumeration = {"json", "csv", "excel"}))
            @QueryParam("formato") @DefaultValue("json") String formato) {
        
        try {
            LOG.info("Gerando relatório de efetividade das regras - período: " + dataInicio + " a " + dataFim);
            
            LocalDate inicio = parseDate(dataInicio);
            LocalDate fim = parseDate(dataFim);
            
            RelatorioEfetividadeDTO relatorio = relatorioService.gerarRelatorioEfetividadeRegras(
                inicio, fim, regraId, campanhaId);
            
            if ("json".equalsIgnoreCase(formato)) {
                return Response.ok(SuccessResponseDTO.ok("Relatório de efetividade das regras gerado com sucesso", relatorio)).build();
            } else {
                ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(
                    "EFETIVIDADE_REGRAS", 
                    Map.of("dataInicio", inicio, "dataFim", fim, "regraId", regraId, "campanhaId", campanhaId),
                    formato
                );
                return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
            }
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao gerar relatório de efetividade das regras: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/campanhas/performance")
    @Operation(summary = "Relatório de performance das campanhas", 
               description = "Gera relatório de performance e efetividade das campanhas de bônus")
    @APIResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                 content = @Content(schema = @Schema(implementation = RelatorioEfetividadeDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response relatorioPerformanceCampanhas(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "ID da campanha específica para analisar")
            @QueryParam("campanhaId") Long campanhaId,
            
            @Parameter(description = "Segmento da campanha para filtrar")
            @QueryParam("segmento") String segmento,
            
            @Parameter(description = "Formato de saída", schema = @Schema(type = SchemaType.STRING, enumeration = {"json", "csv", "excel"}))
            @QueryParam("formato") @DefaultValue("json") String formato) {
        
        try {
            LOG.info("Gerando relatório de performance das campanhas - período: " + dataInicio + " a " + dataFim);
            
            LocalDate inicio = parseDate(dataInicio);
            LocalDate fim = parseDate(dataFim);
            
            RelatorioEfetividadeDTO relatorio = relatorioService.gerarRelatorioEfetividadeCampanhas(
                inicio, fim, campanhaId, segmento);
            
            if ("json".equalsIgnoreCase(formato)) {
                return Response.ok(SuccessResponseDTO.ok("Relatório de performance das campanhas gerado com sucesso", relatorio)).build();
            } else {
                ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(
                    "EFETIVIDADE_CAMPANHAS", 
                    Map.of("dataInicio", inicio, "dataFim", fim, "campanhaId", campanhaId, "segmento", segmento),
                    formato
                );
                return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
            }
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao gerar relatório de performance das campanhas: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/usuarios/ranking")
    @Operation(summary = "Relatório de ranking de usuários", 
               description = "Gera ranking de usuários por diferentes critérios (pontos, transações, valor)")
    @APIResponse(responseCode = "200", description = "Relatório gerado com sucesso",
                 content = @Content(schema = @Schema(implementation = RelatorioRankingDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response relatorioRankingUsuarios(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Limite de usuários no ranking", example = "100")
            @QueryParam("limite") @DefaultValue("100") @Min(1) Integer limite,
            
            @Parameter(description = "Critério de ordenação", schema = @Schema(type = SchemaType.STRING, enumeration = {"pontos", "transacoes", "valor", "resgates"}))
            @QueryParam("criterio") @DefaultValue("pontos") String criterio,
            
            @Parameter(description = "Formato de saída", schema = @Schema(type = SchemaType.STRING, enumeration = {"json", "csv", "excel"}))
            @QueryParam("formato") @DefaultValue("json") String formato) {
        
        try {
            LOG.info("Gerando relatório de ranking de usuários - período: " + dataInicio + " a " + dataFim + ", critério: " + criterio);
            
            LocalDate inicio = parseDate(dataInicio);
            LocalDate fim = parseDate(dataFim);
            
            RelatorioRankingDTO relatorio = relatorioService.gerarRelatorioRankingUsuarios(
                inicio, fim, criterio, limite);
            
            if ("json".equalsIgnoreCase(formato)) {
                return Response.ok(SuccessResponseDTO.ok("Relatório de ranking de usuários gerado com sucesso", relatorio)).build();
            } else {
                ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(
                    "RANKING_USUARIOS", 
                    Map.of("dataInicio", inicio, "dataFim", fim, "criterio", criterio, "limite", limite),
                    formato
                );
                return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
            }
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao gerar relatório de ranking de usuários: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao gerar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/exportar/{tipo}")
    @Operation(summary = "Exportar relatório", 
               description = "Exporta relatório em diferentes formatos (CSV, Excel, PDF)")
    @APIResponse(responseCode = "200", description = "Relatório exportado com sucesso",
                 content = @Content(schema = @Schema(implementation = ExportacaoRelatorioDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response exportarRelatorio(
            @Parameter(description = "Tipo do relatório", schema = @Schema(type = SchemaType.STRING, enumeration = {
                "PONTOS_ACUMULADOS", "PONTOS_EXPIRADOS", "VOLUME_TRANSACOES", 
                "STATUS_RESGATES", "EFETIVIDADE_REGRAS", "EFETIVIDADE_CAMPANHAS", "RANKING_USUARIOS"
            }))
            @PathParam("tipo") String tipo,
            
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Formato de exportação", schema = @Schema(type = SchemaType.STRING, enumeration = {"csv", "excel", "pdf"}))
            @QueryParam("formato") @DefaultValue("csv") String formato,
            
            @QueryParam("usuarioId") Long usuarioId,
            @QueryParam("cartaoId") Long cartaoId,
            @QueryParam("categoria") String categoria,
            @QueryParam("mcc") String mcc,
            @QueryParam("parceiroId") Long parceiroId,
            @QueryParam("status") String status,
            @QueryParam("recompensaId") Long recompensaId,
            @QueryParam("regraId") Long regraId,
            @QueryParam("campanhaId") Long campanhaId,
            @QueryParam("segmento") String segmento,
            @QueryParam("criterio") String criterio,
            @QueryParam("limite") Integer limite) {
        
        try {
            LOG.info("Exportando relatório - tipo: " + tipo + ", formato: " + formato);
            
            LocalDate inicio = parseDate(dataInicio);
            LocalDate fim = parseDate(dataFim);
            
            Map<String, Object> filtros = new HashMap<>();
            filtros.put("dataInicio", inicio);
            filtros.put("dataFim", fim);
            filtros.put("usuarioId", usuarioId);
            filtros.put("cartaoId", cartaoId);
            filtros.put("categoria", categoria);
            filtros.put("mcc", mcc);
            filtros.put("parceiroId", parceiroId);
            filtros.put("status", status);
            filtros.put("recompensaId", recompensaId);
            filtros.put("regraId", regraId);
            filtros.put("campanhaId", campanhaId);
            filtros.put("segmento", segmento);
            filtros.put("criterio", criterio);
            filtros.put("limite", limite);
            
            ExportacaoRelatorioDTO exportacao = relatorioService.exportarRelatorio(tipo, filtros, formato);
            
            return Response.ok(SuccessResponseDTO.ok("Relatório exportado com sucesso", exportacao)).build();
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao exportar relatório: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao exportar relatório: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/dashboard/executivo")
    @Operation(summary = "Dashboard executivo", 
               description = "Dashboard com métricas consolidadas para visão executiva")
    @APIResponse(responseCode = "200", description = "Dashboard consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = DashboardDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response dashboardExecutivo(
            @Parameter(description = "Período do dashboard", schema = @Schema(type = SchemaType.STRING, enumeration = {"dia", "semana", "mes", "trimestre", "ano"}))
            @QueryParam("periodo") @DefaultValue("mes") String periodo) {
        
        try {
            LOG.info("Consultando dashboard executivo - período: " + periodo);
            
            // Por enquanto, retorna um dashboard básico
            // TODO: Implementar lógica completa do dashboard
            DashboardDTO dashboard = new DashboardDTO();
            dashboard.periodoIni = java.time.LocalDateTime.now().minusDays(30);
            dashboard.periodoFim = java.time.LocalDateTime.now();
            
            return Response.ok(SuccessResponseDTO.ok("Dashboard executivo consultado com sucesso", dashboard)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar dashboard executivo: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar dashboard: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/tipos")
    @Operation(summary = "Listar tipos de relatório", 
               description = "Retorna lista de tipos de relatório disponíveis")
    @APIResponse(responseCode = "200", description = "Lista de tipos retornada com sucesso")
    public Response listarTiposRelatorio() {
        try {
            List<String> tipos = relatorioService.listarTiposRelatorio();
            return Response.ok(SuccessResponseDTO.ok("Tipos de relatório listados com sucesso", tipos)).build();
        } catch (Exception e) {
            LOG.error("Erro ao listar tipos de relatório: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar tipos: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/formatos")
    @Operation(summary = "Listar formatos de exportação", 
               description = "Retorna lista de formatos de exportação disponíveis")
    @APIResponse(responseCode = "200", description = "Lista de formatos retornada com sucesso")
    public Response listarFormatosExportacao() {
        try {
            List<String> formatos = relatorioService.listarFormatosExportacao();
            return Response.ok(SuccessResponseDTO.ok("Formatos de exportação listados com sucesso", formatos)).build();
        } catch (Exception e) {
            LOG.error("Erro ao listar formatos de exportação: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar formatos: " + e.getMessage()))
                    .build();
        }
    }

    // =====================================================================================
    // Métodos auxiliares
    // =====================================================================================

    /**
     * Converte string de data para LocalDate
     * @param dataString String no formato yyyy-MM-dd
     * @return LocalDate ou null se string for null/empty
     * @throws DateTimeParseException se formato for inválido
     */
    private LocalDate parseDate(String dataString) throws DateTimeParseException {
        if (dataString == null || dataString.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dataString.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
    }
}

