package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "campanha_bonus", schema = "loyalty")
@Check(constraints = "multiplicador_extra >= 0 AND prioridade >= 0 AND (teto IS NULL OR teto > 0)")
public class CampanhaBonus extends PanacheEntity {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
    @Column(name = "nome", nullable = false, length = 120)
    public String nome;

    @NotNull(message = "Multiplicador extra é obrigatório")
    @DecimalMin(value = "0.0000", inclusive = true, message = "Multiplicador extra deve ser maior ou igual a 0.0000")
    @DecimalMax(value = "9999.9999", inclusive = true, message = "Multiplicador extra deve ser menor ou igual a 9999.9999")
    @Digits(integer = 4, fraction = 4, message = "Multiplicador extra deve ter no máximo 4 dígitos inteiros e 4 decimais")
    @Column(name = "multiplicador_extra", nullable = false, precision = 8, scale = 4)
    public BigDecimal multiplicadorExtra = BigDecimal.ZERO;

    @NotNull(message = "Data de início da vigência é obrigatória")
    @Column(name = "vigencia_ini", nullable = false)
    public LocalDate vigenciaIni;

    @Column(name = "vigencia_fim")
    public LocalDate vigenciaFim;

    @Size(max = 60, message = "Segmento deve ter no máximo 60 caracteres")
    @Column(name = "segmento", length = 60)
    public String segmento;

    @NotNull(message = "Prioridade é obrigatória")
    @Min(value = 0, message = "Prioridade deve ser maior ou igual a 0")
    @Column(name = "prioridade", nullable = false)
    public Integer prioridade = 0;

    @AssertTrue(message = "Teto deve ser nulo ou maior que zero")
    public boolean isTetoValido() {
        return teto == null || teto > 0;
    }
    
    @Column(name = "teto")
    public Long teto; // opcional (NULL ou > 0)

    public CampanhaBonus() {}

    public CampanhaBonus(String nome,
                         BigDecimal multiplicadorExtra,
                         LocalDate vigenciaIni,
                         LocalDate vigenciaFim,
                         String segmento,
                         Integer prioridade,
                         Long teto) {
        this.nome = nome;
        this.multiplicadorExtra = (multiplicadorExtra != null)
                ? multiplicadorExtra
                : BigDecimal.ZERO;
        this.vigenciaIni = vigenciaIni;
        this.vigenciaFim = vigenciaFim;
        this.segmento = segmento;
        this.prioridade = (prioridade != null) ? prioridade : 0;
        this.teto = teto;
    }

    // ---- Validações/normalizações de persistência ----
    @AssertTrue(message = "Data de fim deve ser posterior ou igual à data de início")
    public boolean isPeriodoValido() {
        if (vigenciaFim == null || vigenciaIni == null) return true;
        return !vigenciaFim.isBefore(vigenciaIni);
    }

    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (nome != null) nome = nome.trim();
        if (segmento != null) segmento = segmento.trim();

        // Aplicar valores padrão conforme DDL
        if (multiplicadorExtra == null) {
            multiplicadorExtra = BigDecimal.ZERO;
        } else {
            // Garantir escala correta (4 decimais)
            multiplicadorExtra = multiplicadorExtra.setScale(4, java.math.RoundingMode.HALF_UP);
        }

        if (prioridade == null || prioridade < 0) {
            prioridade = 0;
        }

        // Teto: NULL ou > 0 (não forçar nada, deixar a validação @AssertTrue cuidar)
    }

    // ---- Regras auxiliares / domínio ----
    public enum StatusVigencia { AGUARDANDO_INICIO, VIGENTE, PROXIMA_EXPIRACAO, EXPIRADA }

    /** Vigente na data atual conforme vigência. */
    public boolean estaVigente() {
        return estaVigenteEm(LocalDate.now());
    }

    public boolean estaVigenteEm(LocalDate data) {
        if (vigenciaIni == null) return false;
        boolean iniciou = !data.isBefore(vigenciaIni);
        boolean naoExpirou = (vigenciaFim == null) || !data.isAfter(vigenciaFim);
        return iniciou && naoExpirou;
    }

    /** Se segmento não informado, aplica para todos. */
    public boolean aplicaParaSegmento(String segmentoUsuario) {
        if (segmento == null || segmento.isBlank()) return true;
        return segmento.equalsIgnoreCase(segmentoUsuario);
    }

    /** Multiplicador total: 1 + extra. */
    public BigDecimal getMultiplicadorTotal() {
        return BigDecimal.ONE.add(multiplicadorExtra);
    }

    /** multiplicador_extra ≥ 0 */
    public boolean temMultiplicadorExtraValido() {
        return multiplicadorExtra != null && multiplicadorExtra.compareTo(BigDecimal.ZERO) >= 0;
    }

    /** pontos_totais = floor(pontos_base * (1 + multiplicador_extra)) */
    public Long calcularPontosComBonus(Long pontosBase) {
        if (pontosBase == null || pontosBase <= 0) return 0L;
        BigDecimal pontosTotais = BigDecimal.valueOf(pontosBase).multiply(getMultiplicadorTotal());
        return pontosTotais.longValue(); // trunc -> floor para positivos
    }

    public boolean temTeto() {
        return teto != null && teto > 0;
    }

    public boolean estaExpirada() {
        return vigenciaFim != null && LocalDate.now().isAfter(vigenciaFim);
    }

    public boolean estaProximaExpiracao() {
        if (vigenciaFim == null) return false;
        LocalDate hoje = LocalDate.now();
        LocalDate limite = vigenciaFim.minusDays(7);
        return (hoje.isAfter(limite) || hoje.isEqual(limite)) && !estaExpirada();
    }

    public StatusVigencia getStatusVigencia() {
        if (estaExpirada()) return StatusVigencia.EXPIRADA;
        if (estaProximaExpiracao()) return StatusVigencia.PROXIMA_EXPIRACAO;
        return estaVigente() ? StatusVigencia.VIGENTE : StatusVigencia.AGUARDANDO_INICIO;
    }
}
