package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.ResgateRequestDTO;
import org.acme.loyalty.dto.ResgateDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;

import java.util.List;

@Path("/resgates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResgateResource {

    @POST
    public Response solicitarResgate(ResgateRequestDTO request) {
        try {
            // TODO: Implementar serviço de resgate
            ResgateDTO resgate = new ResgateDTO();
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(resgate))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao solicitar resgate: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    public Response listarResgates(
            @QueryParam("usuarioId") Long usuarioId,
            @QueryParam("cartaoId") Long cartaoId,
            @QueryParam("status") String status,
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim,
            @QueryParam("pagina") @DefaultValue("1") Integer pagina,
            @QueryParam("tamanho") @DefaultValue("20") Integer tamanho) {
        
        try {
            // TODO: Implementar serviço de listagem
            List<ResgateDTO> resgates = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Resgates listados com sucesso", resgates)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar resgates: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response buscarResgate(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de busca
            ResgateDTO resgate = new ResgateDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Resgate encontrado com sucesso", resgate)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Resgate não encontrado"))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/aprovar")
    public Response aprovarResgate(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de aprovação
            ResgateDTO resgate = new ResgateDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Resgate aprovado com sucesso", resgate)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao aprovar resgate: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/concluir")
    public Response concluirResgate(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de conclusão
            ResgateDTO resgate = new ResgateDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Resgate concluído com sucesso", resgate)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao concluir resgate: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/negar")
    public Response negarResgate(
            @PathParam("id") Long id,
            @QueryParam("motivo") String motivo) {
        try {
            // TODO: Implementar serviço de negação
            ResgateDTO resgate = new ResgateDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Resgate negado com sucesso", resgate)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao negar resgate: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/cancelar")
    public Response cancelarResgate(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de cancelamento
            ResgateDTO resgate = new ResgateDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Resgate cancelado com sucesso", resgate)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao cancelar resgate: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/status")
    public Response consultarStatusResgate(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de consulta de status
            
            return Response.ok(SuccessResponseDTO.ok("Status do resgate consultado com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar status: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}")
    public Response listarResgatesUsuario(
            @PathParam("usuarioId") Long usuarioId,
            @QueryParam("status") String status,
            @QueryParam("pagina") @DefaultValue("1") Integer pagina,
            @QueryParam("tamanho") @DefaultValue("20") Integer tamanho) {
        
        try {
            // TODO: Implementar serviço de resgates por usuário
            List<ResgateDTO> resgates = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Resgates do usuário listados com sucesso", resgates)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar resgates do usuário: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/rastreio")
    public Response consultarRastreioResgate(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de rastreio
            
            return Response.ok(SuccessResponseDTO.ok("Rastreio do resgate consultado com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar rastreio: " + e.getMessage()))
                    .build();
        }
    }
}

