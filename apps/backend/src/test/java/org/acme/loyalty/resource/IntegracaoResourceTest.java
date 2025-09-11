package org.acme.loyalty.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

@QuarkusTest
class IntegracaoResourceTest {

    @Test
    @Order(1)
    void testFluxoCompletoPontuacao() {
        // 1. Criar usuário
        String usuarioJson = """
            {
                "nome": "Usuário Teste",
                "email": "usuario.teste@example.com"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(usuarioJson)
            .when()
            .post("/usuarios")
            .then()
            .statusCode(200)
            .body("data.nome", is("Usuário Teste"))
            .body("data.email", is("usuario.teste@example.com"))
            .body("data.id", notNullValue());

        // 2. Criar cartão
        String cartaoJson = """
            {
                "usuarioId": 1,
                "numero": "1234567890123456",
                "validade": "2026-12-31",
                "ativo": true
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(cartaoJson)
            .when()
            .post("/cartoes")
            .then()
            .statusCode(200)
            .body("data.usuarioId", is(1))
            .body("data.numero", is("1234567890123456"))
            .body("data.validade", is("2026-12-31"))
            .body("data.ativo", is(true))
            .body("data.id", notNullValue());

        // 3. Criar recompensa
        String recompensaJson = """
            {
                "tipo": "MILHAS",
                "descricao": "Recompensa de Teste",
                "custoPontos": 1000,
                "estoque": 10,
                "ativo": true
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(recompensaJson)
            .when()
            .post("/recompensas")
            .then()
            .statusCode(200)
            .body("data.tipo", is("MILHAS"))
            .body("data.descricao", is("Recompensa de Teste"))
            .body("data.custoPontos", is(1000))
            .body("data.estoque", is(10))
            .body("data.ativo", is(true))
            .body("data.id", notNullValue());

        // 4. Criar regra de conversão
        String regraJson = """
            {
                "nome": "Regra Teste",
                "multiplicador": 1.0,
                "vigenciaIni": "2025-01-01T00:00:00",
                "vigenciaFim": "2025-12-31T23:59:59",
                "prioridade": 1,
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
            .body("data.nome", is("Regra Teste"))
            .body("data.multiplicador", is(1.0f))
            .body("data.prioridade", is(1))
            .body("data.ativo", is(true))
            .body("data.id", notNullValue());

        // 5. Criar transação
        String transacaoJson = """
            {
                "usuarioId": 1,
                "cartaoId": 1,
                "valor": 100.00,
                "moeda": "BRL",
                "mcc": "5411",
                "categoria": "Supermercado",
                "dataEvento": "2025-09-09T10:00:00",
                "autorizacao": "AUTH123456"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(transacaoJson)
            .when()
            .post("/transacoes")
            .then()
            .statusCode(200)
            .body("data.usuarioId", is(1))
            .body("data.cartaoId", is(1))
            .body("data.valor", is(100.00f))
            .body("data.moeda", is("BRL"))
            .body("data.status", is("APROVADA"))
            .body("data.id", notNullValue());

        // 6. Verificar saldo de pontos
        given()
            .when()
            .get("/pontos/saldo/1")
            .then()
            .statusCode(200)
            .body("data.usuarioId", is(1))
            .body("data.saldoTotal", notNullValue());

        // 7. Solicitar resgate
        String resgateJson = """
            {
                "usuarioId": 1,
                "cartaoId": 1,
                "recompensaId": 1
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(resgateJson)
            .when()
            .post("/resgates")
            .then()
            .statusCode(200)
            .body("data.usuarioId", is(1))
            .body("data.cartaoId", is(1))
            .body("data.recompensaId", is(1))
            .body("data.status", is("PENDENTE"))
            .body("data.id", notNullValue());
    }

    @Test
    @Order(2)
    void testFluxoCompletoNotificacao() {
        // 1. Enviar notificação
        String notificacaoJson = """
            {
                "usuarioId": 1,
                "evento": "ACUMULO",
                "viaEmail": true,
                "viaSms": false,
                "viaPush": false,
                "email": "usuario.teste@example.com",
                "assunto": "Pontos Acumulados",
                "mensagem": "Você acumulou 100 pontos!"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(notificacaoJson)
            .when()
            .post("/notificacoes")
            .then()
            .statusCode(200)
            .body("data.usuarioId", is(1))
            .body("data.status", is("ENFILEIRADA"))
            .body("data.id", notNullValue());

        // 2. Listar notificações do usuário
        given()
            .queryParam("usuarioId", 1)
            .when()
            .get("/notificacoes")
            .then()
            .statusCode(200)
            .body("data", notNullValue());

        // 3. Consultar configuração de notificação
        given()
            .when()
            .get("/notificacoes/configuracao/1")
            .then()
            .statusCode(200)
            .body("data.usuarioId", is(1))
            .body("data.emailAtivo", is(true));
    }

    @Test
    @Order(3)
    void testFluxoCompletoRelatorios() {
        // 1. Gerar relatório de pontos acumulados
        given()
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .when()
            .get("/relatorios/pontos-acumulados")
            .then()
            .statusCode(200)
            .body("data.periodoInicio", notNullValue())
            .body("data.periodoFim", notNullValue());

        // 2. Gerar relatório de volume de transações
        given()
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .when()
            .get("/relatorios/volume-transacoes")
            .then()
            .statusCode(200)
            .body("data.periodoInicio", notNullValue())
            .body("data.periodoFim", notNullValue());

        // 3. Gerar relatório de status de resgates
        given()
            .queryParam("dataInicio", "2025-01-01")
            .queryParam("dataFim", "2025-12-31")
            .when()
            .get("/relatorios/status-resgates")
            .then()
            .statusCode(200)
            .body("data.periodoInicio", notNullValue())
            .body("data.periodoFim", notNullValue());
    }
}
