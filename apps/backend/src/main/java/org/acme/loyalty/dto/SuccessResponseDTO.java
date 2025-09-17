package org.acme.loyalty.dto;

import java.time.LocalDateTime;

public class SuccessResponseDTO<T> {
    
    public String message;
    public T data;
    public Integer status;
    public LocalDateTime timestamp;
    public String path;
    
    // Construtores
    public SuccessResponseDTO() {
        this.timestamp = LocalDateTime.now();
        this.status = 200;
    }
    
    public SuccessResponseDTO(String message, T data) {
        this();
        this.message = message;
        this.data = data;
    }
    
    public SuccessResponseDTO(String message, T data, Integer status) {
        this(message, data);
        this.status = status;
    }
    
    public SuccessResponseDTO(String message, T data, Integer status, String path) {
        this(message, data, status);
        this.path = path;
    }
    
    // Métodos estáticos para respostas comuns
    public static <T> SuccessResponseDTO<T> created(T data) {
        return new SuccessResponseDTO<>("Recurso criado com sucesso", data, 201);
    }
    
    public static <T> SuccessResponseDTO<T> ok(T data) {
        return new SuccessResponseDTO<>("Operação realizada com sucesso", data, 200);
    }
    
    public static <T> SuccessResponseDTO<T> ok(String message, T data) {
        return new SuccessResponseDTO<>(message, data, 200);
    }
    
    public static <T> SuccessResponseDTO<T> updated(T data) {
        return new SuccessResponseDTO<>("Recurso atualizado com sucesso", data, 200);
    }
    
    public static <T> SuccessResponseDTO<T> deleted() {
        return new SuccessResponseDTO<>("Recurso removido com sucesso", null, 200);
    }
    
    public static <T> SuccessResponseDTO<T> processed(T data) {
        return new SuccessResponseDTO<>("Processamento realizado com sucesso", data, 200);
    }
    
    // Métodos de negócio
    public Boolean hasData() {
        return data != null;
    }
    
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.toString() : "N/A";
    }
    
    public String getStatusText() {
        if (status == 200) {
            return "OK";
        } else if (status == 201) {
            return "Created";
        } else if (status == 202) {
            return "Accepted";
        } else if (status == 204) {
            return "No Content";
        } else {
            return "Status: " + status;
        }
    }
}

