package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class DataTest {

    @Test
    void testRecompensasEndpoint() {
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    void testCriarRecompensa() {
        String recompensaJson = """
            {
                "tipo": "PRODUTO",
                "descricao": "Teste de Recompensa",
                "custoPontos": 1000,
                "estoque": 5,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Recompensa de teste",
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
            .body("data.id", notNullValue())
            .body("data.descricao", is("Teste de Recompensa"));
    }

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
    void testRecompensasAfterCreate() {
        // Primeiro criar uma recompensa
        String recompensaJson = """
            {
                "tipo": "GIFT",
                "descricao": "Vale Presente Teste",
                "custoPontos": 500,
                "estoque": 10,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Vale presente de teste",
                "imagemUrl": "https://example.com/gift.jpg",
                "validadeRecompensa": "2025-12-31"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(recompensaJson)
            .when()
            .post("/recompensas")
            .then()
            .statusCode(201);

        // Depois verificar se aparece na listagem
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }
}
