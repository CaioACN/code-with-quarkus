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
class TransacaoResourceTest {

    @Test
    @Order(1)
    void testCriarTransacao() {
        String transacaoJson = """
            {
                "usuarioId": 1,
                "cartaoId": 1,
                "valor": 100.50,
                "moeda": "BRL",
                "mcc": "5411",
                "categoria": "Supermercado",
                "parceiroId": 1,
                "dataEvento": "2025-09-09T10:00:00",
                "autorizacao": "AUTH123456"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(transacaoJson)
            .when()
            .post("/transacoes")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("data.usuarioId", is(1))
            .body("data.cartaoId", is(1))
            .body("data.valor", is(100.50f))
            .body("data.moeda", is("BRL"))
            .body("data.mcc", is("5411"))
            .body("data.categoria", is("Supermercado"))
            .body("data.parceiroId", is(1))
            .body("data.status", is("APROVADA"))
            .body("data.id", notNullValue());
    }

    @Test
    @Order(2)
    void testListarTransacoes() {
        given()
            .when()
            .get("/transacoes")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data", hasSize(greaterThan(0)));
    }

    @Test
    @Order(3)
    void testListarTransacoesComFiltros() {
        given()
            .queryParam("usuarioId", 1)
            .queryParam("status", "APROVADA")
            .when()
            .get("/transacoes")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(4)
    void testCriarTransacaoComDadosInvalidos() {
        String transacaoJson = """
            {
                "usuarioId": null,
                "cartaoId": null,
                "valor": -100.50,
                "moeda": "INVALID",
                "mcc": "12345"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(transacaoJson)
            .when()
            .post("/transacoes")
            .then()
            .statusCode(400);
    }
}
