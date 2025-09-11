package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class PontosResourceTest {

    @Test
    @Order(1)
    void testConsultarSaldoUsuario() {
        given()
            .when()
            .get("/usuarios/1/pontos/saldo")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.usuarioId", is(1));
    }

    @Test
    @Order(2)
    void testConsultarExtratoUsuario() {
        given()
            .queryParam("cartaoId", 1)
            .when()
            .get("/usuarios/1/pontos/extrato")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(3)
    void testConsultarExtratoComFiltros() {
        given()
            .queryParam("cartaoId", 1)
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .queryParam("tipoMovimento", "ACUMULO")
            .when()
            .get("/usuarios/1/pontos/extrato")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(4)
    void testConsultarSaldoUsuarioInexistente() {
        given()
            .when()
            .get("/usuarios/999999/pontos/saldo")
            .then()
            .statusCode(404);
    }
}
