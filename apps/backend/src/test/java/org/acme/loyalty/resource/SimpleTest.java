package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class SimpleTest {

    @Test
    void testHealthEndpoint() {
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.status", is("UP"));
    }

    @Test
    void testRecompensasEndpoint() {
        given()
            .when()
            .get("/recompensas")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testNotificacoesEndpoint() {
        given()
            .when()
            .get("/notificacoes/usuario/1")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }
}
