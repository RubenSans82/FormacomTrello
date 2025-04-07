package com.example.formacomtrello.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat; // Para parsear la fecha del formulario

import jakarta.validation.constraints.*;
import java.time.LocalDate;

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class TaskDto {

    // No incluimos id, project, createdAt, status, comments.

    @NotBlank(message = "El título de la tarea no puede estar vacío.")
    @Size(min = 3, max = 150, message = "El título debe tener entre 3 y 150 caracteres.")
    private String title;

    @Size(max = 2000, message = "La descripción no puede exceder los 2000 caracteres.")
    private String description;

    @NotNull(message = "La fecha de vencimiento es obligatoria.")
    @FutureOrPresent(message = "La fecha de vencimiento no puede ser una fecha pasada.")
    // Especifica cómo esperar la fecha del input HTML (type="date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @NotBlank(message = "Debe asignar la tarea a un colaborador.")
    @Email(message = "El email del colaborador asignado no es válido.")
    private String assignedUserEmail; // Usamos el email para identificar al usuario en el formulario

    // Nota: El estado (status) no se incluye aquí generalmente,
    // ya que se establece por defecto al crear o se modifica
    // mediante acciones específicas (ej. /complete).
    // Si necesitaras establecerlo en el formulario, lo añadirías aquí
    // y probablemente sería un Enum `TaskStatus`.
    // private TaskStatus status;


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getAssignedUserEmail() {
        return assignedUserEmail;
    }

    public void setAssignedUserEmail(String assignedUserEmail) {
        this.assignedUserEmail = assignedUserEmail;
    }
}