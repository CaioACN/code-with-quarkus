package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.TransacaoRequestDTO;
import org.acme.loyalty.dto.TransacaoResponseDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;
import org.acme.loyalty.service.TransacaoService;
import org.acme.loyalty.entity.Transacao;

import java.util.List;

@Path("/transacoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransacaoResource {

    @Inject
    TransacaoService transacaoService;

    @POST
    public Response criarTransacao(TransacaoRequestDTO request) {
        try {
            Transacao transacao = transacaoService.criarTransacao(request);
            TransacaoResponseDTO response = new TransacaoResponseDTO(transacao.id);
            
            return Response.status(Response.Status.CREATED)
                    .entity(SuccessResponseDTO.created(response))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao criar transação: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    public Response listarTransacoes(
            @QueryParam("usuarioId") Long usuarioId,
            @QueryParam("cartaoId") Long cartaoId,
            @QueryParam("status") String status,
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim,
            @QueryParam("pagina") @DefaultValue("1") Integer pagina,
            @QueryParam("tamanho") @DefaultValue("20") Integer tamanho) {
        
        try {
            List<TransacaoResponseDTO> transacoes = transacaoService.listarTransacoes(
                usuarioId, cartaoId, status, dataInicio, dataFim, pagina, tamanho);
            
            return Response.ok(SuccessResponseDTO.ok(transacoes)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao listar transações: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    public Response buscarTransacao(@PathParam("id") Long id) {
        try {
            TransacaoResponseDTO transacao = transacaoService.buscarTransacaoPorId(id);
            if (transacao != null) {
                return Response.ok(SuccessResponseDTO.ok(transacao)).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(ErrorResponseDTO.notFound("Transação não encontrada"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao buscar transação: " + e.getMessage()))
                    .build();
        }
    }

    @PUT
    @Path("/{id}/status")
    public Response atualizarStatus(
            @PathParam("id") Long id,
            @QueryParam("status") String novoStatus) {
        try {
            TransacaoResponseDTO transacao = transacaoService.atualizarStatus(id, novoStatus);
            return Response.ok(SuccessResponseDTO.updated(transacao)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao atualizar status: " + e.getMessage()))
                    .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletarTransacao(@PathParam("id") Long id) {
        try {
            transacaoService.deletarTransacao(id);
            return Response.ok(SuccessResponseDTO.deleted()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(ErrorResponseDTO.badRequest("Erro ao deletar transação: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{id}/pontos")
    public Response consultarPontosTransacao(@PathParam("id") Long id) {
        try {
            Integer pontos = transacaoService.consultarPontosTransacao(id);
            return Response.ok(SuccessResponseDTO.ok("Pontos da transação", pontos)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ErrorResponseDTO.notFound("Transação não encontrada ou sem pontos"))
                    .build();
        }
    }
}

