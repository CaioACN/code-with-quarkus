package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;

@Path("/notificacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotificacaoResource {

    @GET
    @Path("/usuario/{usuarioId}")
    public Response listarNotificacoesUsuario(
            @PathParam("usuarioId") Long usuarioId,
            @QueryParam("tipo") String tipo,
            @QueryParam("lida") Boolean lida,
            @QueryParam("pagina") @DefaultValue("1") Integer pagina,
            @QueryParam("tamanho") @DefaultValue("20") Integer tamanho) {
        
        try {
            // TODO: Implementar serviço de listagem de notificações
            
            return Response.ok(SuccessResponseDTO.ok("Notificações do usuário listadas com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar notificações: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/ler")
    public Response marcarComoLida(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de marcação como lida
            
            return Response.ok(SuccessResponseDTO.ok("Notificação marcada como lida com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao marcar como lida: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/usuario/{usuarioId}/ler-todas")
    public Response marcarTodasComoLidas(@PathParam("usuarioId") Long usuarioId) {
        try {
            // TODO: Implementar serviço de marcação de todas como lidas
            
            return Response.ok(SuccessResponseDTO.ok("Todas as notificações marcadas como lidas com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao marcar todas como lidas: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletarNotificacao(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de exclusão de notificação
            
            return Response.ok(SuccessResponseDTO.deleted()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao deletar notificação: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/usuario/{usuarioId}/limpar")
    public Response limparNotificacoesUsuario(
            @PathParam("usuarioId") Long usuarioId,
            @QueryParam("tipo") String tipo) {
        try {
            // TODO: Implementar serviço de limpeza de notificações
            
            return Response.ok(SuccessResponseDTO.ok("Notificações limpas com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao limpar notificações: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}/nao-lidas")
    public Response contarNotificacoesNaoLidas(@PathParam("usuarioId") Long usuarioId) {
        try {
            // TODO: Implementar serviço de contagem de notificações não lidas
            
            return Response.ok(SuccessResponseDTO.ok("Contagem de notificações não lidas realizada com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao contar notificações: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/enviar")
    public Response enviarNotificacao(
            @QueryParam("usuarioId") Long usuarioId,
            @QueryParam("tipo") String tipo,
            @QueryParam("titulo") String titulo,
            @QueryParam("mensagem") String mensagem,
            @QueryParam("canal") String canal) {
        try {
            // TODO: Implementar serviço de envio de notificação
            
            return Response.ok(SuccessResponseDTO.ok("Notificação enviada com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao enviar notificação: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/configuracoes/usuario/{usuarioId}")
    public Response consultarConfiguracoesNotificacao(@PathParam("usuarioId") Long usuarioId) {
        try {
            // TODO: Implementar serviço de consulta de configurações
            
            return Response.ok(SuccessResponseDTO.ok("Configurações de notificação consultadas com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar configurações: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/configuracoes/usuario/{usuarioId}")
    public Response atualizarConfiguracoesNotificacao(
            @PathParam("usuarioId") Long usuarioId,
            @QueryParam("email") Boolean email,
            @QueryParam("sms") Boolean sms,
            @QueryParam("push") Boolean push) {
        try {
            // TODO: Implementar serviço de atualização de configurações
            
            return Response.ok(SuccessResponseDTO.ok("Configurações de notificação atualizadas com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar configurações: " + e.getMessage()))
                    .build();
        }
    }
}

