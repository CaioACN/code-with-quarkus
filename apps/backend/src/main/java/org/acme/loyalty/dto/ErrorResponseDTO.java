package org.acme.loyalty.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponseDTO {
    
    public String message;
    public String error;
    public Integer status;
    public LocalDateTime timestamp;
    public String path;
    public List<String> details;
    
    // Construtores
    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponseDTO(String message, String error, Integer status) {
        this();
        this.message = message;
        this.error = error;
        this.status = status;
    }
    
    public ErrorResponseDTO(String message, String error, Integer status, String path) {
        this(message, error, status);
        this.path = path;
    }
    
    public ErrorResponseDTO(String message, String error, Integer status, String path, List<String> details) {
        this(message, error, status, path);
        this.details = details;
    }
    
    // Métodos estáticos para erros comuns
    public static ErrorResponseDTO badRequest(String message) {
        return new ErrorResponseDTO(message, "Bad Request", 400);
    }
    
    public static ErrorResponseDTO notFound(String message) {
        return new ErrorResponseDTO(message, "Not Found", 404);
    }
    
    public static ErrorResponseDTO internalError(String message) {
        return new ErrorResponseDTO(message, "Internal Server Error", 500);
    }
    
    public static ErrorResponseDTO validationError(String message, List<String> details) {
        return new ErrorResponseDTO(message, "Validation Error", 400, null, details);
    }
    
    public static ErrorResponseDTO unauthorized(String message) {
        return new ErrorResponseDTO(message, "Unauthorized", 401);
    }
    
    public static ErrorResponseDTO forbidden(String message) {
        return new ErrorResponseDTO(message, "Forbidden", 403);
    }
    
    public static ErrorResponseDTO conflict(String message) {
        return new ErrorResponseDTO(message, "Conflict", 409);
    }
    
    // Métodos de negócio
    public void addDetail(String detail) {
        if (details == null) {
            details = new java.util.ArrayList<>();
        }
        details.add(detail);
    }
    
    public Boolean hasDetails() {
        return details != null && !details.isEmpty();
    }
    
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.toString() : "N/A";
    }
}

