package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.*;
import org.acme.loyalty.service.CampanhaBonusService;
import org.acme.loyalty.exception.NotFoundException;

import java.util.List;

@Path("/campanhas-bonus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CampanhaBonusResource {

    @Inject
    CampanhaBonusService campanhaBonusService;

    @POST
    public Response criarCampanha(CampanhaBonusRequestDTO request) {
        try {
            CampanhaBonusResponseDTO campanha = campanhaBonusService.criarCampanha(request);
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(campanha))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao criar campanha: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro interno ao criar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    public Response listarCampanhas(
            @QueryParam("nome") String nome,
            @QueryParam("segmento") String segmento,
            @QueryParam("ativo") Boolean ativo,
            @QueryParam("vigente") Boolean vigente,
            @QueryParam("pagina") @DefaultValue("0") Integer pagina,
            @QueryParam("tamanho") @DefaultValue("10") Integer tamanho) {
        try {
            List<CampanhaBonusResponseDTO> campanhas = campanhaBonusService.listarCampanhas(nome, segmento, ativo, vigente, pagina, tamanho);
            
            return Response.ok(SuccessResponseDTO.ok("Campanhas listadas com sucesso", campanhas)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro interno ao listar campanhas: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response buscarCampanha(@PathParam("id") Long id) {
        try {
            CampanhaBonusResponseDTO campanha = campanhaBonusService.buscarCampanhaPorId(id);
            
            return Response.ok(SuccessResponseDTO.ok("Campanha encontrada com sucesso", campanha)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Campanha não encontrada: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro interno ao buscar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response atualizarCampanha(@PathParam("id") Long id, CampanhaBonusUpdateDTO request) {
        try {
            CampanhaBonusResponseDTO campanha = campanhaBonusService.atualizarCampanha(id, request);
            
            return Response.ok(SuccessResponseDTO.updated(campanha)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Campanha não encontrada: " + e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar campanha: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro interno ao atualizar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletarCampanha(@PathParam("id") Long id) {
        try {
            campanhaBonusService.deletarCampanha(id);
            
            return Response.ok(SuccessResponseDTO.ok("Campanha deletada com sucesso", null)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Campanha não encontrada: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro interno ao deletar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/ativar")
    public Response ativarCampanha(@PathParam("id") Long id) {
        try {
            CampanhaBonusResponseDTO campanha = campanhaBonusService.ativarCampanha(id);
            return Response.ok(SuccessResponseDTO.ok("Campanha ativada com sucesso", campanha)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Campanha não encontrada: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro interno ao ativar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/{id}/desativar")
    public Response desativarCampanha(@PathParam("id") Long id) {
        try {
            CampanhaBonusResponseDTO campanha = campanhaBonusService.desativarCampanha(id);
            return Response.ok(SuccessResponseDTO.ok("Campanha desativada com sucesso", campanha)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Campanha não encontrada: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro interno ao desativar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/status")
    public Response consultarStatusCampanha(@PathParam("id") Long id) {
        try {
            // Implementar serviço de consulta de status quando necessário
            
            // Implementar método consultarStatusCampanhaBonus no CampanhaBonusService
            // CampanhaBonusDTO campanha = campanhaBonusService.consultarStatusCampanhaBonus(id);
            CampanhaBonusDTO campanha = new CampanhaBonusDTO();
            return Response.ok(SuccessResponseDTO.ok("Status da campanha consultado com sucesso", campanha)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar status: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/vigentes")
    public Response listarCampanhasVigentes() {
        try {
            // Implementar serviço de campanhas vigentes quando necessário
            List<CampanhaBonusDTO> campanhas = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Campanhas vigentes listadas com sucesso", campanhas)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar campanhas vigentes: " + e.getMessage()))
                    .build();
        }
    }
}




