package org.acme.loyalty.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.loyalty.dto.RecompensaSimpleDTO;

@Path("/test")
public class TestResource {

    @GET
    @Path("/simple")
    @Produces(MediaType.APPLICATION_JSON)
    public Response testSimple() {
        RecompensaSimpleDTO dto = new RecompensaSimpleDTO(1L, "GIFT", "Teste", 100L, 10L, true);
        return Response.ok(dto).build();
    }
}
