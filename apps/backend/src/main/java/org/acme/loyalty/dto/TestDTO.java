package org.acme.loyalty.dto;

public class TestDTO {
    public Long id;
    public String nome;
    public String descricao;
    
    public TestDTO() {}
    
    public TestDTO(Long id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
    }
}
