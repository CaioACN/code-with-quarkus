package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class RecompensaTest {

    @Test
    void testCreateRecompensaSuccess() {
        String recompensaJson = """
            {
                "tipo": "PRODUTO",
                "descricao": "Produto de Teste",
                "custoPontos": 1000,
                "estoque": 5,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Produto de teste",
                "imagemUrl": "https://example.com/produto.jpg",
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
            .body("data.descricao", is("Produto de Teste"));
    }

    @Test
    void testCreateRecompensaMinimal() {
        String recompensaJson = """
            {
                "tipo": "GIFT",
                "descricao": "Vale Presente",
                "custoPontos": 500,
                "estoque": 10
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
            .body("data.descricao", is("Vale Presente"));
    }

    @Test
    void testListRecompensas() {
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }
}

