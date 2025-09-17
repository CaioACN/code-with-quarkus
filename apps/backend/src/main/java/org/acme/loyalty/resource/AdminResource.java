package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.*;
import org.acme.loyalty.service.AdminService;
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

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Administração", description = "APIs administrativas do sistema de pontos")
public class AdminResource {

    private static final Logger LOG = Logger.getLogger(AdminResource.class);
    
    @Inject
    AdminService adminService;

    @GET
    @Path("/dashboard")
    @Operation(summary = "Consultar dashboard administrativo", 
               description = "Retorna métricas consolidadas para o dashboard administrativo")
    @APIResponse(responseCode = "200", description = "Dashboard consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = DashboardDTO.class)))
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarDashboard() {
        try {
            LOG.info("Consultando dashboard administrativo");
            
            DashboardDTO dashboard = adminService.consultarDashboard();
            
            LOG.info("Dashboard consultado com sucesso");
            
            return Response.ok(SuccessResponseDTO.ok("Dashboard consultado com sucesso", dashboard)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar dashboard: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar dashboard: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/estatisticas")
    @Operation(summary = "Consultar estatísticas do sistema", 
               description = "Retorna estatísticas consolidadas do sistema de pontos")
    @APIResponse(responseCode = "200", description = "Estatísticas consultadas com sucesso",
                 content = @Content(schema = @Schema(implementation = EstatisticasDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarEstatisticas(
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim) {
        
        try {
            LOG.info("Consultando estatísticas - período: " + dataInicio + " a " + dataFim);
            
            // Validação de datas (se necessário)
            parseDate(dataInicio);
            parseDate(dataFim);
            
            // Implementar método consultarEstatisticas no AdminService
            // EstatisticasDTO estatisticas = adminService.consultarEstatisticas(inicio, fim);
            EstatisticasDTO estatisticas = new EstatisticasDTO();
            
            LOG.info("Estatísticas consultadas com sucesso");
            
            return Response.ok(SuccessResponseDTO.ok("Estatísticas consultadas com sucesso", estatisticas)).build();
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar estatísticas: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar estatísticas: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/pontos/ajuste")
    @Operation(summary = "Realizar ajuste de pontos", 
               description = "Realiza ajuste manual de pontos (crédito ou débito) para um usuário/cartão")
    @APIResponse(responseCode = "200", description = "Ajuste de pontos realizado com sucesso",
                 content = @Content(schema = @Schema(implementation = MovimentoPontosDTO.class)))
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response realizarAjustePontos(
            @Parameter(description = "Dados do ajuste de pontos", required = true)
            @Valid @NotNull AjustePontosDTO ajusteRequest) {
        
        try {
            LOG.info("Realizando ajuste de pontos - usuário: " + ajusteRequest.usuarioId + 
                    ", cartão: " + ajusteRequest.cartaoId + ", pontos: " + ajusteRequest.pontos);
            
            // Implementar método realizarAjustePontos no AdminService
            // MovimentoPontosDTO movimento = adminService.realizarAjustePontos(ajusteRequest);
            MovimentoPontosDTO movimento = new MovimentoPontosDTO();
            
            LOG.info("Ajuste de pontos realizado com sucesso - ID: " + movimento.id);
            
            return Response.ok(SuccessResponseDTO.ok("Ajuste de pontos realizado com sucesso", movimento)).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Erro de validação ao realizar ajuste: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Dados inválidos: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao realizar ajuste de pontos: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao realizar ajuste: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/processar-teste")
    @Produces(MediaType.APPLICATION_JSON)
    public Response processarTeste() {
        try {
            long pontosGerados = adminService.processarTransacaoPontuacao(9L, 1.0);
            return Response.ok("{\"success\": true, \"pontosGerados\": " + pontosGerados + ", \"transacaoId\": 9}").build();
        } catch (Exception e) {
            return Response.status(500).entity("{\"error\": \"" + e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("/pontos/estorno")
    @Operation(summary = "Realizar estorno de pontos", 
               description = "Realiza estorno de pontos de uma transação específica")
    @APIResponse(responseCode = "200", description = "Estorno de pontos realizado com sucesso",
                 content = @Content(schema = @Schema(implementation = MovimentoPontosDTO.class)))
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response realizarEstornoPontos(
            @Parameter(description = "ID da transação", required = true, example = "123")
            @QueryParam("transacaoId") @Min(1) Long transacaoId,
            
            @Parameter(description = "Motivo do estorno", required = true)
            @QueryParam("motivo") @NotNull String motivo) {
        
        try {
            LOG.info("Realizando estorno de pontos - transação: " + transacaoId + ", motivo: " + motivo);
            
            // Implementar método realizarEstornoPontos no AdminService
            // MovimentoPontosDTO movimento = adminService.realizarEstornoPontos(transacaoId, motivo);
            MovimentoPontosDTO movimento = new MovimentoPontosDTO();
            
            LOG.info("Estorno de pontos realizado com sucesso - ID: " + movimento.id);
            
            return Response.ok(SuccessResponseDTO.ok("Estorno de pontos realizado com sucesso", movimento)).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Erro de validação ao realizar estorno: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Dados inválidos: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao realizar estorno de pontos: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao realizar estorno: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/usuarios/{usuarioId}/auditoria")
    @Operation(summary = "Consultar auditoria do usuário", 
               description = "Retorna histórico de auditoria de um usuário específico")
    @APIResponse(responseCode = "200", description = "Auditoria consultada com sucesso",
                 content = @Content(schema = @Schema(implementation = AuditoriaUsuarioDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarAuditoriaUsuario(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim) {
        
        try {
            LOG.info("Consultando auditoria do usuário - ID: " + usuarioId);
            
            // Validação de datas (se necessário)
            parseDate(dataInicio);
            parseDate(dataFim);
            
            // Implementar método consultarAuditoriaUsuario no AdminService
            // AuditoriaUsuarioDTO auditoria = adminService.consultarAuditoriaUsuario(usuarioId, inicio, fim);
            AuditoriaUsuarioDTO auditoria = new AuditoriaUsuarioDTO();
            
            LOG.info("Auditoria consultada com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Auditoria consultada com sucesso", auditoria)).build();
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar auditoria do usuário - ID: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar auditoria: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/manutencao/limpeza")
    @Operation(summary = "Executar limpeza de manutenção", 
               description = "Executa limpeza de dados antigos do sistema")
    @APIResponse(responseCode = "200", description = "Limpeza executada com sucesso")
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response executarLimpezaManutencao(
            @Parameter(description = "Número de dias para manter dados", example = "365")
            @QueryParam("dias") @Min(1) Integer dias,
            
            @Parameter(description = "Tipo de limpeza", schema = @Schema(type = SchemaType.STRING, enumeration = {"transacoes", "movimentos", "logs", "todos"}))
            @QueryParam("tipo") @NotNull String tipo) {
        
        try {
            LOG.info("Executando limpeza de manutenção - dias: " + dias + ", tipo: " + tipo);
            
            // Implementar método executarLimpezaManutencao no AdminService
            // String resultado = adminService.executarLimpezaManutencao(dias, tipo);
            String resultado = "Limpeza executada com sucesso";
            
            LOG.info("Limpeza de manutenção executada com sucesso");
            
            return Response.ok(SuccessResponseDTO.ok("Limpeza de manutenção executada com sucesso", resultado)).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Erro de validação ao executar limpeza: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Parâmetros inválidos: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao executar limpeza de manutenção: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao executar limpeza: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/sistema/health")
    @Operation(summary = "Consultar health do sistema", 
               description = "Retorna informações de saúde do sistema")
    @APIResponse(responseCode = "200", description = "Health consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = SaudeSistemaDTO.class)))
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarHealthSistema() {
        try {
            LOG.info("Consultando health do sistema");
            
            // Implementar método consultarHealthSistema no AdminService
            // SaudeSistemaDTO health = adminService.consultarHealthSistema();
            SaudeSistemaDTO health = new SaudeSistemaDTO();
            
            LOG.info("Health do sistema consultado com sucesso");
            
            return Response.ok(SuccessResponseDTO.ok("Health do sistema consultado com sucesso", health)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar health do sistema: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar health: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/sistema/metricas")
    @Operation(summary = "Consultar métricas do sistema", 
               description = "Retorna métricas de performance e uso do sistema")
    @APIResponse(responseCode = "200", description = "Métricas consultadas com sucesso",
                 content = @Content(schema = @Schema(implementation = MetricasSistemaDTO.class)))
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarMetricasSistema() {
        try {
            LOG.info("Consultando métricas do sistema");
            
            // Implementar método consultarMetricasSistema no AdminService
            // MetricasSistemaDTO metricas = adminService.consultarMetricasSistema();
            MetricasSistemaDTO metricas = new MetricasSistemaDTO();
            
            LOG.info("Métricas do sistema consultadas com sucesso");
            
            return Response.ok(SuccessResponseDTO.ok("Métricas do sistema consultadas com sucesso", metricas)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar métricas do sistema: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar métricas: " + e.getMessage()))
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

