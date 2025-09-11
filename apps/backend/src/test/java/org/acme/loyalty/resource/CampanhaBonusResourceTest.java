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
class CampanhaBonusResourceTest {

    @Test
    @Order(1)
    void testCriarCampanha() {
        String campanhaJson = """
            {
                "nome": "Campanha Teste",
                "descricao": "Campanha de teste para validação",
                "multiplicadorExtra": 0.5,
                "vigenciaIni": "2025-01-01",
                "vigenciaFim": "2025-12-31",
                "segmento": "PREMIUM",
                "prioridade": 1
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(campanhaJson)
            .when()
            .post("/campanhas")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("data.nome", is("Campanha Teste"))
            .body("data.descricao", is("Campanha de teste para validação"))
            .body("data.multiplicadorExtra", is(0.5f))
            .body("data.segmento", is("PREMIUM"))
            .body("data.prioridade", is(1))
            .body("data.id", notNullValue());
    }

    @Test
    @Order(2)
    void testListarCampanhas() {
        given()
            .when()
            .get("/campanhas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data", hasSize(greaterThan(0)));
    }

    @Test
    @Order(3)
    void testListarCampanhasVigentes() {
        given()
            .when()
            .get("/campanhas/vigentes")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(4)
    void testCriarCampanhaComDadosInvalidos() {
        String campanhaJson = """
            {
                "nome": "",
                "descricao": "",
                "multiplicadorExtra": -1.0,
                "vigenciaIni": "2025-12-31",
                "vigenciaFim": "2025-01-01",
                "prioridade": -1
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(campanhaJson)
            .when()
            .post("/campanhas")
            .then()
            .statusCode(400);
    }
}
