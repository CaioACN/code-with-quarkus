package org.acme.loyalty.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "flyway_schema_history", schema = "loyalty")
public class FlywaySchemaHistory extends PanacheEntityBase {
    
    @Id
    @NotNull(message = "Installed rank é obrigatório")
    @Column(name = "installed_rank", nullable = false)
    public Integer installedRank;
    
    @Size(max = 50, message = "Version deve ter no máximo 50 caracteres")
    @Column(name = "version", length = 50)
    public String version;
    
    @NotBlank(message = "Description é obrigatória")
    @Size(max = 200, message = "Description deve ter no máximo 200 caracteres")
    @Column(name = "description", nullable = false, length = 200)
    public String description;
    
    @NotBlank(message = "Type é obrigatório")
    @Size(max = 20, message = "Type deve ter no máximo 20 caracteres")
    @Column(name = "type", nullable = false, length = 20)
    public String type;
    
    @NotBlank(message = "Script é obrigatório")
    @Size(max = 1000, message = "Script deve ter no máximo 1000 caracteres")
    @Column(name = "script", nullable = false, length = 1000)
    public String script;
    
    @Column(name = "checksum")
    public Integer checksum;
    
    @NotBlank(message = "Installed by é obrigatório")
    @Size(max = 100, message = "Installed by deve ter no máximo 100 caracteres")
    @Column(name = "installed_by", nullable = false, length = 100)
    public String installedBy;
    
    @NotNull(message = "Installed on é obrigatório")
    @Column(name = "installed_on", nullable = false)
    public LocalDateTime installedOn;
    
    @NotNull(message = "Execution time é obrigatório")
    @Column(name = "execution_time", nullable = false)
    public Integer executionTime;
    
    @NotNull(message = "Success é obrigatório")
    @Column(name = "success", nullable = false)
    public Boolean success;
    
    // Construtores
    public FlywaySchemaHistory() {}
    
    public FlywaySchemaHistory(Integer installedRank, String version, String description, 
                              String type, String script, Integer checksum, String installedBy, 
                              LocalDateTime installedOn, Integer executionTime, Boolean success) {
        this.installedRank = installedRank;
        this.version = version;
        this.description = description;
        this.type = type;
        this.script = script;
        this.checksum = checksum;
        this.installedBy = installedBy;
        this.installedOn = installedOn;
        this.executionTime = executionTime;
        this.success = success;
    }
    
    // ---- Normalização de dados ----
    @PrePersist
    @PreUpdate
    protected void normalize() {
        // Normalizar strings
        if (version != null) version = version.trim();
        if (description != null) description = description.trim();
        if (type != null) type = type.trim();
        if (script != null) script = script.trim();
        if (installedBy != null) installedBy = installedBy.trim();
        
        // Definir installedOn se não foi definido
        if (installedOn == null) {
            installedOn = LocalDateTime.now();
        }
        
        // Definir success se não foi definido
        if (success == null) {
            success = false;
        }
    }
    
    // Métodos de negócio
    /**
     * Verifica se a migração foi bem-sucedida
     */
    public boolean foiBemSucedida() {
        return success != null && success;
    }
    
    /**
     * Verifica se a migração falhou
     */
    public boolean falhou() {
        return success != null && !success;
    }
    
    /**
     * Verifica se é uma migração de versão
     */
    public boolean isMigracaoVersao() {
        return "SQL".equals(type);
    }
    
    /**
     * Verifica se é uma migração de baseline
     */
    public boolean isBaseline() {
        return "BASELINE".equals(type);
    }
    
    /**
     * Verifica se é uma migração de repeatable
     */
    public boolean isRepeatable() {
        return "REPEATABLE".equals(type);
    }
    
    /**
     * Verifica se o installed rank é válido
     */
    public boolean temInstalledRankValido() {
        return installedRank != null && installedRank > 0;
    }
    
    /**
     * Verifica se o execution time é válido
     */
    public boolean temExecutionTimeValido() {
        return executionTime != null && executionTime >= 0;
    }
    
    /**
     * Retorna o status da migração
     */
    public String getStatus() {
        if (success == null) return "UNKNOWN";
        return success ? "SUCCESS" : "FAILED";
    }
    
    /**
     * Retorna informações resumidas da migração
     */
    public String getResumo() {
        return String.format("Rank: %d, Version: %s, Type: %s, Status: %s", 
                           installedRank, version, type, getStatus());
    }
}
