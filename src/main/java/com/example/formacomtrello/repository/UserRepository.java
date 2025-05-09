package com.example.formacomtrello.repository;

import com.example.formacomtrello.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// Repositorio para la entidad User
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA generará automáticamente la query para buscar un usuario por su email
    Optional<User> findByEmail(String email);

    // Método para comprobar rápidamente si un email ya existe (más eficiente que findByEmail().isPresent())
    boolean existsByEmail(String email);
}