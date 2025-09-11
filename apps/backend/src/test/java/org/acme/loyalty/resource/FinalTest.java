package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class FinalTest {

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
    void testCreateRecompensaSuccess() {
        String recompensaJson = """
            {
                "tipo": "PRODUTO",
                "descricao": "Produto de Teste Final",
                "custoPontos": 1000,
                "estoque": 5,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Produto de teste para validação final",
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
            .body("data.descricao", is("Produto de Teste Final"));
    }

    @Test
    void testCreateMultipleRecompensas() {
        // Criar recompensa 1
        String recompensa1 = """
            {
                "tipo": "GIFT",
                "descricao": "Vale Presente R$ 50",
                "custoPontos": 2500,
                "estoque": 10,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Vale presente para lojas parceiras",
                "imagemUrl": "https://example.com/gift.jpg",
                "validadeRecompensa": "2025-12-31"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(recompensa1)
            .when()
            .post("/recompensas")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());

        // Criar recompensa 2
        String recompensa2 = """
            {
                "tipo": "MILHAS",
                "descricao": "5.000 Milhas Aéreas",
                "custoPontos": 5000,
                "estoque": 5,
                "parceiroId": 2,
                "ativo": true,
                "detalhes": "Milhas para qualquer destino nacional",
                "imagemUrl": "https://example.com/milhas.jpg",
                "validadeRecompensa": "2025-12-31"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(recompensa2)
            .when()
            .post("/recompensas")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());

        // Verificar se ambas aparecem na listagem
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
                "tipo": "CASHBACK",
                "descricao": "R$ 20 de Cashback",
                "custoPontos": 2000,
                "estoque": 50,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Cashback direto na conta",
                "imagemUrl": "https://example.com/cashback.jpg",
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
            .body("data.descricao", is("R$ 20 de Cashback"));
    }
}

