package com.example.formacomtrello.model;

import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDIENTE("Pendiente"),
    EN_PROGRESO("En Progreso"),
    COMPLETADA("Completada");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

}