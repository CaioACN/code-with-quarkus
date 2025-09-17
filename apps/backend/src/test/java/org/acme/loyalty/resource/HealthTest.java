package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class HealthTest {

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
    void testHealthReady() {
        given()
            .when()
            .get("/health/ready")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testHealthLive() {
        given()
            .when()
            .get("/health/live")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }
}
