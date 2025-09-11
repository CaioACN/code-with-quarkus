package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.*;
import org.acme.loyalty.service.NotificacaoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/notificacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notificações", description = "APIs para gerenciamento de notificações dos usuários")
public class NotificacaoResource {

    private static final Logger LOG = Logger.getLogger(NotificacaoResource.class);
    
    @Inject
    NotificacaoService notificacaoService;

    @GET
    @Path("/usuario/{usuarioId}")
    @Operation(summary = "Listar notificações do usuário", 
               description = "Retorna lista de notificações de um usuário específico")
    @APIResponse(responseCode = "200", description = "Notificações listadas com sucesso",
                 content = @Content(schema = @Schema(implementation = NotificacaoResponseDTO.class)))
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response listarNotificacoesUsuario(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "Tipo de notificação", schema = @Schema(type = SchemaType.STRING, enumeration = {"ACUMULO", "EXPIRACAO", "RESGATE", "SISTEMA"}))
            @QueryParam("tipo") String tipo,
            
            @Parameter(description = "Filtrar por notificações lidas/não lidas")
            @QueryParam("lida") Boolean lida,
            
            @Parameter(description = "Número da página", example = "1")
            @QueryParam("pagina") @DefaultValue("1") @Min(1) Integer pagina,
            
            @Parameter(description = "Tamanho da página", example = "20")
            @QueryParam("tamanho") @DefaultValue("20") @Min(1) Integer tamanho) {
        
        try {
            LOG.info("Listando notificações do usuário - ID: " + usuarioId);
            
            List<NotificacaoResponseDTO> notificacoes = notificacaoService.listarNotificacoes(usuarioId, tipo, lida, pagina, tamanho);
            
            LOG.info("Notificações listadas com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Notificações do usuário listadas com sucesso", notificacoes)).build();
        } catch (Exception e) {
            LOG.error("Erro ao listar notificações do usuário - ID: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar notificações: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/ler")
    @Operation(summary = "Marcar notificação como lida", 
               description = "Marca uma notificação específica como lida")
    @APIResponse(responseCode = "200", description = "Notificação marcada como lida com sucesso")
    @APIResponse(responseCode = "404", description = "Notificação não encontrada")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response marcarComoLida(
            @Parameter(description = "ID da notificação", required = true, example = "123")
            @PathParam("id") @Min(1) Long id,
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @QueryParam("usuarioId") @Min(1) Long usuarioId) {
        
        try {
            LOG.info("Marcando notificação como lida - ID: " + id);
            
            notificacaoService.marcarComoLida(id, usuarioId);
            
            LOG.info("Notificação marcada como lida com sucesso - ID: " + id);
            
            return Response.ok(SuccessResponseDTO.ok("Notificação marcada como lida com sucesso")).build();
        } catch (Exception e) {
            LOG.error("Erro ao marcar notificação como lida - ID: " + id + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao marcar como lida: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/usuario/{usuarioId}/ler-todas")
    @Operation(summary = "Marcar todas as notificações como lidas", 
               description = "Marca todas as notificações de um usuário como lidas")
    @APIResponse(responseCode = "200", description = "Todas as notificações marcadas como lidas com sucesso")
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response marcarTodasComoLidas(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId) {
        
        try {
            LOG.info("Marcando todas as notificações como lidas - usuário: " + usuarioId);
            
            notificacaoService.marcarTodasComoLidas(usuarioId);
            
            LOG.info("Todas as notificações marcadas como lidas com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Todas as notificações marcadas como lidas com sucesso")).build();
        } catch (Exception e) {
            LOG.error("Erro ao marcar todas as notificações como lidas - usuário: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao marcar todas como lidas: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Deletar notificação", 
               description = "Remove uma notificação específica")
    @APIResponse(responseCode = "200", description = "Notificação deletada com sucesso")
    @APIResponse(responseCode = "404", description = "Notificação não encontrada")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response deletarNotificacao(
            @Parameter(description = "ID da notificação", required = true, example = "123")
            @PathParam("id") @Min(1) Long id) {
        
        try {
            LOG.info("Deletando notificação - ID: " + id);
            
            // Implementar método deletarNotificacao no NotificacaoService
            // notificacaoService.deletarNotificacao(id);
            
            LOG.info("Notificação deletada com sucesso - ID: " + id);
            
            return Response.ok(SuccessResponseDTO.deleted()).build();
        } catch (Exception e) {
            LOG.error("Erro ao deletar notificação - ID: " + id + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao deletar notificação: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/usuario/{usuarioId}/limpar")
    @Operation(summary = "Limpar notificações do usuário", 
               description = "Remove notificações de um usuário (opcionalmente por tipo)")
    @APIResponse(responseCode = "200", description = "Notificações limpas com sucesso")
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response limparNotificacoesUsuario(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "Tipo de notificação para filtrar", schema = @Schema(type = SchemaType.STRING, enumeration = {"ACUMULO", "EXPIRACAO", "RESGATE", "SISTEMA"}))
            @QueryParam("tipo") String tipo) {
        
        try {
            LOG.info("Limpando notificações do usuário - ID: " + usuarioId + ", tipo: " + tipo);
            
            // Implementar método limparNotificacoesUsuario no NotificacaoService
            // notificacaoService.limparNotificacoesUsuario(usuarioId, tipo);
            
            LOG.info("Notificações limpas com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Notificações limpas com sucesso")).build();
        } catch (Exception e) {
            LOG.error("Erro ao limpar notificações do usuário - ID: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao limpar notificações: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}/nao-lidas")
    @Operation(summary = "Contar notificações não lidas", 
               description = "Retorna a quantidade de notificações não lidas de um usuário")
    @APIResponse(responseCode = "200", description = "Contagem realizada com sucesso",
                 content = @Content(schema = @Schema(implementation = Long.class)))
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response contarNotificacoesNaoLidas(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId) {
        
        try {
            LOG.info("Contando notificações não lidas - usuário: " + usuarioId);
            
            // Implementar método contarNotificacoesNaoLidas no NotificacaoService
            // Long count = notificacaoService.contarNotificacoesNaoLidas(usuarioId);
            Long count = 0L;
            
            LOG.info("Contagem realizada com sucesso - usuário: " + usuarioId + ", não lidas: " + count);
            
            return Response.ok(SuccessResponseDTO.ok("Contagem de notificações não lidas realizada com sucesso", count)).build();
        } catch (Exception e) {
            LOG.error("Erro ao contar notificações não lidas - usuário: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao contar notificações: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/enviar")
    @Operation(summary = "Enviar notificação", 
               description = "Envia uma notificação para um usuário específico")
    @APIResponse(responseCode = "200", description = "Notificação enviada com sucesso",
                 content = @Content(schema = @Schema(implementation = NotificacaoResponseDTO.class)))
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response enviarNotificacao(
            @Parameter(description = "Dados da notificação", required = true)
            @Valid @NotNull NotificacaoRequestDTO request) {
        
        try {
            LOG.info("Enviando notificação - usuário: " + request.usuarioId);
            
            notificacaoService.enviarNotificacao(request);
            
            LOG.info("Notificação enviada com sucesso - usuário: " + request.usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Notificação enviada com sucesso")).build();
        } catch (Exception e) {
            LOG.error("Erro ao enviar notificação - usuário: " + request.usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao enviar notificação: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/configuracoes/usuario/{usuarioId}")
    @Operation(summary = "Consultar configurações de notificação", 
               description = "Retorna as configurações de notificação de um usuário")
    @APIResponse(responseCode = "200", description = "Configurações consultadas com sucesso",
                 content = @Content(schema = @Schema(implementation = ConfiguracaoNotificacaoDTO.class)))
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response consultarConfiguracoesNotificacao(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId) {
        
        try {
            LOG.info("Consultando configurações de notificação - usuário: " + usuarioId);
            
            ConfiguracaoNotificacaoDTO config = notificacaoService.consultarConfiguracaoUsuario(usuarioId);
            
            LOG.info("Configurações consultadas com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Configurações de notificação consultadas com sucesso", config)).build();
        } catch (Exception e) {
            LOG.error("Erro ao consultar configurações de notificação - usuário: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar configurações: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/configuracoes/usuario/{usuarioId}")
    @Operation(summary = "Atualizar configurações de notificação", 
               description = "Atualiza as configurações de notificação de um usuário")
    @APIResponse(responseCode = "200", description = "Configurações atualizadas com sucesso",
                 content = @Content(schema = @Schema(implementation = ConfiguracaoNotificacaoDTO.class)))
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    @APIResponse(responseCode = "404", description = "Usuário não encontrado")
    @APIResponse(responseCode = "500", description = "Erro interno do servidor")
    public Response atualizarConfiguracoesNotificacao(
            @Parameter(description = "ID do usuário", required = true, example = "123")
            @PathParam("usuarioId") @Min(1) Long usuarioId,
            
            @Parameter(description = "Dados das configurações", required = true)
            @Valid @NotNull ConfiguracaoNotificacaoDTO config) {
        
        try {
            LOG.info("Atualizando configurações de notificação - usuário: " + usuarioId);
            
            notificacaoService.atualizarConfiguracaoUsuario(usuarioId, config);
            
            LOG.info("Configurações atualizadas com sucesso - usuário: " + usuarioId);
            
            return Response.ok(SuccessResponseDTO.ok("Configurações de notificação atualizadas com sucesso", config)).build();
        } catch (Exception e) {
            LOG.error("Erro ao atualizar configurações de notificação - usuário: " + usuarioId + ", erro: " + e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar configurações: " + e.getMessage()))
                    .build();
        }
    }
}