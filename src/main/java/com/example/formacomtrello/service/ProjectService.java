package com.example.formacomtrello.service;

import com.example.formacomtrello.model.Project;
import com.example.formacomtrello.model.User; // Asumiendo que quieres devolver/trabajar con entidades a veces
import com.example.formacomtrello.dto.ProjectDto; // Asumiendo DTO para creación/edición

import java.util.List;
import java.util.Optional;

public interface ProjectService {

    /**
     * Crea un nuevo proyecto.
     * @param projectDto DTO con la información del proyecto.
     * @param ownerEmail Email del usuario GESTOR que crea el proyecto.
     * @return El proyecto creado.
     * @throws RuntimeException si el usuario no es GESTOR o no se encuentra.
     */
    Project createProject(ProjectDto projectDto, String ownerEmail);

    /**
     * Encuentra un proyecto por su ID, verificando el acceso del usuario.
     * @param projectId ID del proyecto.
     * @param userEmail Email del usuario que solicita (GESTOR o COLABORADOR).
     * @return Optional con el proyecto si se encuentra y el usuario tiene acceso, Optional vacío en caso contrario.
     */
    Optional<Project> findProjectById(Long projectId, String userEmail);

    /**
     * Actualiza un proyecto existente. Solo el propietario puede editar.
     * @param projectId ID del proyecto a actualizar.
     * @param projectDto DTO con los nuevos datos.
     * @param ownerEmail Email del usuario GESTOR que intenta actualizar.
     * @return El proyecto actualizado.
     * @throws ResourceNotFoundException si el proyecto no existe.
     * @throws UnauthorizedAccessException si el usuario no es el propietario.
     */
    Project updateProject(Long projectId, ProjectDto projectDto, String ownerEmail);

    /**
     * Marca un proyecto como cerrado. Solo el propietario puede cerrarlo.
     * @param projectId ID del proyecto a cerrar.
     * @param ownerEmail Email del usuario GESTOR.
     * @throws ResourceNotFoundException si el proyecto no existe.
     * @throws UnauthorizedAccessException si el usuario no es el propietario.
     */
    void closeProject(Long projectId, String ownerEmail);

    /**
     * Añade un colaborador existente a un proyecto. Crea el colaborador si no existe.
     * @param projectId ID del proyecto.
     * @param collaboratorEmail Email del colaborador a añadir/invitar.
     * @param managerEmail Email del GESTOR propietario del proyecto.
     * @param collaboratorDetails Detalles del colaborador si necesita ser creado (nombre, apellidos, etc.).
     * @return El usuario colaborador añadido o creado.
     * @throws ResourceNotFoundException si el proyecto no existe.
     * @throws UnauthorizedAccessException si el managerEmail no es el propietario.
     * @throws RuntimeException si el usuario a invitar ya existe pero es GESTOR.
     */
    User addOrInviteCollaborator(Long projectId, String collaboratorEmail, User collaboratorDetails, String managerEmail);


    /**
     * Obtiene todos los proyectos propiedad de un GESTOR.
     * @param ownerEmail Email del GESTOR.
     * @return Lista de proyectos.
     */
    List<Project> findProjectsByOwner(String ownerEmail);

    /**
     * Obtiene todos los proyectos en los que un usuario es COLABORADOR.
     * @param collaboratorEmail Email del COLABORADOR.
     * @return Lista de proyectos.
     */
    List<Project> findProjectsByCollaborator(String collaboratorEmail);

    /**
     * Obtiene todos los proyectos asociados a un usuario (propietario o colaborador).
     * @param userEmail Email del usuario.
     * @return Lista de proyectos.
     */
    List<Project> findUserAccessibleProjects(String userEmail);

    /**
     * Verifica si un usuario tiene acceso a un proyecto (es propietario o colaborador).
     * @param projectId ID del proyecto.
     * @param userEmail Email del usuario.
     * @return true si tiene acceso, false en caso contrario.
     */
    boolean canUserAccessProject(Long projectId, String userEmail);

    boolean inviteCollaborator(Long projectId, String email);
}