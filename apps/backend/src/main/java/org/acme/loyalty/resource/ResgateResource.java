package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.ErrorResponseDTO;
import org.acme.loyalty.dto.PageResponseDTO;
import org.acme.loyalty.dto.ResgateRequestDTO;
import org.acme.loyalty.dto.ResgateResponseDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.service.ResgateService;

import java.util.List;

@Path("/resgates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResgateResource {

    @Inject
    ResgateService resgateService;

    @POST
    public Response solicitarResgate(ResgateRequestDTO request) {
        try {
            ResgateResponseDTO resgate = resgateService.solicitarResgate(request);
            
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
            List<ResgateResponseDTO> resgates = resgateService.listarResgates(status, usuarioId, cartaoId, null, dataInicio, dataFim, pagina, tamanho);
            
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
            // Implementar serviço de busca
            ResgateResponseDTO resgate = new ResgateResponseDTO();
            
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
            ResgateResponseDTO resgate = resgateService.aprovarResgate(id, null);
            
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
            ResgateResponseDTO resgate = resgateService.concluirResgate(id, null);
            
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
            ResgateResponseDTO resgate = resgateService.negarResgate(id, motivo);
            
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
            // Implementar serviço de cancelamento
            ResgateResponseDTO resgate = new ResgateResponseDTO();
            
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
            // Implementar serviço de consulta de status
            
            // Implementar método consultarStatusResgate no ResgateService
            // ResgateResponseDTO resgate = resgateService.consultarStatusResgate(id);
            ResgateResponseDTO resgate = new ResgateResponseDTO();
            return Response.ok(SuccessResponseDTO.ok("Status do resgate consultado com sucesso", resgate)).build();
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
            PageResponseDTO<ResgateResponseDTO> resgates = resgateService.listarResgatesUsuario(usuarioId, status, pagina, tamanho);
            
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
            // Implementar serviço de rastreio
            
            // Implementar método consultarRastreioResgate no ResgateService
            // ResgateResponseDTO resgate = resgateService.consultarRastreioResgate(id);
            ResgateResponseDTO resgate = new ResgateResponseDTO();
            return Response.ok(SuccessResponseDTO.ok("Rastreio do resgate consultado com sucesso", resgate)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar rastreio: " + e.getMessage()))
                    .build();
        }
    }
}




