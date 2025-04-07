package com.example.formacomtrello.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//@Data // Genera getters, setters, toString, equals, hashCode
//@NoArgsConstructor // Constructor sin argumentos
//@AllArgsConstructor // Constructor con todos los argumentos
public class ProjectDto {

    // No incluimos el ID aquí porque se maneja por separado (en la URL para editar, no existe al crear)
    // Tampoco owner, collaborators, tasks, createdAt, closed. Esos se manejan en la lógica de servicio.

    @NotBlank(message = "El título del proyecto no puede estar vacío.")
    @Size(min = 3, max = 100, message = "El título debe tener entre 3 y 100 caracteres.")
    private String title;

    // La descripción puede ser opcional o tener un tamaño máximo si se desea.
    @Size(max = 1000, message = "La descripción no puede exceder los 1000 caracteres.")
    private String description;

    public ProjectDto() {
    }

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
}