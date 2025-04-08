package com.example.formacomtrello.controller;

import com.example.formacomtrello.dto.CommentDto;
import com.example.formacomtrello.dto.TaskDto;
import com.example.formacomtrello.exception.ResourceNotFoundException;
import com.example.formacomtrello.exception.UnauthorizedAccessException;
import com.example.formacomtrello.model.*;
import com.example.formacomtrello.service.ProjectService;
import com.example.formacomtrello.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
// Nota: Algunas rutas empiezan con /projects/{projectId}/tasks, otras con /tasks/{taskId}
// No ponemos un @RequestMapping base aquí para manejar ambas estructuras.
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectService projectService; // Necesario para obtener colaboradores, verificar acceso a proyecto

    // --- Vista Detallada de Tarea ---
    @GetMapping("/tasks/{taskId}")
    @PreAuthorize("isAuthenticated()") // Acceso básico, el servicio verifica el acceso específico al proyecto
    public String viewTaskDetails(@PathVariable Long taskId, Model model, Authentication authentication) {
        String userEmail = authentication.getName();
        try {
            // findTaskById ya verifica si el usuario (gestor o colaborador) tiene acceso al proyecto de la tarea
            Task task = taskService.findTaskById(taskId, userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada o sin acceso: " + taskId));

            List<Comment> comments = taskService.findCommentsByTaskId(taskId, userEmail); // El servicio también verifica acceso

            // Determinar permisos para la vista
            User currentUser = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername().equals(userEmail) ?
                    task.getProject().getOwner() : // Placeholder, necesitamos buscar el usuario real
                    taskService.findTaskById(taskId, userEmail).get().getAssignedUser(); // Simplificado, buscar usuario real por email


            boolean canComment = projectService.canUserAccessProject(task.getProject().getId(), userEmail); // Gestor o Colaborador del proyecto
            boolean canComplete = userEmail.equals(task.getAssignedUser().getEmail())
                    && task.getStatus() != TaskStatus.COMPLETADA
                    && !task.getProject().isClosed();
            boolean canEditDelete = userEmail.equals(task.getProject().getOwner().getEmail())
                    && !task.getProject().isClosed();


            model.addAttribute("task", task);
            model.addAttribute("comments", comments);
            model.addAttribute("canComment", canComment);
            model.addAttribute("canComplete", canComplete);
            model.addAttribute("canEditDelete", canEditDelete); // Para mostrar botones editar/eliminar
            model.addAttribute("newComment", new CommentDto()); // Para el formulario de comentarios

            return "tasks/task-detail";

        } catch (ResourceNotFoundException | UnauthorizedAccessException e) {
            // Redirigir a una página de error o a la lista de proyectos si no se encuentra o no hay acceso
            model.addAttribute("errorMessage", e.getMessage());
            return "error"; // O redirigir: "redirect:/error/404";
        }
    }


    // --- Crear Tarea (Formulario y Proceso) ---

    @GetMapping("/projects/{projectId}/tasks/new")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String showCreateTaskForm(@PathVariable Long projectId, Model model, Authentication authentication) {
        String managerEmail = authentication.getName();
        try {
            // Verificar que el usuario es el GESTOR de ESTE proyecto y que no esté cerrado
            Project project = projectService.findProjectById(projectId, managerEmail)
                    .filter(p -> p.getOwner().getEmail().equals(managerEmail)) // Doble check de propiedad
                    .orElseThrow(() -> new UnauthorizedAccessException("No eres el gestor de este proyecto o no existe."));

            if (project.isClosed()) {
                // No mostrar formulario si proyecto está cerrado
                model.addAttribute("errorMessage", "No se pueden añadir tareas a un proyecto cerrado.");
                return "error"; // O redirigir a detalles del proyecto con mensaje flash
            }

            TaskDto taskDto = new TaskDto();
            // Añadir colaboradores para el dropdown de asignación
            model.addAttribute("taskDto", taskDto);
            model.addAttribute("projectId", projectId);
            model.addAttribute("collaborators", project.getCollaborators()); // Pasar colaboradores del proyecto
            model.addAttribute("isEditMode", false);

            return "manager/task-form"; // Vista de formulario (puede ser la misma para crear/editar)

        } catch (ResourceNotFoundException | UnauthorizedAccessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String createTask(@PathVariable Long projectId,
                             @Valid @ModelAttribute("taskDto") TaskDto taskDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes,
                             Model model) { // Model para recargar datos si hay error de validación

        String managerEmail = authentication.getName();

        // Re-verificar acceso y estado del proyecto antes de procesar
        Project project;
        try {
            project = projectService.findProjectById(projectId, managerEmail)
                    .filter(p -> p.getOwner().getEmail().equals(managerEmail))
                    .orElseThrow(() -> new UnauthorizedAccessException("Acceso denegado al proyecto."));
            if (project.isClosed()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No se pueden añadir tareas a un proyecto cerrado.");
                return "redirect:/projects/" + projectId;
            }
        } catch (ResourceNotFoundException | UnauthorizedAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al crear tarea: " + e.getMessage());
            return "redirect:/manager/dashboard"; // O a donde sea apropiado
        }


        if (bindingResult.hasErrors()) {
            // Si hay errores de validación, volver al formulario
            model.addAttribute("projectId", projectId);
            model.addAttribute("collaborators", project.getCollaborators()); // Recargar colaboradores
            model.addAttribute("isEditMode", false);
            return "manager/task-form";
        }

        try {
            taskService.createTask(projectId, taskDto, managerEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Tarea creada exitosamente.");
            return "redirect:/projects/" + projectId; // Redirigir a los detalles del proyecto

        } catch (ResourceNotFoundException | UnauthorizedAccessException | IllegalArgumentException e) {
            // Si el servicio lanza error (e.g., colaborador no encontrado, no pertenece al proyecto)
            model.addAttribute("errorMessage", "Error al crear tarea: " + e.getMessage());
            model.addAttribute("projectId", projectId);
            model.addAttribute("collaborators", project.getCollaborators()); // Recargar
            model.addAttribute("isEditMode", false);
            model.addAttribute("taskDto", taskDto); // Devolver DTO con datos previos
            return "manager/task-form"; // Volver al formulario mostrando el error
        } catch (IllegalStateException e) { // Capturar error de proyecto cerrado
            redirectAttributes.addFlashAttribute("errorMessage", "Error al crear tarea: " + e.getMessage());
            return "redirect:/projects/" + projectId;
        }
    }


    // --- Editar Tarea (Formulario y Proceso) ---

    @GetMapping("/tasks/{taskId}/edit")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String showEditTaskForm(@PathVariable Long taskId, Model model, Authentication authentication) {
        String managerEmail = authentication.getName();
        try {
            Task task = taskService.findTaskById(taskId, managerEmail) // Servicio ya valida acceso general
                    .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada o sin acceso: " + taskId));

            // Verificar que el usuario es GESTOR del proyecto específico de la tarea
            Project project = task.getProject();
            if (!project.getOwner().getEmail().equals(managerEmail)) {
                throw new UnauthorizedAccessException("No eres el gestor del proyecto de esta tarea.");
            }
            if (project.isClosed()) {
                model.addAttribute("errorMessage", "No se pueden editar tareas de un proyecto cerrado.");
                return "error";
            }


            // Crear DTO a partir de la entidad para pre-rellenar el formulario
            TaskDto taskDto = new TaskDto();
            taskDto.setTitle(task.getTitle());
            taskDto.setDescription(task.getDescription());
            taskDto.setDueDate(task.getDueDate());
            taskDto.setAssignedUserEmail(task.getAssignedUser().getEmail());
            // taskDto.setStatus(task.getStatus()); // El estado se cambia por separado, no en edición general

            model.addAttribute("taskDto", taskDto);
            model.addAttribute("taskId", taskId);
            model.addAttribute("projectId", project.getId()); // Puede ser útil en la vista
            model.addAttribute("collaborators", project.getCollaborators());
            model.addAttribute("isEditMode", true);

            return "manager/task-form";

        } catch (ResourceNotFoundException | UnauthorizedAccessException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/tasks/{taskId}/edit")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String updateTask(@PathVariable Long taskId,
                             @Valid @ModelAttribute("taskDto") TaskDto taskDto,
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes,
                             Model model) {

        String managerEmail = authentication.getName();
        Project project = null; // Para recargar colaboradores si falla la validación

        // --- Re-validar Acceso y Estado antes de procesar ---
        try {
            Task existingTask = taskService.findTaskById(taskId, managerEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada o sin acceso."));
            project = existingTask.getProject();
            if (!project.getOwner().getEmail().equals(managerEmail)) {
                throw new UnauthorizedAccessException("No eres el gestor de este proyecto.");
            }
            if (project.isClosed()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No se puede editar tareas de un proyecto cerrado.");
                return "redirect:/tasks/" + taskId;
            }
        } catch (ResourceNotFoundException | UnauthorizedAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al editar tarea: " + e.getMessage());
            return "redirect:/manager/dashboard"; // O a donde sea apropiado
        }
        // --- Fin Re-validación ---


        if (bindingResult.hasErrors()) {
            model.addAttribute("taskId", taskId);
            model.addAttribute("projectId", project.getId());
            model.addAttribute("collaborators", project.getCollaborators()); // Recargar
            model.addAttribute("isEditMode", true);
            return "manager/task-form";
        }

        try {
            taskService.updateTask(taskId, taskDto, managerEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Tarea actualizada exitosamente.");
            return "redirect:/tasks/" + taskId; // Volver a los detalles de la tarea

        } catch (ResourceNotFoundException | UnauthorizedAccessException | IllegalArgumentException e) {
            model.addAttribute("errorMessage", "Error al actualizar tarea: " + e.getMessage());
            model.addAttribute("taskId", taskId);
            model.addAttribute("projectId", project.getId());
            model.addAttribute("collaborators", project.getCollaborators()); // Recargar
            model.addAttribute("isEditMode", true);
            model.addAttribute("taskDto", taskDto); // Devolver datos
            return "manager/task-form";
        } catch (IllegalStateException e) { // Capturar error de proyecto cerrado
            redirectAttributes.addFlashAttribute("errorMessage", "Error al editar tarea: " + e.getMessage());
            return "redirect:/tasks/" + taskId;
        }
    }


    // --- Eliminar Tarea ---
    @PostMapping("/tasks/{taskId}/delete")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String deleteTask(@PathVariable Long taskId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        String managerEmail = authentication.getName();
        Long projectId = null;

        try {
            // Primero obtenemos la tarea para saber a qué proyecto redirigir y verificar propiedad
            Task task = taskService.findTaskById(taskId, managerEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada o sin acceso."));

            // Verificar que es el GESTOR de ese proyecto y que no está cerrado
            Project project = task.getProject();
            projectId = project.getId(); // Guardamos el ID para la redirección
            if (!project.getOwner().getEmail().equals(managerEmail)) {
                throw new UnauthorizedAccessException("No eres el gestor de este proyecto.");
            }
            if (project.isClosed()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No se puede eliminar tareas de un proyecto cerrado.");
                return "redirect:/tasks/" + taskId;
            }

            // Ahora sí, eliminamos
            taskService.deleteTask(taskId, managerEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Tarea eliminada exitosamente.");
            return "redirect:/projects/" + projectId; // Redirigir al proyecto

        } catch (ResourceNotFoundException | UnauthorizedAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar tarea: " + e.getMessage());
            // Si projectId se obtuvo, redirigir al proyecto, si no, al dashboard
            return projectId != null ? "redirect:/projects/" + projectId : "redirect:/manager/dashboard";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar tarea: " + e.getMessage());
            return "redirect:/tasks/" + taskId;
        }
    }


    // --- Marcar Tarea como Completada (Colaborador) ---
    @PostMapping("/tasks/{taskId}/complete")
    @PreAuthorize("hasAuthority('COLABORADOR')")
    public String markTaskAsCompleted(@PathVariable Long taskId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        String collaboratorEmail = authentication.getName();

        try {
            taskService.markTaskAsCompleted(taskId, collaboratorEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Tarea marcada como completada.");
        } catch (ResourceNotFoundException | UnauthorizedAccessException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "No se pudo completar la tarea: " + e.getMessage());
        }
        return "redirect:/tasks/" + taskId; // Siempre redirigir de vuelta a la tarea
    }


    // --- Añadir Comentario ---
    @PostMapping("/tasks/{taskId}/comment")
    @PreAuthorize("isAuthenticated()") // Acceso verificado en el servicio
    public String addComment(@PathVariable Long taskId,
                             @Valid @ModelAttribute("newComment") CommentDto commentDto, // Usar un DTO para el comentario
                             BindingResult bindingResult,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {

        String userEmail = authentication.getName();

        // Si hay errores de validación, redirigir con mensaje de error
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError", "El contenido del comentario no puede estar vacío.");
            return "redirect:/tasks/" + taskId;
        }

        try {
            // Validamos manualmente que el contenido no esté vacío (doble verificación)
            if (commentDto.getContent() == null || commentDto.getContent().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("commentError", "El contenido del comentario no puede estar vacío.");
                return "redirect:/tasks/" + taskId;
            }
            
            // Intentamos añadir el comentario
            taskService.addCommentToTask(taskId, commentDto, userEmail);
            redirectAttributes.addFlashAttribute("successMessage", "Comentario añadido exitosamente.");
            
        } catch (ResourceNotFoundException e) {
            // La tarea o el usuario no existen
            redirectAttributes.addFlashAttribute("errorMessage", "No se encontró la tarea: " + e.getMessage());
            return "redirect:/collaborator/dashboard"; // Redirección a un lugar seguro
            
        } catch (UnauthorizedAccessException e) {
            // El usuario no tiene acceso al proyecto
            redirectAttributes.addFlashAttribute("errorMessage", "No tienes permiso para comentar en esta tarea: " + e.getMessage());
            return "redirect:/collaborator/dashboard"; // Redirección a un lugar seguro
            
        } catch (IllegalStateException e) {
            // Proyecto cerrado u otro problema de estado
            redirectAttributes.addFlashAttribute("errorMessage", "No se puede añadir el comentario: " + e.getMessage());
            
        } catch (Exception e) {
            // Cualquier otra excepción inesperada
            redirectAttributes.addFlashAttribute("errorMessage", "Error inesperado al añadir comentario: " + e.getMessage());
        }

        // Siempre intentamos volver a la página de la tarea
        return "redirect:/tasks/" + taskId;
    }
}
