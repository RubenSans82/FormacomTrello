package com.example.formacomtrello.service.impl;

import com.example.formacomtrello.model.*;
import com.example.formacomtrello.repository.ProjectRepository;
import com.example.formacomtrello.repository.UserRepository;
import com.example.formacomtrello.service.ProjectService;
import com.example.formacomtrello.service.UserService; // Para invitar/crear colaboradores
import com.example.formacomtrello.dto.ProjectDto;
import com.example.formacomtrello.exception.ResourceNotFoundException;
import com.example.formacomtrello.exception.UnauthorizedAccessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService; // Inyectamos UserService

    @Override
    @Transactional
    public Project createProject(ProjectDto projectDto, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + ownerEmail));

        if (owner.getRole() != Role.GESTOR) {
            throw new UnauthorizedAccessException("Solo los GESTORES pueden crear proyectos.");
        }

        Project project = new Project();
        project.setTitle(projectDto.getTitle());
        project.setDescription(projectDto.getDescription());
        project.setOwner(owner);
        project.setCreatedAt(LocalDateTime.now());
        project.setClosed(false);
        // Inicialmente sin colaboradores ni tareas

        return projectRepository.save(project);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findProjectById(Long projectId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + userEmail));

        Optional<Project> projectOpt = projectRepository.findById(projectId);

        if (projectOpt.isPresent()) {
            Project project = projectOpt.get();
            // Verificar si es propietario o colaborador
            if (project.getOwner().equals(user) || project.getCollaborators().contains(user)) {
                return projectOpt;
            } else {
                // El usuario existe pero no tiene acceso a este proyecto
                return Optional.empty(); // O lanzar UnauthorizedAccessException si prefieres
            }
        }
        return Optional.empty(); // Proyecto no encontrado
    }

    // Helper method para verificación de propietario
    private Project findProjectAndVerifyOwnership(Long projectId, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario GESTOR no encontrado con email: " + ownerEmail));
        if (owner.getRole() != Role.GESTOR) {
            throw new UnauthorizedAccessException("La acción requiere rol GESTOR.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));

        if (!project.getOwner().equals(owner)) {
            throw new UnauthorizedAccessException("El usuario " + ownerEmail + " no es el propietario del proyecto " + projectId);
        }
        return project;
    }


    @Override
    @Transactional
    public Project updateProject(Long projectId, ProjectDto projectDto, String ownerEmail) {
        Project project = findProjectAndVerifyOwnership(projectId, ownerEmail);

        if (project.isClosed()){
            throw new IllegalStateException("No se puede editar un proyecto cerrado.");
        }

        project.setTitle(projectDto.getTitle());
        project.setDescription(projectDto.getDescription());
        // No se permite cambiar el propietario, fecha de creación o estado 'closed' aquí

        return projectRepository.save(project);
    }

    @Override
    @Transactional
    public void closeProject(Long projectId, String ownerEmail) {
        Project project = findProjectAndVerifyOwnership(projectId, ownerEmail);
        project.setClosed(true);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    public User addOrInviteCollaborator(Long projectId, String collaboratorEmail, User collaboratorDetails, String managerEmail) {
        Project project = findProjectAndVerifyOwnership(projectId, managerEmail);

        if (project.isClosed()){
            throw new IllegalStateException("No se puede añadir colaboradores a un proyecto cerrado.");
        }

        Optional<User> existingUserOpt = userService.findByEmail(collaboratorEmail);
        User collaborator;

        if (existingUserOpt.isPresent()) {
            collaborator = existingUserOpt.get();
            if (collaborator.getRole() == Role.GESTOR) {
                throw new IllegalArgumentException("No se puede añadir un GESTOR como colaborador.");
            }
            // Verificar si ya es colaborador de este proyecto
            if (project.getCollaborators().contains(collaborator)){
                // Ya es colaborador, no hacemos nada o podrías devolverlo directamente
                System.out.println("Usuario " + collaboratorEmail + " ya es colaborador del proyecto " + projectId);
                return collaborator; // O lanzar excepción si prefieres un error en este caso
            }
        } else {
            // El usuario no existe, lo creamos (invitamos)
            if (collaboratorDetails == null || collaboratorDetails.getNombre() == null || collaboratorDetails.getApellidos() == null){
                throw new IllegalArgumentException("Se requieren nombre y apellidos para invitar a un nuevo colaborador.");
            }
            // Usamos el servicio de usuario para crear el colaborador (sin contraseña, firstLogin=true)
            collaborator = userService.inviteCollaborator(
                    collaboratorEmail,
                    collaboratorDetails.getNombre(),
                    collaboratorDetails.getApellidos(),
                    collaboratorDetails.getTelefono()// Puede ser null
            );
        }

        // Añadir el colaborador al set del proyecto
        project.getCollaborators().add(collaborator);
        // Es buena práctica también actualizar la relación inversa si es necesario (aunque aquí es ManyToMany y User no la guarda así directamente)
        // collaborator.getProjectsCollaborating().add(project); // Ojo si la relación es bidireccional gestionada

        projectRepository.save(project); // Guardamos el proyecto con el nuevo colaborador
        return collaborator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findProjectsByOwner(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + ownerEmail));
        return projectRepository.findByOwnerId(owner.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findProjectsByCollaborator(String collaboratorEmail) {
        User collaborator = userRepository.findByEmail(collaboratorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + collaboratorEmail));
        return projectRepository.findByCollaboratorsContains(collaborator);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findUserAccessibleProjects(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + userEmail));

        // Combina ambas listas sin duplicados
        List<Project> owned = projectRepository.findByOwnerId(user.getId());
        List<Project> collaborating = projectRepository.findByCollaboratorsContains(user);

        // Usamos un Stream para combinar y eliminar duplicados por ID
        return Stream.concat(owned.stream(), collaborating.stream())
                .distinct() // Asegura que no haya proyectos duplicados si alguien es owner y colaborador (aunque no debería pasar por diseño)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessProject(Long projectId, String userEmail) {
        Optional<User> userOpt = userRepository.findByEmail(userEmail);
        if (!userOpt.isPresent()) return false;
        User user = userOpt.get();

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (!projectOpt.isPresent()) return false;
        Project project = projectOpt.get();

        return project.getOwner().equals(user) || project.getCollaborators().contains(user);
    }

    @Override
    @Transactional
    public boolean inviteCollaborator(Long projectId, String email) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con ID: " + projectId));
            
            if (project.isClosed()) {
                throw new IllegalStateException("No se puede añadir colaboradores a un proyecto cerrado.");
            }
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));
            
            if (user.getRole() == Role.GESTOR) {
                throw new IllegalArgumentException("No se puede añadir un GESTOR como colaborador.");
            }
            
            // Verificar si ya es colaborador de este proyecto
            if (project.getCollaborators().contains(user)) {
                return true; // Ya es colaborador, consideramos exitosa la operación
            }
            
            // Añadir el colaborador al proyecto
            project.getCollaborators().add(user);
            projectRepository.save(project);
            
            return true;
        } catch (Exception e) {
            System.err.println("Error al invitar colaborador: " + e.getMessage());
            return false;
        }
    }
}
