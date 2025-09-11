package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class SimpleDataTest {

    @Test
    void testHealthEndpoint() {
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testRecompensasList() {
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    void testCreateRecompensaWithMinimalData() {
        String recompensaJson = """
            {
                "tipo": "PRODUTO",
                "descricao": "Teste Simples",
                "custoPontos": 1000,
                "estoque": 5,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Teste",
                "imagemUrl": "https://example.com/test.jpg",
                "validadeRecompensa": "2025-12-31"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(recompensaJson)
            .when()
            .post("/recompensas")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data.descricao", is("Teste Simples"));
    }

    @Test
    void testCreateRecompensaWithErrorDetails() {
        String recompensaJson = """
            {
                "tipo": "PRODUTO",
                "descricao": "Teste com Erro",
                "custoPontos": 1000,
                "estoque": 5,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Teste",
                "imagemUrl": "https://example.com/test.jpg",
                "validadeRecompensa": "2025-12-31"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(recompensaJson)
            .when()
            .post("/recompensas")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }
}
