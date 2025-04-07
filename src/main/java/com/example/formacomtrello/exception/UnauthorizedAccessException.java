package com.example.formacomtrello.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción personalizada para indicar que el usuario actual, aunque autenticado,
 * no tiene los permisos necesarios para realizar la acción solicitada sobre un recurso.
 * Extiende RuntimeException para ser una excepción no comprobada (unchecked).
 * La anotación @ResponseStatus(HttpStatus.FORBIDDEN) ayuda a que Spring MVC
 * devuelva automáticamente un código de estado HTTP 403 Forbidden si esta excepción
 * no es manejada específicamente.
 */
@ResponseStatus(HttpStatus.FORBIDDEN) // Devuelve un 403 Forbidden automáticamente si no se maneja
public class UnauthorizedAccessException extends RuntimeException {

    /**
     * Constructor que acepta un mensaje descriptivo del error de autorización.
     *
     * @param message El mensaje detallando por qué el acceso fue denegado.
     */
    public UnauthorizedAccessException(String message) {
        super(message);
    }

    /**
     * Constructor que acepta un mensaje y la causa original del error.
     *
     * @param message El mensaje descriptivo.
     * @param cause   La excepción original que causó este error.
     */
    public UnauthorizedAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}