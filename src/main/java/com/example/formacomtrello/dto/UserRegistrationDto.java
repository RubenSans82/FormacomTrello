package com.example.formacomtrello.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Considera añadir una validación personalizada para asegurar que password y confirmPassword coincidan.

/**
 * DTO para el registro de nuevos usuarios (específicamente GESTORES).
 * Contiene los campos necesarios del formulario de registro, incluyendo contraseña.
 */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class UserRegistrationDto {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no debe exceder los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios.")
    @Size(max = 150, message = "Los apellidos no deben exceder los 150 caracteres.")
    private String apellidos;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "Debe proporcionar un formato de email válido.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    // Puedes añadir más restricciones de contraseña si lo deseas (ej. @Pattern)
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres.")
    private String password;

    // Campo para confirmar la contraseña en el formulario.
    // La validación de coincidencia se haría en el servicio/controller o con una anotación custom.
    @NotBlank(message = "Debe confirmar la contraseña.")
    private String confirmPassword;

    // Teléfono opcional
    @Size(max = 20, message = "El teléfono no debe exceder los 20 caracteres.")
    private String telefono;

    // No incluimos rol aquí, se asume GESTOR al registrarse mediante este DTO.
    // No incluimos fotoUrl, id, firstLogin, etc.

    public UserRegistrationDto() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }


}
