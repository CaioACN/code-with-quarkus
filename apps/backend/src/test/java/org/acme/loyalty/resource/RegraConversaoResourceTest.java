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
class RegraConversaoResourceTest {

    @Test
    @Order(1)
    void testCriarRegraConversao() {
        String regraJson = """
            {
                "nome": "Regra Teste",
                "multiplicador": 1.5,
                "mccRegex": "5411",
                "categoria": "Supermercado",
                "parceiroId": 1,
                "vigenciaIni": "2025-01-01T00:00:00",
                "vigenciaFim": "2025-12-31T23:59:59",
                "prioridade": 1,
                "tetoMensal": 10000,
                "ativo": true
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(regraJson)
            .when()
            .post("/regras-conversao")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.nome", is("Regra Teste"))
            .body("data.multiplicador", is(1.5f))
            .body("data.mccRegex", is("5411"))
            .body("data.categoria", is("Supermercado"))
            .body("data.parceiroId", is(1))
            .body("data.prioridade", is(1))
            .body("data.tetoMensal", is(10000))
            .body("data.ativo", is(true))
            .body("data.id", notNullValue());
    }

    @Test
    @Order(2)
    void testListarRegrasConversao() {
        given()
            .when()
            .get("/regras-conversao")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data", hasSize(greaterThan(0)));
    }

    @Test
    @Order(3)
    void testListarRegrasConversaoComFiltros() {
        given()
            .queryParam("nome", "Regra")
            .queryParam("categoria", "Supermercado")
            .queryParam("ativo", true)
            .when()
            .get("/regras-conversao")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(4)
    void testCriarRegraConversaoComDadosInvalidos() {
        String regraJson = """
            {
                "nome": "",
                "multiplicador": -1.5,
                "prioridade": -1,
                "vigenciaIni": "2025-12-31T23:59:59",
                "vigenciaFim": "2025-01-01T00:00:00"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(regraJson)
            .when()
            .post("/regras-conversao")
            .then()
            .statusCode(400);
    }
}
