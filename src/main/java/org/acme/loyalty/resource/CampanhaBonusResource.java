package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.CampanhaBonusDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;

import java.util.List;

@Path("/campanhas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CampanhaBonusResource {

    @POST
    public Response criarCampanha(CampanhaBonusDTO request) {
        try {
            // TODO: Implementar serviço de campanhas
            CampanhaBonusDTO campanha = new CampanhaBonusDTO();
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(campanha))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao criar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    public Response listarCampanhas(
            @QueryParam("ativo") Boolean ativo,
            @QueryParam("segmento") String segmento,
            @QueryParam("vigente") Boolean vigente,
            @QueryParam("proximaExpiracao") Boolean proximaExpiracao) {
        
        try {
            // TODO: Implementar serviço de listagem
            List<CampanhaBonusDTO> campanhas = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Campanhas listadas com sucesso", campanhas)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar campanhas: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response buscarCampanha(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de busca
            CampanhaBonusDTO campanha = new CampanhaBonusDTO();
            
            return Response.ok(SuccessResponseDTO.ok("Campanha encontrada com sucesso", campanha)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Campanha não encontrada"))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response atualizarCampanha(@PathParam("id") Long id, CampanhaBonusDTO request) {
        try {
            // TODO: Implementar serviço de atualização
            CampanhaBonusDTO campanha = new CampanhaBonusDTO();
            
            return Response.ok(SuccessResponseDTO.updated(campanha)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletarCampanha(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de exclusão
            
            return Response.ok(SuccessResponseDTO.deleted()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao deletar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/ativar")
    public Response ativarCampanha(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de ativação
            
            return Response.ok(SuccessResponseDTO.ok("Campanha ativada com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao ativar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/desativar")
    public Response desativarCampanha(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de desativação
            
            return Response.ok(SuccessResponseDTO.ok("Campanha desativada com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao desativar campanha: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/status")
    public Response consultarStatusCampanha(@PathParam("id") Long id) {
        try {
            // TODO: Implementar serviço de consulta de status
            
            return Response.ok(SuccessResponseDTO.ok("Status da campanha consultado com sucesso", null)).build();
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
            // TODO: Implementar serviço de campanhas vigentes
            List<CampanhaBonusDTO> campanhas = List.of();
            
            return Response.ok(SuccessResponseDTO.ok("Campanhas vigentes listadas com sucesso", campanhas)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar campanhas vigentes: " + e.getMessage()))
                    .build();
        }
    }
}

