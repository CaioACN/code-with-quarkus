package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class ResgateResourceTest {

    @Test
    @Order(1)
    void testSolicitarResgate() {
        String resgateJson = """
            {
                "usuarioId": 1,
                "cartaoId": 1,
                "recompensaId": 1
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(resgateJson)
            .when()
            .post("/resgates")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("data.usuarioId", is(1))
            .body("data.cartaoId", is(1))
            .body("data.recompensaId", is(1))
            .body("data.status", is("PENDENTE"))
            .body("data.id", notNullValue());
    }

    @Test
    @Order(2)
    void testListarResgates() {
        given()
            .when()
            .get("/resgates")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data", hasSize(greaterThan(0)));
    }

    @Test
    @Order(3)
    void testListarResgatesComFiltros() {
        given()
            .queryParam("usuarioId", 1)
            .queryParam("status", "PENDENTE")
            .when()
            .get("/resgates")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(4)
    void testSolicitarResgateComDadosInvalidos() {
        String resgateJson = """
            {
                "usuarioId": null,
                "cartaoId": null,
                "recompensaId": null
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(resgateJson)
            .when()
            .post("/resgates")
            .then()
            .statusCode(400);
    }
}
