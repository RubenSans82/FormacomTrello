package com.example.formacomtrello.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentDto {

    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres")
    private String content;

    // Constructor vacío correcto
    public CommentDto() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
