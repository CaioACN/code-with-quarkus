package org.acme.loyalty.dto;

import java.util.List;

public class PageResponseDTO<T> {
    
    public List<T> content;
    public Long totalElements;
    public Integer totalPages;
    public Integer size;
    public Integer number;
    public Boolean first;
    public Boolean last;
    
    // Construtores
    public PageResponseDTO() {}
    
    public PageResponseDTO(List<T> content, Long totalElements, Integer size, Integer number) {
        this.content = content;
        this.totalElements = totalElements;
        this.size = size;
        this.number = number;
        this.totalPages = calculateTotalPages();
        this.first = (number == 0);
        this.last = (number >= totalPages - 1);
    }
    
    // Métodos de negócio
    private Integer calculateTotalPages() {
        if (size == null || size <= 0) return 0;
        if (totalElements == null || totalElements <= 0) return 0;
        
        return (int) Math.ceil((double) totalElements / size);
    }
    
    public static <T> PageResponseDTO<T> of(List<T> content, Long totalElements, Integer size, Integer number) {
        return new PageResponseDTO<>(content, totalElements, size, number);
    }
    
    public Boolean hasNext() {
        return !last;
    }
    
    public Boolean hasPrevious() {
        return !first;
    }
}