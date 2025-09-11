package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class TestResourceTest {

    @Test
    @Order(1)
    void testCriarDadosTeste() {
        given()
            .when()
            .post("/test/dados")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data.usuarios", notNullValue())
            .body("data.cartoes", notNullValue())
            .body("data.recompensas", notNullValue())
            .body("data.regras", notNullValue())
            .body("data.campanhas", notNullValue());
    }

    @Test
    @Order(2)
    void testLimparDadosTeste() {
        given()
            .when()
            .delete("/test/dados")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data.mensagem", is("Dados de teste limpos com sucesso"));
    }

    @Test
    @Order(3)
    void testConsultarStatusTeste() {
        given()
            .when()
            .get("/test/status")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data.status", is("OK"))
            .body("data.timestamp", notNullValue());
    }

    @Test
    @Order(4)
    void testExecutarTesteIntegracao() {
        given()
            .when()
            .post("/test/integracao")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data.status", is("OK"))
            .body("data.timestamp", notNullValue());
    }
}
