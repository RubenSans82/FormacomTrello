package com.example.formacomtrello.service.impl;

import com.example.formacomtrello.model.*;
import com.example.formacomtrello.repository.TaskRepository;
import com.example.formacomtrello.repository.ProjectRepository;
import com.example.formacomtrello.repository.UserRepository;
import com.example.formacomtrello.repository.CommentRepository;
import com.example.formacomtrello.service.TaskService;
import com.example.formacomtrello.dto.TaskDto;
import com.example.formacomtrello.dto.CommentDto;
import com.example.formacomtrello.exception.ResourceNotFoundException;
import com.example.formacomtrello.exception.UnauthorizedAccessException;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository; // Para verificar proyecto y acceso

    @Autowired
    private UserRepository userRepository; // Para buscar gestor y colaborador

    @Autowired
    private CommentRepository commentRepository; // Para guardar comentarios

    // --- Helper Methods ---

    private User findUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + projectId));
    }

    private Task findTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada: " + taskId));
    }

    // Verifica si un usuario es el GESTOR de un proyecto
    private void verifyProjectManager(Project project, User manager) {
        if (!project.getOwner().equals(manager) || manager.getRole() != Role.GESTOR) {
            throw new UnauthorizedAccessException("Usuario " + manager.getEmail() + " no es el gestor del proyecto " + project.getId());
        }
        if (project.isClosed()) {
            throw new IllegalStateException("El proyecto " + project.getId() + " está cerrado.");
        }
    }

    // Verifica si un usuario tiene acceso general a un proyecto (Gestor o Colaborador)
    private void verifyProjectAccess(Project project, User user) {
        if (!project.getOwner().equals(user) && !project.getCollaborators().contains(user)) {
            throw new UnauthorizedAccessException("Usuario " + user.getEmail() + " no tiene acceso al proyecto " + project.getId());
        }
    }


    // --- Service Methods Implementation ---

    @Override
    @Transactional
    public Task createTask(Long projectId, TaskDto taskDto, String managerEmail) {
        User manager = findUserOrThrow(managerEmail);
        Project project = findProjectOrThrow(projectId);
        verifyProjectManager(project, manager); // Verifica que es el gestor y el proyecto no está cerrado

        User assignedUser = findUserOrThrow(taskDto.getAssignedUserEmail());
        if (assignedUser.getRole() != Role.COLABORADOR) {
            throw new IllegalArgumentException("Solo se pueden asignar tareas a COLABORADORES.");
        }
        // Verificar que el colaborador asignado pertenece a ESTE proyecto
        if (!project.getCollaborators().contains(assignedUser)) {
            throw new IllegalArgumentException("El usuario " + assignedUser.getEmail() + " no es colaborador de este proyecto.");
        }

        Task task = new Task();
        task.setProject(project);
        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setDueDate(taskDto.getDueDate());
        task.setAssignedUser(assignedUser);
        task.setStatus(TaskStatus.PENDIENTE); // Estado inicial por defecto
        task.setCreatedAt(LocalDateTime.now());

        return taskRepository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Task> findTaskById(Long taskId, String userEmail) {
        User user = findUserOrThrow(userEmail);
        Optional<Task> taskOpt = taskRepository.findById(taskId);

        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            // Verificar acceso al proyecto de la tarea
            try {
                verifyProjectAccess(task.getProject(), user);
                return taskOpt; // Tiene acceso
            } catch (UnauthorizedAccessException e) {
                return Optional.empty(); // No tiene acceso
            }
        }
        return Optional.empty(); // Tarea no encontrada
    }

    @Override
    @Transactional
    public Task updateTask(Long taskId, TaskDto taskDto, String managerEmail) {
        User manager = findUserOrThrow(managerEmail);
        Task task = findTaskOrThrow(taskId);
        Project project = task.getProject(); // Ya tenemos el proyecto desde la tarea
        verifyProjectManager(project, manager); // Verifica gestor y que proyecto no esté cerrado

        User newAssignedUser = findUserOrThrow(taskDto.getAssignedUserEmail());
        if (newAssignedUser.getRole() != Role.COLABORADOR) {
            throw new IllegalArgumentException("Solo se pueden asignar tareas a COLABORADORES.");
        }
        if (!project.getCollaborators().contains(newAssignedUser)) {
            throw new IllegalArgumentException("El usuario " + newAssignedUser.getEmail() + " no es colaborador de este proyecto.");
        }

        // Actualizar campos
        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setDueDate(taskDto.getDueDate());
        task.setAssignedUser(newAssignedUser);
        // El estado se cambia con changeTaskStatus, no aquí directamente por edición general
        // task.setStatus(taskDto.getStatus()); // Podría incluirse si el DTO lo trae y se permite

        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, String managerEmail) {
        User manager = findUserOrThrow(managerEmail);
        Task task = findTaskOrThrow(taskId);
        Project project = task.getProject();
        verifyProjectManager(project, manager); // Verifica gestor y que proyecto no esté cerrado

        taskRepository.deleteById(taskId);
    }

    @Override
    @Transactional
    public Task changeTaskStatus(Long taskId, TaskStatus newStatus, String userEmail) {
        User user = findUserOrThrow(userEmail);
        Task task = findTaskOrThrow(taskId);
        Project project = task.getProject();
        if (project.isClosed()){
            throw new IllegalStateException("El proyecto " + project.getId() + " está cerrado.");
        }

        // Quién puede cambiar el estado?
        // 1. El Gestor del proyecto puede cambiar a cualquier estado
        // 2. El Colaborador asignado SOLO puede cambiar a COMPLETADA

        boolean isManager = project.getOwner().equals(user) && user.getRole() == Role.GESTOR;
        boolean isAssignedCollaborator = task.getAssignedUser().equals(user) && user.getRole() == Role.COLABORADOR;

        if (isManager) {
            // El gestor puede poner cualquier estado
            task.setStatus(newStatus);
        } else if (isAssignedCollaborator) {
            // El colaborador asignado solo puede marcar como completada
            if (newStatus == TaskStatus.COMPLETADA) {
                task.setStatus(TaskStatus.COMPLETADA);
            } else {
                throw new UnauthorizedAccessException("Como colaborador asignado, solo puedes marcar la tarea como COMPLETADA.");
            }
        } else {
            // Ni gestor ni colaborador asignado
            throw new UnauthorizedAccessException("No tienes permisos para cambiar el estado de esta tarea.");
        }

        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Task markTaskAsCompleted(Long taskId, String collaboratorEmail) {
        User collaborator = findUserOrThrow(collaboratorEmail);
        if (collaborator.getRole() != Role.COLABORADOR) {
            throw new UnauthorizedAccessException("Solo colaboradores pueden completar tareas.");
        }
        Task task = findTaskOrThrow(taskId);
        if (task.getProject().isClosed()){
            throw new IllegalStateException("El proyecto de esta tarea está cerrado.");
        }

        if (!task.getAssignedUser().equals(collaborator)) {
            throw new UnauthorizedAccessException("No eres el usuario asignado para completar esta tarea.");
        }

        task.setStatus(TaskStatus.COMPLETADA);
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public Comment addCommentToTask(Long taskId, CommentDto commentDto, String userEmail) {
        User author = findUserOrThrow(userEmail);
        Task task = findTaskOrThrow(taskId);
        Project project = task.getProject();
        // Verificar que el usuario tiene acceso al proyecto (es gestor o colaborador)
        verifyProjectAccess(project, author);
        if (project.isClosed()){
            throw new IllegalStateException("No se puede comentar en tareas de un proyecto cerrado.");
        }

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setContent(commentDto.getContent());
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findTasksByProjectId(Long projectId, String userEmail) {
        User user = findUserOrThrow(userEmail);
        Project project = findProjectOrThrow(projectId);
        verifyProjectAccess(project, user); // Asegura que el usuario puede ver el proyecto

        return taskRepository.findByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findTasksByAssignedUser(String userEmail) {
        User user = findUserOrThrow(userEmail);
        // No se requiere verificación de proyecto aquí, trae todas las tareas asignadas al usuario
        return taskRepository.findByAssignedUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findCommentsByTaskId(Long taskId, String userEmail) {
        User user = findUserOrThrow(userEmail);
        Task task = findTaskOrThrow(taskId);
        Project project = task.getProject();
        // Verificar acceso al proyecto antes de devolver comentarios
        verifyProjectAccess(project, user);

        // El repositorio ya los devuelve ordenados por fecha ASC
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
    }
}