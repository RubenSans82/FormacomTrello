package com.example.formacomtrello.service.impl;
// Imports...
import com.example.formacomtrello.dto.UserRegistrationDto;
import com.example.formacomtrello.model.Role;
import com.example.formacomtrello.model.User;
import com.example.formacomtrello.repository.UserRepository;
import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User registerManager(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email ya registrado: " + dto.getEmail()); // Mejor excepción personalizada
        }
        User manager = new User();
        manager.setEmail(dto.getEmail());
        manager.setPassword(passwordEncoder.encode(dto.getPassword())); // Codificar contraseña
        manager.setNombre(dto.getNombre());
        manager.setApellidos(dto.getApellidos());
        manager.setTelefono(dto.getTelefono());
        manager.setRole(Role.GESTOR);
        manager.setFotoUrl(dto.getFotoUrl()); // Si se incluye
        return userRepository.save(manager);
    }

    @Override
    @Transactional
    public User inviteCollaborator(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email ya registrado: " + dto.getEmail());
        }
        
        User collaborator = new User();
        collaborator.setEmail(dto.getEmail());
        // Asegurar que la contraseña se codifique correctamente
        collaborator.setPassword(passwordEncoder.encode(dto.getPassword()));
        collaborator.setNombre(dto.getNombre());
        collaborator.setApellidos(dto.getApellidos());
        collaborator.setTelefono(dto.getTelefono());
        collaborator.setRole(Role.COLABORADOR);

        return userRepository.save(collaborator);
    }

    @Override
    @Transactional
    public User inviteCollaborator(String email, String nombre, String apellidos, String password) {
        // Crear un nuevo UserRegistrationDto con los datos proporcionados
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail(email);
        dto.setNombre(nombre);
        dto.setApellidos(apellidos);
        dto.setPassword(password);
        dto.setConfirmPassword(password); // Asumimos que la contraseña ya está validada
        
        // Llamar al método existente que usa el DTO
        return inviteCollaborator(dto);
    }

    // Eliminamos el método obsoleto que causaba el error
    // No se necesita este método ya que existe la versión que usa UserRegistrationDto

    @Override
    @Transactional
    public void setPasswordForCollaborator(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Eliminar esta condición:
        // if (user.getRole() != Role.COLABORADOR || !user.isFirstLogin()) {
        //     throw new RuntimeException("Acción no permitida para este usuario");
        // }

        user.setPassword(passwordEncoder.encode(newPassword));
        // Eliminar esta línea:
        // user.setFirstLogin(false);
        userRepository.save(user);
    }

    // Implementar findByEmail, save, updateProfile...
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, User updatedData) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")); // ResourceNotFoundException

        // Actualizar solo los campos permitidos (nombre, apellidos, telefono, foto?)
        existingUser.setNombre(updatedData.getNombre());
        existingUser.setApellidos(updatedData.getApellidos());
        existingUser.setTelefono(updatedData.getTelefono());
        existingUser.setFotoUrl(updatedData.getFotoUrl());
        // No permitir cambiar email o rol desde aquí (quizás contraseña en otra función)

        return userRepository.save(existingUser);
    }

    @Override
    public boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
