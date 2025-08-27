package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.RecompensaDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;

import java.util.List;

@Path("/recompensas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecompensaResource {

    @GET
    public Response listarRecompensas(
            @QueryParam("ativo") Boolean ativo,
            @QueryParam("tipo") String tipo,
            @QueryParam("parceiroId") Long parceiroId,
            @QueryParam("disponivel") Boolean disponivel,
            @QueryParam("custoMin") Long custoMin,
            @QueryParam("custoMax") Long custoMax) {
        
        try {
            // TODO: Implementar serviço de listagem
            List<RecompensaDTO> recompensas = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Recompensas listadas com sucesso", recompensas)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar recompensas: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response buscarRecompensa(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de busca
            RecompensaDTO recompensa = new RecompensaDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Recompensa encontrada com sucesso", recompensa)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
        }
    }

    @POST
    public Response criarRecompensa(RecompensaDTO request) {
        try {
            // TODO: Implementar serviço de criação
            RecompensaDTO recompensa = new RecompensaDTO();
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(recompensa))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao criar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response atualizarRecompensa(@PathParam("id") Long id, RecompensaDTO request) {
        try {
            // TODO: Implementar serviço de atualização
            RecompensaDTO recompensa = new RecompensaDTO();
            
            return Response.ok(SuccessResponseDTO.updated(recompensa)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletarRecompensa(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de exclusão
            
            return Response.ok(SuccessResponseDTO.deleted()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao deletar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/ativar")
    public Response ativarRecompensa(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de ativação
            
            return Response.ok(SuccessResponseDTO.ok("Recompensa ativada com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao ativar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/desativar")
    public Response desativarRecompensa(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de desativação
            
            return Response.ok(SuccessResponseDTO.ok("Recompensa desativada com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao desativar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/estoque")
    public Response consultarEstoqueRecompensa(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de consulta de estoque
            
            return Response.ok(SuccessResponseDTO.ok("Estoque da recompensa consultado com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar estoque: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/estoque")
    public Response atualizarEstoqueRecompensa(
            @PathParam("id") Long id,
            @QueryParam("quantidade") Long quantidade,
            @QueryParam("operacao") String operacao) {
        try {
            // TODO: Implementar serviço de atualização de estoque
            
            return Response.ok(SuccessResponseDTO.ok("Estoque atualizado com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar estoque: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/disponiveis")
    public Response listarRecompensasDisponiveis() {
        try {
            // TODO: Implementar serviço de recompensas disponíveis
            List<RecompensaDTO> recompensas = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Recompensas disponíveis listadas com sucesso", recompensas)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar recompensas disponíveis: " + e.getMessage()))
                    .build();
        }
    }
}

