package com.example.formacomtrello.repository;

import com.example.formacomtrello.model.Project;
import com.example.formacomtrello.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Repositorio para la entidad Project
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Busca proyectos donde el ID del propietario coincide
    List<Project> findByOwnerId(Long ownerId);

    // Busca proyectos donde la colección 'collaborators' contiene al usuario especificado
    List<Project> findByCollaboratorsContains(User collaborator);

    // Busca proyectos donde el usuario es el propietario O es un colaborador
    // Spring Data JPA entiende la convención 'Or' en el nombre del método
    List<Project> findByOwnerIdOrCollaboratorsContains(Long ownerId, User collaborator);

    // Podrías añadir más métodos específicos si los necesitas, por ejemplo:
    // List<Project> findByOwnerAndClosed(User owner, boolean closed);
}