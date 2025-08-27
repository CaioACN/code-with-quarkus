package org.acme.loyalty.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.SuccessResponseDTO;
import org.acme.loyalty.dto.ErrorResponseDTO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    @GET
    public Response healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "Loyalty Points System");
            health.put("version", "1.0.0");
            
            return Response.ok(SuccessResponseDTO.ok("Sistema funcionando normalmente", health)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ErrorResponseDTO.internalError("Sistema com problemas: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/ready")
    public Response readinessCheck() {
        try {
            // TODO: Implementar verificações de readiness (banco, cache, etc.)
            Map<String, Object> readiness = new HashMap<>();
            readiness.put("status", "READY");
            readiness.put("database", "UP");
            readiness.put("cache", "UP");
            readiness.put("timestamp", LocalDateTime.now());
            
            return Response.ok(SuccessResponseDTO.ok("Sistema pronto para receber requisições", readiness)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ErrorResponseDTO.internalError("Sistema não está pronto: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/live")
    public Response livenessCheck() {
        try {
            Map<String, Object> liveness = new HashMap<>();
            liveness.put("status", "ALIVE");
            liveness.put("timestamp", LocalDateTime.now());
            liveness.put("uptime", "0s"); // TODO: Implementar cálculo de uptime
            
            return Response.ok(SuccessResponseDTO.ok("Sistema está vivo", liveness)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(ErrorResponseDTO.internalError("Sistema não está vivo: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/info")
    public Response systemInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("name", "Loyalty Points System");
            info.put("version", "1.0.0");
            info.put("description", "Sistema de fidelidade por pontos para cartões de crédito");
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("quarkusVersion", "3.x");
            info.put("timestamp", LocalDateTime.now());
            
            return Response.ok(SuccessResponseDTO.ok("Informações do sistema consultadas com sucesso", info)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar informações: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/metrics")
    public Response systemMetrics() {
        try {
            // TODO: Implementar métricas reais do sistema
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("timestamp", LocalDateTime.now());
            metrics.put("transacoesProcessadas", 0);
            metrics.put("pontosAcumulados", 0);
            metrics.put("resgatesRealizados", 0);
            metrics.put("usuariosAtivos", 0);
            metrics.put("uptime", "0s");
            
            return Response.ok(SuccessResponseDTO.ok("Métricas do sistema consultadas com sucesso", metrics)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro ao consultar métricas: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/ping")
    public Response ping() {
        try {
            Map<String, Object> pong = new HashMap<>();
            pong.put("message", "pong");
            pong.put("timestamp", LocalDateTime.now());
            
            return Response.ok(SuccessResponseDTO.ok("Ping realizado com sucesso", pong)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(ErrorResponseDTO.internalError("Erro no ping: " + e.getMessage()))
                    .build();
        }
    }
}

