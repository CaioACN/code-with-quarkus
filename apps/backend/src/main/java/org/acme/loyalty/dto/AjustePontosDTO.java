package org.acme.loyalty.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.acme.loyalty.entity.Cartao;
import org.acme.loyalty.entity.MovimentoPontos;
import org.acme.loyalty.entity.MovimentoPontos.TipoMovimento;
import org.acme.loyalty.entity.Transacao;
import org.acme.loyalty.entity.Usuario;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "AjustePontos", description = "Payload para criar um movimento de AJUSTE de pontos (positivo = crédito, negativo = débito)")
public class AjustePontosDTO {

    @NotNull
    @Schema(description = "ID do usuário", example = "10", required = true)
    public Long usuarioId;

    @NotNull
    @Schema(description = "ID do cartão", example = "1", required = true)
    public Long cartaoId;

    @NotNull
    @Schema(description = "Quantidade de pontos a ajustar. Positivo credita, negativo debita", example = "150", required = true)
    public Integer pontos;

    @Size(max = 500)
    @Schema(description = "Motivo/observação do ajuste (audit trail)", example = "Ajuste manual por divergência de fatura")
    public String observacao;

    @Schema(description = "ID da transação relacionada (opcional)", example = "123")
    public Long transacaoId;

    @Schema(description = "Referência externa da transação (opcional)", example = "123")
    public Long refTransacaoId;

    @Schema(description = "Nome/código da regra aplicada (opcional, para auditoria)", example = "AJUSTE_MANUAL")
    public String regraAplicada;

    @Schema(description = "Campanha aplicada (opcional, para auditoria)", example = "Campanha Retenção Agosto")
    public String campanhaAplicada;

    @Schema(description = "Identificador do job/lote se o ajuste for em batch", example = "job-2025-08-27-001")
    public String jobId;

    // ---- Regras simples ----
    @AssertTrue(message = "pontos não pode ser zero")
    public boolean isPontosNaoZero() {
        return pontos != null && pontos != 0;
    }

    @Schema(hidden = true)
    public boolean isCredito() {
        return pontos != null && pontos > 0;
    }

    @Schema(hidden = true)
    public boolean isDebito() {
        return pontos != null && pontos < 0;
    }

    // ---- Mapeamento para entidade ----
    public MovimentoPontos toEntity(Usuario usuario, Cartao cartao, Transacao transacao) {
        MovimentoPontos mp = new MovimentoPontos();
        mp.usuario = usuario;
        mp.cartao = cartao;
        mp.tipo = TipoMovimento.AJUSTE;
        mp.pontos = this.pontos; // pode ser negativo (débito) ou positivo (crédito)
        mp.observacao = this.observacao;
        mp.jobId = this.jobId;
        mp.regraAplicada = this.regraAplicada;
        mp.campanhaAplicada = this.campanhaAplicada;
        mp.criadoEm = LocalDateTime.now(); // obrigatório na entidade

        if (transacao != null) {
            mp.transacao = transacao;
            mp.refTransacaoId = transacao.id;
        } else if (this.refTransacaoId != null) {
            mp.refTransacaoId = this.refTransacaoId;
        }

        return mp;
    }
}
