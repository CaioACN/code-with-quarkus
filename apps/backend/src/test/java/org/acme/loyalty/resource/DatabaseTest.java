package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class DatabaseTest {

    @Test
    void testDatabaseConnection() {
        // Teste simples para verificar se o banco está funcionando
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testCreateAndListRecompensas() {
        // Criar uma recompensa
        String recompensaJson = """
            {
                "tipo": "PRODUTO",
                "descricao": "Produto de Teste",
                "custoPontos": 1000,
                "estoque": 5,
                "parceiroId": 1,
                "ativo": true,
                "detalhes": "Produto de teste para validação",
                "imagemUrl": "https://example.com/produto.jpg",
                "validadeRecompensa": "2025-12-31"
            }
            """;

        // Criar recompensa
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

        // Listar recompensas
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    void testCreateMultipleRecompensas() {
        // Criar várias recompensas para testar persistência
        String[] tipos = {"PRODUTO", "GIFT", "MILHAS", "CASHBACK"};
        
        for (int i = 0; i < tipos.length; i++) {
            String recompensaJson = String.format("""
                {
                    "tipo": "%s",
                    "descricao": "Recompensa %s %d",
                    "custoPontos": %d,
                    "estoque": 10,
                    "parceiroId": 1,
                    "ativo": true,
                    "detalhes": "Recompensa de teste %d",
                    "imagemUrl": "https://example.com/%s.jpg",
                    "validadeRecompensa": "2025-12-31"
                }
                """, tipos[i], tipos[i], i + 1, (i + 1) * 500, i + 1, tipos[i].toLowerCase());

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

        // Verificar se todas foram criadas
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }
}
