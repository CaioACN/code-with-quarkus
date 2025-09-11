package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
class AdminResourceTest {

    @Test
    void testConsultarDashboard() {
        given()
          .when().get("/admin/dashboard")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             .body("data.totalUsuarios", notNullValue())
             .body("data.totalTransacoes", notNullValue())
             .body("data.saldoTotal", notNullValue())
             .body("data.resgatesPendentes", notNullValue());
    }

    @Test
    void testConsultarSaudeSistema() {
        given()
          .when().get("/admin/sistema/health")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             .body("data.statusGeral", notNullValue())
             .body("data.timestamp", notNullValue());
    }

    @Test
    void testConsultarMetricas() {
        given()
          .when().get("/admin/sistema/metricas")
          .then()
             .statusCode(200)
             .contentType(ContentType.JSON)
             .body("message", notNullValue())
             .body("status", notNullValue());
    }
}
