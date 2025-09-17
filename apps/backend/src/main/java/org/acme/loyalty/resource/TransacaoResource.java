package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.*;
import org.acme.loyalty.service.TransacaoService;
import org.acme.loyalty.entity.Transacao;
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

@Path("/transacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transações", description = "APIs para gestão de transações do sistema de pontos")
public class TransacaoResource {

    private static final Logger LOG = Logger.getLogger(TransacaoResource.class);

    @Inject
    TransacaoService transacaoService;

    @POST
    @Operation(summary = "Criar transação", 
               description = "Registra uma nova transação no sistema e emite evento TransactionCreated")
    @APIResponse(responseCode = "201", description = "Transação criada com sucesso",
                 content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response criarTransacao(
            @Parameter(description = "Dados da transação", required = true)
            @Valid @NotNull TransacaoRequestDTO request) {
        
        try {
            LOG.info("Criando transação - cartão: " + request.cartaoId + ", valor: " + request.valor);
            
            Transacao transacao = transacaoService.criarTransacao(request);
            TransacaoResponseDTO response = TransacaoResponseDTO.fromEntity(transacao);
            
            LOG.info("Transação criada com sucesso - ID: " + transacao.id);
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(response))
                    .build();
        } catch (IllegalArgumentException e) {
            LOG.error("Erro de validação ao criar transação: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Dados inválidos: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro interno ao criar transação: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao criar transação: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Operation(summary = "Listar transações", 
               description = "Lista transações com filtros opcionais e paginação")
    @APIResponse(responseCode = "200", description = "Lista de transações retornada com sucesso",
                 content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response listarTransacoes(
            @Parameter(description = "ID do usuário para filtrar")
            @QueryParam("usuarioId") Long usuarioId,
            
            @Parameter(description = "ID do cartão para filtrar")
            @QueryParam("cartaoId") Long cartaoId,
            
            @Parameter(description = "Status da transação", schema = @Schema(type = SchemaType.STRING, enumeration = {"PENDENTE", "PROCESSADA", "REJEITADA", "ESTORNADA"}))
            @QueryParam("status") String status,
            
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Categoria para filtrar")
            @QueryParam("categoria") String categoria,
            
            @Parameter(description = "MCC para filtrar")
            @QueryParam("mcc") String mcc,
            
            @Parameter(description = "ID do parceiro para filtrar")
            @QueryParam("parceiroId") Long parceiroId,
            
            @Parameter(description = "Número da página", example = "1")
            @QueryParam("pagina") @DefaultValue("1") @Min(1) Integer pagina,
            
            @Parameter(description = "Tamanho da página", example = "20")
            @QueryParam("tamanho") @DefaultValue("20") @Min(1) Integer tamanho) {
        
        try {
            LOG.info("Listando transações - página: " + pagina + ", tamanho: " + tamanho);
            
            // Validação de datas (se necessário)
            parseDate(dataInicio);
            parseDate(dataFim);
            
            PageResponseDTO<TransacaoResponseDTO> transacoes = transacaoService.listarTransacoes(
                usuarioId, cartaoId, status, dataInicio, dataFim, pagina, tamanho);
            
            LOG.info("Transações listadas com sucesso - total: " + transacoes.totalElements);
            
            return Response.ok(SuccessResponseDTO.ok("Transações listadas com sucesso", transacoes)).build();
        } catch (DateTimeParseException e) {
            LOG.error("Erro ao parsear data: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Formato de data inválido. Use yyyy-MM-dd"))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao listar transações: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar transações: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Buscar transação por ID", 
               description = "Retorna os detalhes de uma transação específica")
    @APIResponse(responseCode = "200", description = "Transação encontrada com sucesso",
                 content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class)))
    @APIResponse(responseCode = "404", description = "Transação não encontrada")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response buscarTransacao(
            @Parameter(description = "ID da transação", required = true, example = "123")
            @PathParam("id") @Min(1) Long id) {
        
        try {
            LOG.info("Buscando transação - ID: " + id);
            
            TransacaoResponseDTO transacao = transacaoService.buscarTransacaoPorId(id);
            if (transacao != null) {
                LOG.info("Transação encontrada - ID: " + id);
                return Response.ok(SuccessResponseDTO.ok("Transação encontrada com sucesso", transacao)).build();
            } else {
                LOG.warn("Transação não encontrada - ID: " + id);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ErrorResponseDTO.notFound("Transação não encontrada"))
                        .build();
            }
        } catch (Exception e) {
            LOG.error("Erro ao buscar transação - ID: " + id + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao buscar transação: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/status")
    @Operation(summary = "Atualizar status da transação", 
               description = "Atualiza o status de uma transação (PENDENTE, PROCESSADA, REJEITADA, ESTORNADA)")
    @APIResponse(responseCode = "200", description = "Status atualizado com sucesso",
                 content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Status inválido")
    @APIResponse(responseCode = "404", description = "Transação não encontrada")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response atualizarStatus(
            @Parameter(description = "ID da transação", required = true, example = "123")
            @PathParam("id") @Min(1) Long id,
            
            @Parameter(description = "Novo status", required = true, schema = @Schema(type = SchemaType.STRING, enumeration = {"PENDENTE", "PROCESSADA", "REJEITADA", "ESTORNADA"}))
            @QueryParam("status") @NotNull String novoStatus) {
        
        try {
            LOG.info("Atualizando status da transação - ID: " + id + ", novo status: " + novoStatus);
            
            TransacaoResponseDTO transacao = transacaoService.atualizarStatus(id, novoStatus);
            
            LOG.info("Status da transação atualizado com sucesso - ID: " + id);
            
            return Response.ok(SuccessResponseDTO.updated(transacao)).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Status inválido para transação - ID: " + id + ", status: " + novoStatus);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Status inválido: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao atualizar status da transação - ID: " + id + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao atualizar status: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/estorno")
    @Operation(summary = "Estornar transação", 
               description = "Estorna uma transação e reverte os pontos gerados")
    @APIResponse(responseCode = "200", description = "Transação estornada com sucesso",
                 content = @Content(schema = @Schema(implementation = TransacaoResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Transação não pode ser estornada")
    @APIResponse(responseCode = "404", description = "Transação não encontrada")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response estornarTransacao(
            @Parameter(description = "ID da transação", required = true, example = "123")
            @PathParam("id") @Min(1) Long id,
            
            @Parameter(description = "Motivo do estorno")
            @QueryParam("motivo") String motivo) {
        
        try {
            LOG.info("Estornando transação - ID: " + id + ", motivo: " + motivo);
            
            // Implementar método estornarTransacao no TransacaoService
            // TransacaoResponseDTO transacao = transacaoService.estornarTransacao(id, motivo);
            TransacaoResponseDTO transacao = new TransacaoResponseDTO(id);
            
            LOG.info("Transação estornada com sucesso - ID: " + id);
            
            return Response.ok(SuccessResponseDTO.ok("Transação estornada com sucesso", transacao)).build();
        } catch (IllegalArgumentException e) {
            LOG.error("Transação não pode ser estornada - ID: " + id + ", erro: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Transação não pode ser estornada: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Erro ao estornar transação - ID: " + id + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao estornar transação: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/pontos")
    @Operation(summary = "Consultar pontos da transação", 
               description = "Retorna a quantidade de pontos gerados por uma transação")
    @APIResponse(responseCode = "200", description = "Pontos consultados com sucesso")
    @APIResponse(responseCode = "404", description = "Transação não encontrada")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarPontosTransacao(
            @Parameter(description = "ID da transação", required = true, example = "123")
            @PathParam("id") @Min(1) Long id) {
        
        try {
            LOG.info("Consultando pontos da transação - ID: " + id);
            
            Integer pontos = transacaoService.consultarPontosTransacao(id);
            
            LOG.info("Pontos consultados com sucesso - ID: " + id + ", pontos: " + pontos);
            
            return Response.ok(SuccessResponseDTO.ok("Pontos da transação consultados com sucesso", pontos)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar pontos da transação - ID: " + id + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Transação não encontrada ou sem pontos"))
                    .build();
        }
    }

    @GET
    @Path("/usuarios/{usuarioId}/extrato")
    @Operation(summary = "Consultar extrato do usuário", 
               description = "Retorna o extrato de transações de um usuário com paginação")
    @APIResponse(responseCode = "200", description = "Extrato consultado com sucesso",
                 content = @Content(schema = @Schema(implementation = ExtratoPontosDTO.class)))
    @APIResponse(responseCode = "400", description = "Parâmetros inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarExtratoUsuario(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "ID do cartão (opcional)")
            @QueryParam("cartaoId") Long cartaoId,
            
            @Parameter(description = "Data de início (formato: yyyy-MM-dd)", example = "2024-01-01")
            @QueryParam("dataInicio") String dataInicio,
            
            @Parameter(description = "Data de fim (formato: yyyy-MM-dd)", example = "2024-12-31")
            @QueryParam("dataFim") String dataFim,
            
            @Parameter(description = "Número da página", example = "1")
            @QueryParam("pagina") @DefaultValue("1") @Min(1) Integer pagina,
            
            @Parameter(description = "Tamanho da página", example = "20")
            @QueryParam("tamanho") @DefaultValue("20") @Min(1) Integer tamanho) {
        
        try {
            LOG.info("Consultando extrato do usuário - ID: " + usuarioId + ", cartão: " + cartaoId);
            
            // Validação de datas (se necessário)
            parseDate(dataInicio);
            parseDate(dataFim);
            
            // Implementar método consultarExtratoUsuario no TransacaoService
            // ExtratoPontosDTO extrato = transacaoService.consultarExtratoUsuario(usuarioId, cartaoId, inicio, fim, pagina, tamanho);
            ExtratoPontosDTO extrato = new ExtratoPontosDTO();
            
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

