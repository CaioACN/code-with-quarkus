package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class DiagnosticTest {

    @Test
    void testCreateRecompensaWithErrorResponse() {
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
            .statusCode(400)
            .log().all(); // Mostra a resposta completa para diagnosticar
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
    void testRecompensasList() {
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .log().all(); // Mostra a resposta para ver se há dados
    }
}
