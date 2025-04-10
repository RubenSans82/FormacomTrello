package com.example.formacomtrello.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

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
    
    // Archivo de foto subido
    private MultipartFile fotoFile;
    
    // Contraseña actual (necesaria para cambiar la contraseña)
    private String currentPassword;
    
    // Contraseña nueva - Quitamos la validación de tamaño mínimo aquí
    // para permitir valores vacíos cuando no se desea cambiar la contraseña
    private String password;
    
    // Confirmación de la nueva contraseña
    private String confirmPassword;

    public UserProfileDto() {
    }

    public UserProfileDto(String nombre, String apellidos, String telefono, String fotoUrl) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.fotoUrl = fotoUrl;
        this.password = null;
        this.currentPassword = null;
        this.confirmPassword = null;
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
    
    public MultipartFile getFotoFile() {
        return fotoFile;
    }
    
    public void setFotoFile(MultipartFile fotoFile) {
        this.fotoFile = fotoFile;
    }
    
    public String getCurrentPassword() {
        return currentPassword;
    }
    
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
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
}
