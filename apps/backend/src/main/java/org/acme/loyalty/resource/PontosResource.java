package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.*;
import org.acme.loyalty.service.PontosService;
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
import java.util.List;

@Path("/usuarios/{usuarioId}/pontos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Pontos", description = "APIs para consulta de pontos e saldos dos usuários")
public class PontosResource {

    private static final Logger LOG = Logger.getLogger(PontosResource.class);
    
    @Inject
    PontosService pontosService;

    @GET
    @Path("/saldo")
    @Operation(summary = "Consultar saldo do usuário", 
               description = "Retorna o saldo total de pontos de um usuário")
    @APIResponse(responseCode = "200", description = "Saldo consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = SaldoUsuarioDTO.class)))
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarSaldo(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId) {
        
        try {
            LOG.info("Consultando saldo do usuário - ID: " + usuarioId);
            
            SaldoUsuarioDTO saldo = pontosService.consultarSaldo(usuarioId);
            
            LOG.info("Saldo consultado com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Saldo consultado com sucesso", saldo)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar saldo do usuário - ID: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar saldo: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/extrato")
    @Operation(summary = "Consultar extrato de pontos", 
               description = "Retorna o extrato detalhado de movimentos de pontos de um usuário")
    @APIResponse(responseCode = "200", description = "Extrato consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = ExtratoPontosDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarExtrato(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "ID do cartão (opcional)")
            @QueryParam("cartaoId") Long cartaoId,
            
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Tipo de movimento", schema = @Schema(type = SchemaType.STRING, enumeration = {"ACUMULO", "EXPIRACAO", "RESGATE", "ESTORNO", "AJUSTE"}))
            @QueryParam("tipoMovimento") String tipoMovimento,
            
            @Parameter(description = "Categoria para filtrar")
            @QueryParam("categoria") String categoria,
            
            @Parameter(description = "ID do parceiro para filtrar")
            @QueryParam("parceiroId") Long parceiroId,
            
            @Parameter(description = "Número da página", example = "1")
            @QueryParam("pagina") @DefaultValue("1") @Min(1) Integer pagina,
            
            @Parameter(description = "Tamanho da página", example = "20")
            @QueryParam("tamanho") @DefaultValue("20") @Min(1) Integer tamanho) {
        
        try {
            LOG.info("Consultando extrato do usuário - ID: " + usuarioId + ", cartão: " + cartaoId);
            
            // Validação de datas (se necessário)
            parseDate(dataInicio);
            parseDate(dataFim);
            
            ExtratoPontosDTO extrato = pontosService.consultarExtrato(usuarioId, cartaoId, dataInicio, dataFim, tipoMovimento, categoria, parceiroId, pagina, tamanho);
            
            LOG.info("Extrato consultado com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Extrato consultado com sucesso", extrato)).build();
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar extrato do usuário - ID: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar extrato: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/saldo/{cartaoId}")
    @Operation(summary = "Consultar saldo do cartão", 
               description = "Retorna o saldo de pontos de um cartão específico")
    @APIResponse(responseCode = "200", description = "Saldo do cartão consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = SaldoPontosDTO.class)))
    @APIResponse(responseCode = "404", description = "Cartão não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarSaldoCartao(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "ID do cartão", required = true, example = "456")
            @PathParam("cartaoId") @Min(1) Long cartaoId) {
        
        try {
            LOG.info("Consultando saldo do cartão - usuário: " + usuarioId + ", cartão: " + cartaoId);
            
            SaldoPontosDTO saldo = pontosService.consultarSaldoCartao(usuarioId, cartaoId);
            
            LOG.info("Saldo do cartão consultado com sucesso - cartão: " + cartaoId);
            
            return Response.ok(SuccessResponseDTO.ok("Saldo do cartão consultado com sucesso", saldo)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar saldo do cartão - usuário: " + usuarioId + ", cartão: " + cartaoId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar saldo do cartão: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/movimentos")
    @Operation(summary = "Consultar movimentos de pontos", 
               description = "Retorna lista de movimentos de pontos de um usuário")
    @APIResponse(responseCode = "200", description = "Movimentos consultados com sucesso",
                 content = @Content(schema = @Schema(implementation = MovimentoPontosDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarMovimentos(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "ID do cartão (opcional)")
            @QueryParam("cartaoId") Long cartaoId,
            
            @Parameter(description = "Tipo de movimento", schema = @Schema(type = SchemaType.STRING, enumeration = {"ACUMULO", "EXPIRACAO", "RESGATE", "ESTORNO", "AJUSTE"}))
            @QueryParam("tipo") String tipo,
            
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Número da página", example = "1")
            @QueryParam("pagina") @DefaultValue("1") @Min(1) Integer pagina,
            
            @Parameter(description = "Tamanho da página", example = "20")
            @QueryParam("tamanho") @DefaultValue("20") @Min(1) Integer tamanho) {
        
        try {
            LOG.info("Consultando movimentos do usuário - ID: " + usuarioId + ", cartão: " + cartaoId);
            
            // Validação de datas (se necessário)
            parseDate(dataInicio);
            parseDate(dataFim);
            
            List<MovimentoPontosDTO> movimentos = pontosService.consultarMovimentos(usuarioId, cartaoId, tipo, dataInicio, dataFim, pagina, tamanho);
            
            LOG.info("Movimentos consultados com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Movimentos consultados com sucesso", movimentos)).build();
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar movimentos do usuário - ID: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar movimentos: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/resumo")
    @Operation(summary = "Consultar resumo de pontos", 
               description = "Retorna resumo consolidado de pontos de um usuário")
    @APIResponse(responseCode = "200", description = "Resumo consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = SaldoUsuarioDTO.class)))
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarResumo(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId) {
        
        try {
            LOG.info("Consultando resumo do usuário - ID: " + usuarioId);
            
            SaldoUsuarioDTO resumo = pontosService.consultarSaldo(usuarioId);
            
            LOG.info("Resumo consultado com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Resumo consultado com sucesso", resumo)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar resumo do usuário - ID: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar resumo: " + e.getMessage()))
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

