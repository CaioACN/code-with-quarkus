package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.RecompensaRequestDTO;
import org.acme.loyalty.dto.RecompensaResponseDTO;
import org.acme.loyalty.dto.RecompensaUpdateDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;
import org.acme.loyalty.dto.TestDTO;
import org.acme.loyalty.service.RecompensaService;

import java.util.List;

@Path("/recompensas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RecompensaResource {

    @Inject
    RecompensaService recompensaService;

    @GET
    public Response listarRecompensas(
            @QueryParam("ativo") Boolean ativo,
            @QueryParam("tipo") String tipo,
            @QueryParam("parceiroId") Long parceiroId,
            @QueryParam("disponivel") Boolean disponivel,
            @QueryParam("custoMin") Long custoMin,
            @QueryParam("custoMax") Long custoMax,
            @QueryParam("descricao") String descricao,
            @QueryParam("pagina") Integer pagina,
            @QueryParam("tamanho") Integer tamanho) {
        
        try {
            List<RecompensaResponseDTO> recompensas = recompensaService.listarRecompensas(
                tipo, descricao, parceiroId, ativo, pagina, tamanho);
            
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
            RecompensaResponseDTO recompensa = recompensaService.buscarRecompensaPorId(id);
            
            return Response.ok(SuccessResponseDTO.ok("Recompensa encontrada com sucesso", recompensa)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao buscar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    public Response criarRecompensa(RecompensaRequestDTO request) {
        try {
            var recompensa = recompensaService.criarRecompensa(request);
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(recompensa))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao criar recompensa: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao criar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response atualizarRecompensa(@PathParam("id") Long id, RecompensaUpdateDTO request) {
        try {
            RecompensaResponseDTO recompensa = recompensaService.atualizarRecompensa(id, request);
            
            return Response.ok(SuccessResponseDTO.updated(recompensa)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar recompensa: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao atualizar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletarRecompensa(@PathParam("id") Long id) {
        try {
            recompensaService.deletarRecompensa(id);
            
            return Response.ok(SuccessResponseDTO.deleted()).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao deletar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/ativar")
    public Response ativarRecompensa(@PathParam("id") Long id) {
        try {
            RecompensaResponseDTO recompensa = recompensaService.ativarRecompensa(id);
            return Response.ok(SuccessResponseDTO.ok("Recompensa ativada com sucesso", recompensa)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao ativar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/desativar")
    public Response desativarRecompensa(@PathParam("id") Long id) {
        try {
            RecompensaResponseDTO recompensa = recompensaService.desativarRecompensa(id);
            return Response.ok(SuccessResponseDTO.ok("Recompensa desativada com sucesso", recompensa)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao desativar recompensa: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/estoque")
    public Response consultarEstoqueRecompensa(@PathParam("id") Long id) {
        try {
            RecompensaResponseDTO recompensa = recompensaService.buscarRecompensaPorId(id);
            return Response.ok(SuccessResponseDTO.ok("Estoque da recompensa consultado com sucesso", recompensa)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
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
            @QueryParam("motivo") String motivo) {
        try {
            RecompensaResponseDTO recompensa = recompensaService.ajustarEstoque(id, quantidade, motivo);
            return Response.ok(SuccessResponseDTO.ok("Estoque atualizado com sucesso", recompensa)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Recompensa não encontrada"))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar estoque: " + e.getMessage()))
                    .build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar estoque: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao atualizar estoque: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/disponiveis")
    public Response listarRecompensasDisponiveis() {
        try {
            List<RecompensaResponseDTO> recompensas = recompensaService.listarRecompensasDisponiveis();
            
            return Response.ok(SuccessResponseDTO.ok("Recompensas disponíveis listadas com sucesso", recompensas)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar recompensas disponíveis: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/test")
    public Response testEndpoint() {
        try {
            // Criar um DTO de teste simples
            TestDTO testDto = new TestDTO(999L, "Teste", "Teste de serialização");
            
            System.out.println("DEBUG: Test DTO criado: " + testDto);
            System.out.println("DEBUG: Test DTO ID: " + testDto.id);
            System.out.println("DEBUG: Test DTO Nome: " + testDto.nome);
            System.out.println("DEBUG: Test DTO Descrição: " + testDto.descricao);
            
            return Response.ok(SuccessResponseDTO.ok("Teste de serialização", testDto)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro no teste: " + e.getMessage()))
                    .build();
        }
    }
}




