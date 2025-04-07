package com.example.formacomtrello.service;

import com.example.formacomtrello.model.Task;
import com.example.formacomtrello.model.Comment;
import com.example.formacomtrello.model.TaskStatus;
import com.example.formacomtrello.dto.TaskDto;     // Para crear/editar tareas
import com.example.formacomtrello.dto.CommentDto; // Para añadir comentarios

import java.util.List;
import java.util.Optional;

public interface TaskService {

    /**
     * Crea una nueva tarea dentro de un proyecto. Solo el GESTOR del proyecto puede crearla.
     * @param projectId ID del proyecto al que pertenece la tarea.
     * @param taskDto DTO con los datos de la tarea (título, desc, fecha, email_colaborador_asignado).
     * @param managerEmail Email del GESTOR que crea la tarea.
     * @return La tarea creada.
     * @throws ResourceNotFoundException si el proyecto o el colaborador asignado no existen.
     * @throws UnauthorizedAccessException si el usuario no es GESTOR de ese proyecto.
     * @throws IllegalArgumentException si el usuario asignado no es colaborador del proyecto.
     */
    Task createTask(Long projectId, TaskDto taskDto, String managerEmail);

    /**
     * Encuentra una tarea por su ID, verificando que el usuario tenga acceso al proyecto.
     * @param taskId ID de la tarea.
     * @param userEmail Email del usuario (GESTOR o COLABORADOR del proyecto).
     * @return Optional con la tarea si se encuentra y el usuario tiene acceso, Optional vacío si no.
     */
    Optional<Task> findTaskById(Long taskId, String userEmail);

    /**
     * Actualiza una tarea existente. Solo el GESTOR del proyecto puede editarla.
     * @param taskId ID de la tarea a actualizar.
     * @param taskDto DTO con los nuevos datos.
     * @param managerEmail Email del GESTOR.
     * @return La tarea actualizada.
     * @throws ResourceNotFoundException si la tarea, proyecto o nuevo asignado no existen.
     * @throws UnauthorizedAccessException si el usuario no es GESTOR de ese proyecto.
     * @throws IllegalArgumentException si el nuevo usuario asignado no es colaborador del proyecto.
     */
    Task updateTask(Long taskId, TaskDto taskDto, String managerEmail);

    /**
     * Elimina una tarea. Solo el GESTOR del proyecto puede eliminarla.
     * @param taskId ID de la tarea a eliminar.
     * @param managerEmail Email del GESTOR.
     * @throws ResourceNotFoundException si la tarea no existe.
     * @throws UnauthorizedAccessException si el usuario no es GESTOR de ese proyecto.
     */
    void deleteTask(Long taskId, String managerEmail);

    /**
     * Cambia el estado de una tarea. El GESTOR puede cambiar a cualquier estado.
     * El COLABORADOR asignado solo puede marcarla como COMPLETADA.
     * @param taskId ID de la tarea.
     * @param newStatus Nuevo estado de la tarea.
     * @param userEmail Email del usuario que realiza la acción.
     * @return La tarea actualizada.
     * @throws ResourceNotFoundException si la tarea no existe.
     * @throws UnauthorizedAccessException si el usuario no tiene permisos (no es gestor ni colaborador asignado para completar).
     * @throws IllegalArgumentException si el colaborador intenta cambiar a un estado diferente de COMPLETADA.
     */
    Task changeTaskStatus(Long taskId, TaskStatus newStatus, String userEmail);

    /**
     * Método específico para que el colaborador asignado marque la tarea como completada.
     * @param taskId ID de la tarea.
     * @param collaboratorEmail Email del colaborador asignado.
     * @return La tarea actualizada.
     * @throws ResourceNotFoundException si la tarea no existe.
     * @throws UnauthorizedAccessException si el usuario no es el colaborador asignado a la tarea.
     */
    Task markTaskAsCompleted(Long taskId, String collaboratorEmail);

    /**
     * Añade un comentario a una tarea. Solo GESTOR o COLABORADOR del proyecto pueden comentar.
     * @param taskId ID de la tarea.
     * @param commentDto DTO con el contenido del comentario.
     * @param userEmail Email del usuario que comenta.
     * @return El comentario creado.
     * @throws ResourceNotFoundException si la tarea no existe.
     * @throws UnauthorizedAccessException si el usuario no tiene acceso al proyecto.
     */
    Comment addCommentToTask(Long taskId, CommentDto commentDto, String userEmail);

    /**
     * Obtiene todas las tareas de un proyecto específico. Verifica acceso del usuario.
     * @param projectId ID del proyecto.
     * @param userEmail Email del usuario.
     * @return Lista de tareas del proyecto.
     * @throws UnauthorizedAccessException si el usuario no tiene acceso al proyecto.
     */
    List<Task> findTasksByProjectId(Long projectId, String userEmail);

    /**
     * Obtiene todas las tareas asignadas a un usuario específico en todos sus proyectos.
     * @param userEmail Email del usuario (COLABORADOR).
     * @return Lista de tareas asignadas.
     */
    List<Task> findTasksByAssignedUser(String userEmail);

    /**
     * Obtiene los comentarios de una tarea específica. Verifica acceso del usuario.
     * @param taskId ID de la tarea.
     * @param userEmail Email del usuario.
     * @return Lista de comentarios ordenados por fecha.
     * @throws UnauthorizedAccessException si el usuario no tiene acceso al proyecto de la tarea.
     */
    List<Comment> findCommentsByTaskId(Long taskId, String userEmail);
}