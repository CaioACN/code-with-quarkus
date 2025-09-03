package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.acme.loyalty.entity.Resgate;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO para atualização de um Resgate (transições de status e metadados).
 * Compatível com Java 17 / Quarkus 3 / IDE Cursor.
 *
 * Observações:
 * - Campos são opcionais para suportar atualizações parciais (PATCH-like).
 * - Se "status" for informado, serão aplicadas as transições usando os métodos
 *   da entidade (aprovar/concluir/negar/cancelar) e timestamps serão definidos.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ResgateUpdate", description = "Dados para atualizar um resgate (status e metadados)")
public class ResgateUpdateDTO {

    @Schema(
        description = "Novo status do resgate",
        enumeration = {"PENDENTE", "APROVADO", "CONCLUIDO", "NEGADO", "CANCELADO"}
    )
    public Resgate.StatusResgate status;

    @Size(max = 500)
    @Schema(description = "Observação interna do resgate (comentários, anotações)", maxLength = 500)
    public String observacao;

    @Size(max = 100)
    @Schema(description = "Motivo da negação (obrigatório quando status=NEGADO)", maxLength = 100)
    public String motivoNegacao;

    @Size(max = 120)
    @Schema(description = "Código de rastreio/logística (se aplicável)", maxLength = 120, example = "BR123456789XY")
    public String codigoRastreio;

    @Size(max = 120)
    @Schema(description = "Identificador/nome do parceiro processador (se aplicável)", maxLength = 120, example = "ParceiroXPTO")
    public String parceiroProcessador;

    // ---------- Validações de consistência entre campos ----------

    @AssertTrue(message = "motivoNegacao é obrigatório quando status=NEGADO")
    public boolean isMotivoNegacaoQuandoNegadoValido() {
        if (status == Resgate.StatusResgate.NEGADO) {
            return motivoNegacao != null && !motivoNegacao.isBlank();
        }
        return true;
    }

    // (Opcional) Exemplo de regra: não exigir nada quando for apenas metadado.
    // Adicione outras regras de negócio aqui se necessário.

    // ---------- Aplicação no agregado (entidade) ----------

    /**
     * Aplica os campos deste DTO à entidade Resgate.
     * <p>
     * Importante:
     * - Este método não realiza verificação de permissões/roles.
     * - Se necessário, valide transições de estado no serviço antes de aplicar.
     * - Os relacionamentos LAZY não são tocados aqui.
     */
    public void applyTo(Resgate r) {
        if (r == null) return;

        // Metadados
        if (observacao != null) {
            r.observacao = observacao;
        }
        if (codigoRastreio != null) {
            r.codigoRastreio = codigoRastreio;
        }
        if (parceiroProcessador != null) {
            r.parceiroProcessador = parceiroProcessador;
        }

        // Transição de status (se informada)
        if (status != null && status != r.status) {
            switch (status) {
                case PENDENTE -> r.status = Resgate.StatusResgate.PENDENTE;
                case APROVADO -> {
                    // limpa motivo de negação, se houver
                    r.motivoNegacao = null;
                    r.aprovar(); // define status=APROVADO e aprovadoEm=now
                }
                case CONCLUIDO -> r.concluir(); // define status=CONCLUIDO e concluidoEm=now
                case NEGADO -> {
                    r.negar(motivoNegacao); // define status=NEGADO e negadoEm=now
                }
                case CANCELADO -> r.cancelar(); // status=CANCELADO
            }
        }

        // Se desejado, ajuste timestamps quando apenas metadados mudarem:
        // (Ex.: nenhuma regra adicional aqui; timestamps principais são definidos na entidade)
        // r.atualizadoEm = LocalDateTime.now();  // Caso a entidade possua esse campo
        // A entidade Resgate não possui "atualizado_em", então omitimos.
    }

    // ---------- Helpers opcionais ----------

    @Schema(hidden = true)
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
