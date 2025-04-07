package com.example.formacomtrello.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción personalizada para indicar que un recurso solicitado no fue encontrado.
 * Extiende RuntimeException para ser una excepción no comprobada (unchecked).
 * La anotación @ResponseStatus(HttpStatus.NOT_FOUND) ayuda a que Spring MVC
 * devuelva automáticamente un código de estado HTTP 404 si esta excepción
 * llega hasta el DispatcherServlet sin ser manejada por un @ExceptionHandler.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // Devuelve un 404 Not Found automáticamente si no se maneja
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructor que acepta un mensaje descriptivo del error.
     *
     * @param message El mensaje detallando qué recurso no se encontró.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor que acepta un mensaje y la causa original del error.
     *
     * @param message El mensaje descriptivo.
     * @param cause   La excepción original que causó este error.
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // Puedes añadir constructores adicionales si necesitas pasar más contexto,
    // como el tipo de recurso y el ID:
    /*
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s : '%s'", resourceName, fieldName, fieldValue));
    }
    */
}