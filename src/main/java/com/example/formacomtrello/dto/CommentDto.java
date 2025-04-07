package com.example.formacomtrello.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//@Data
//NoArgsConstructor
//@AllArgsConstructor
public class CommentDto {

    // No incluimos id, author, task, createdAt.

    @NotBlank(message = "El contenido del comentario no puede estar vacío.")
    @Size(min = 1, max = 1000, message = "El comentario debe tener entre 1 y 1000 caracteres.")
    private String content;

    public CommentDto() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}