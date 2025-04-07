package com.example.formacomtrello.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Puedes añadir @URL para fotoUrl si quieres validar el formato de URL.

/**
 * DTO para la actualización del perfil de un usuario existente.
 * Contiene solo los campos que el usuario puede modificar desde su página de perfil.
 * NO incluye email, rol ni contraseña.
 */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class UserProfileDto {

    @NotBlank(message = "El nombre no puede estar vacío.")
    @Size(max = 100, message = "El nombre no debe exceder los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Los apellidos no pueden estar vacíos.")
    @Size(max = 150, message = "Los apellidos no deben exceder los 150 caracteres.")
    private String apellidos;

    // Teléfono podría ser opcional
    @Size(max = 20, message = "El teléfono no debe exceder los 20 caracteres.")
    private String telefono;

    // URL de la foto, podría ser opcional
    @Size(max = 512, message = "La URL de la foto no debe exceder los 512 caracteres.")
    private String fotoUrl;

    // NO incluimos id, email, password, role, firstLogin


    public UserProfileDto() {
    }

    public UserProfileDto(String nombre, String apellidos, String telefono, String fotoUrl) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.fotoUrl = fotoUrl;
    }


    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
}