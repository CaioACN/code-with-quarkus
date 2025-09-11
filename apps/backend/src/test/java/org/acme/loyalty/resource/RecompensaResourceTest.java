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
class RecompensaResourceTest {

    @Test
    @Order(1)
    void testCriarRecompensa() {
        String recompensaJson = """
            {
                "tipo": "MILHAS",
                "descricao": "Recompensa de teste",
                "custoPontos": 1000,
                "estoque": 10,
                "parceiroId": 1,
                "detalhes": "Detalhes da recompensa de teste",
                "imagemUrl": "https://example.com/imagem.jpg",
                "validadeRecompensa": "2025-12-31T23:59:59"
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
            .body("data.tipo", is("MILHAS"))
            .body("data.descricao", is("Recompensa de teste"))
            .body("data.custoPontos", is(1000))
            .body("data.estoque", is(10))
            .body("data.parceiroId", is(1))
            .body("data.detalhes", is("Detalhes da recompensa de teste"))
            .body("data.imagemUrl", is("https://example.com/imagem.jpg"))
            .body("data.ativo", is(true))
            .body("data.id", notNullValue());
    }

    @Test
    @Order(2)
    void testListarRecompensas() {
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data", hasSize(greaterThan(0)));
    }

    @Test
    @Order(3)
    void testListarRecompensasDisponiveis() {
        given()
            .when()
            .get("/recompensas/disponiveis")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(4)
    void testCriarRecompensaComDadosInvalidos() {
        String recompensaJson = """
            {
                "tipo": "INVALIDO",
                "descricao": "",
                "custoPontos": -100,
                "estoque": -5
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(recompensaJson)
            .when()
            .post("/recompensas")
            .then()
            .statusCode(400);
    }
}
