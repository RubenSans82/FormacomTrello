package com.example.formacomtrello.service;

import com.example.formacomtrello.dto.UserRegistrationDto;
import com.example.formacomtrello.model.User;

import java.util.Optional;

public interface UserService {
    User registerManager(UserRegistrationDto registrationDto);
    Optional<User> findByEmail(String email);
    User save(User user); // Método genérico para guardar/actualizar
    void setPasswordForCollaborator(String email, String newPassword);
    User updateProfile(Long userId, User updatedData); // Para que el usuario edite sus datos

    boolean isValidEmail(String email);

    // Método corregido para invitar colaboradores
    User inviteCollaborator(UserRegistrationDto userDto);
    
    // Método sobrecargado para invitar colaboradores (resuelve el error en ProjectServiceImpl)
    User inviteCollaborator(String email, String nombre, String apellidos, String password);

    // Otros métodos necesarios...
}
