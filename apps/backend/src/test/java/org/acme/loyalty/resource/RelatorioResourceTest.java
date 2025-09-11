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
class RelatorioResourceTest {

    @Test
    @Order(1)
    void testGerarRelatorioPontosAcumulados() {
        given()
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .when()
            .get("/relatorios/pontos-acumulados")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.periodoInicio", notNullValue())
            .body("data.periodoFim", notNullValue())
            .body("data.granularidade", is("diario"));
    }

    @Test
    @Order(2)
    void testGerarRelatorioPontosExpirados() {
        given()
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .when()
            .get("/relatorios/pontos-expirados")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.periodoInicio", notNullValue())
            .body("data.periodoFim", notNullValue());
    }

    @Test
    @Order(3)
    void testGerarRelatorioVolumeTransacoes() {
        given()
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .queryParam("categoria", "Supermercado")
            .when()
            .get("/relatorios/volume-transacoes")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.periodoInicio", notNullValue())
            .body("data.periodoFim", notNullValue())
            .body("data.agrupamento", is("diario"));
    }

    @Test
    @Order(4)
    void testGerarRelatorioStatusResgates() {
        given()
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .queryParam("status", "PENDENTE")
            .when()
            .get("/relatorios/status-resgates")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.periodoInicio", notNullValue())
            .body("data.periodoFim", notNullValue());
    }

    @Test
    @Order(5)
    void testListarTiposRelatorio() {
        given()
            .when()
            .get("/relatorios/tipos")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data", hasSize(greaterThan(0)));
    }

    @Test
    @Order(6)
    void testListarFormatosExportacao() {
        given()
            .when()
            .get("/relatorios/formatos")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue())
            .body("data", hasSize(greaterThan(0)));
    }
}
