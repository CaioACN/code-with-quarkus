package org.acme.loyalty.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.RegraConversaoDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;

import java.util.List;

@Path("/regras")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegraConversaoResource {

    @POST
    public Response criarRegra(RegraConversaoDTO request) {
        try {
            // Implementar serviço de regras
            RegraConversaoDTO regra = new RegraConversaoDTO();
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(regra))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao criar regra: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    public Response listarRegras(
            @QueryParam("ativo") Boolean ativo,
            @QueryParam("mcc") String mcc,
            @QueryParam("categoria") String categoria,
            @QueryParam("parceiroId") Long parceiroId,
            @QueryParam("vigente") Boolean vigente) {
        
        try {
            // Implementar serviço de listagem
            List<RegraConversaoDTO> regras = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Regras listadas com sucesso", regras)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar regras: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response buscarRegra(@PathParam("id") Long id) {
        try {
            // Implementar serviço de busca
            RegraConversaoDTO regra = new RegraConversaoDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Regra encontrada com sucesso", regra)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Regra não encontrada"))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response atualizarRegra(@PathParam("id") Long id, RegraConversaoDTO request) {
        try {
            // Implementar serviço de atualização
            RegraConversaoDTO regra = new RegraConversaoDTO();
            
            return Response.ok(SuccessResponseDTO.updated(regra)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar regra: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletarRegra(@PathParam("id") Long id) {
        try {
            // Implementar serviço de exclusão
            
            return Response.ok(SuccessResponseDTO.deleted()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao deletar regra: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/ativar")
    public Response ativarRegra(@PathParam("id") Long id) {
        try {
            // Implementar serviço de ativação
            
            // Implementar método ativarRegraConversao no RegraConversaoService
            // RegraConversaoDTO regra = regraConversaoService.ativarRegraConversao(id);
            RegraConversaoDTO regra = new RegraConversaoDTO();
            return Response.ok(SuccessResponseDTO.ok("Regra ativada com sucesso", regra)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao ativar regra: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/desativar")
    public Response desativarRegra(@PathParam("id") Long id) {
        try {
            // Implementar serviço de desativação
            
            // Implementar método desativarRegraConversao no RegraConversaoService
            // RegraConversaoDTO regra = regraConversaoService.desativarRegraConversao(id);
            RegraConversaoDTO regra = new RegraConversaoDTO();
            return Response.ok(SuccessResponseDTO.ok("Regra desativada com sucesso", regra)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao desativar regra: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/aplicacao")
    public Response consultarAplicacaoRegra(@PathParam("id") Long id) {
        try {
            // Implementar serviço de consulta de aplicação
            
            // Implementar método consultarAplicacaoRegraConversao no RegraConversaoService
            // RegraConversaoDTO regra = regraConversaoService.consultarAplicacaoRegraConversao(id);
            RegraConversaoDTO regra = new RegraConversaoDTO();
            return Response.ok(SuccessResponseDTO.ok("Aplicação da regra consultada com sucesso", regra)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar aplicação: " + e.getMessage()))
                    .build();
        }
    }
}




