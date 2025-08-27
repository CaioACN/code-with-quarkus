package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "campanha_bonus", schema = "loyalty")
public class CampanhaBonus extends PanacheEntity {

    @NotBlank
    @Size(max = 120)
    @Column(name = "nome", nullable = false, length = 120)
    public String nome;

    @NotNull
    @DecimalMin("0.0000")                  // DDL permite default 0.0000
    @Digits(integer = 4, fraction = 4)     // numeric(8,4) => até 4 inteiros e 4 decimais
    @Column(name = "multiplicador_extra", nullable = false, precision = 8, scale = 4)
    public BigDecimal multiplicadorExtra = BigDecimal.ZERO; // evita NULL na inserção

    @NotNull
    @Column(name = "vigencia_ini", nullable = false)
    public LocalDate vigenciaIni;          // DATE no banco

    @Column(name = "vigencia_fim")
    public LocalDate vigenciaFim;          // DATE no banco (pode ser NULL)

    @Size(max = 60)
    @Column(name = "segmento", length = 60)
    public String segmento;                // opcional

    @NotNull
    @Min(0)
    @Column(name = "prioridade", nullable = false)
    public Integer prioridade = 0;         // default 0 no DDL

    @Column(name = "teto")
    public Long teto;                      // opcional

    public CampanhaBonus() {}

    public CampanhaBonus(String nome,
                         BigDecimal multiplicadorExtra,
                         LocalDate vigenciaIni,
                         LocalDate vigenciaFim,
                         String segmento,
                         Integer prioridade,
                         Long teto) {
        this.nome = nome;
        this.multiplicadorExtra = (multiplicadorExtra != null) ? multiplicadorExtra : BigDecimal.ZERO;
        this.vigenciaIni = vigenciaIni;
        this.vigenciaFim = vigenciaFim;
        this.segmento = segmento;
        this.prioridade = (prioridade != null) ? prioridade : 0;
        this.teto = teto;
    }

    // --- Regras auxiliares (sem campos inexistentes no DDL) ---

    /** Vigente na data atual conforme vigência. */
    public boolean estaVigente() {
        return estaVigenteEm(LocalDate.now());
    }

    public boolean estaVigenteEm(LocalDate data) {
        boolean iniciou = data.isEqual(vigenciaIni) || data.isAfter(vigenciaIni);
        boolean naoExpirou = (vigenciaFim == null) || data.isBefore(vigenciaFim) || data.isEqual(vigenciaFim);
        return iniciou && naoExpirou;
    }

    /** Se segmento não informado, aplica para todos. */
    public boolean aplicaParaSegmento(String segmentoUsuario) {
        if (segmento == null || segmento.isBlank()) return true;
        return segmento.equalsIgnoreCase(segmentoUsuario);
    }

    /** Multiplicador total = 1 + extra. */
    public BigDecimal getMultiplicadorTotal() {
        return BigDecimal.ONE.add(multiplicadorExtra);
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

    public String getStatusVigencia() {
        if (estaExpirada()) return "EXPIRADA";
        if (estaProximaExpiracao()) return "PROXIMA_EXPIRACAO";
        return estaVigente() ? "VIGENTE" : "AGUARDANDO_INICIO";
    }
}

