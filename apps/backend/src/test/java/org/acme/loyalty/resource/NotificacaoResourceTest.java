package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class NotificacaoResourceTest {

    @Test
    @Order(1)
    void testEnviarNotificacao() {
        String notificacaoJson = """
            {
                "usuarioId": 1,
                "evento": "ACUMULO",
                "viaEmail": true,
                "viaSms": false,
                "viaPush": false,
                "email": "teste@example.com",
                "assunto": "Teste de Notificação",
                "mensagem": "Esta é uma notificação de teste"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(notificacaoJson)
            .when()
            .post("/notificacoes/enviar")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("message", is("Notificação enviada com sucesso"));
    }

    @Test
    @Order(2)
    void testListarNotificacoes() {
        given()
            .when()
            .get("/notificacoes/usuario/1")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data", notNullValue());
    }

    @Test
    @Order(3)
    void testConsultarConfiguracaoNotificacao() {
        given()
            .when()
            .get("/notificacoes/configuracoes/usuario/1")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("data.usuarioId", is(1))
            .body("data.emailAtivo", is(true))
            .body("data.pushAtivo", is(true))
            .body("data.smsAtivo", is(false));
    }

    @Test
    @Order(4)
    void testEnviarNotificacaoComDadosInvalidos() {
        String notificacaoJson = """
            {
                "usuarioId": null,
                "evento": "INVALIDO",
                "viaEmail": false,
                "viaSms": false,
                "viaPush": false
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(notificacaoJson)
            .when()
            .post("/notificacoes/enviar")
            .then()
            .statusCode(400);
    }
}
