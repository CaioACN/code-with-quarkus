package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.SaldoUsuarioDTO;
import org.acme.loyalty.dto.ExtratoPontosDTO;
import org.acme.loyalty.dto.ExtratoFilterDTO;
import org.acme.loyalty.dto.PageRequestDTO;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;

@Path("/usuarios/{usuarioId}/pontos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PontosResource {

    @GET
    @Path("/saldo")
    public Response consultarSaldo(@PathParam("usuarioId") Long usuarioId) {
        try {
            // TODO: Implementar serviço de pontos
            SaldoUsuarioDTO saldo = new SaldoUsuarioDTO(usuarioId, null);
            
            return Response.ok(SuccessResponseDTO.ok("Saldo consultado com sucesso", saldo)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar saldo: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/extrato")
    public Response consultarExtrato(
            @PathParam("usuarioId") Long usuarioId,
            @QueryParam("cartaoId") Long cartaoId,
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim,
            @QueryParam("tipoMovimento") String tipoMovimento,
            @QueryParam("categoria") String categoria,
            @QueryParam("parceiroId") Long parceiroId,
            @QueryParam("pagina") @DefaultValue("1") Integer pagina,
            @QueryParam("tamanho") @DefaultValue("20") Integer tamanho) {
        
        try {
            // TODO: Implementar serviço de extrato
            ExtratoFilterDTO filtros = new ExtratoFilterDTO();
            PageRequestDTO paginacao = new PageRequestDTO(pagina, tamanho);
            
            ExtratoPontosDTO extrato = new ExtratoPontosDTO(
                usuarioId, cartaoId, null, null, null, pagina, tamanho, 0L);
            
            return Response.ok(SuccessResponseDTO.ok("Extrato consultado com sucesso", extrato)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar extrato: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/saldo/{cartaoId}")
    public Response consultarSaldoCartao(
            @PathParam("usuarioId") Long usuarioId,
            @PathParam("cartaoId") Long cartaoId) {
        try {
            // TODO: Implementar serviço de saldo por cartão
            return Response.ok(SuccessResponseDTO.ok("Saldo do cartão consultado com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar saldo do cartão: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/movimentos")
    public Response consultarMovimentos(
            @PathParam("usuarioId") Long usuarioId,
            @QueryParam("cartaoId") Long cartaoId,
            @QueryParam("tipo") String tipo,
            @QueryParam("dataInicio") String dataInicio,
            @QueryParam("dataFim") String dataFim,
            @QueryParam("pagina") @DefaultValue("1") Integer pagina,
            @QueryParam("tamanho") @DefaultValue("20") Integer tamanho) {
        
        try {
            // TODO: Implementar serviço de movimentos
            return Response.ok(SuccessResponseDTO.ok("Movimentos consultados com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar movimentos: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/resumo")
    public Response consultarResumo(@PathParam("usuarioId") Long usuarioId) {
        try {
            // TODO: Implementar serviço de resumo
            return Response.ok(SuccessResponseDTO.ok("Resumo consultado com sucesso", null)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar resumo: " + e.getMessage()))
                    .build();
        }
    }
}

