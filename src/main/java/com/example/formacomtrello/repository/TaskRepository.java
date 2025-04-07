package com.example.formacomtrello.repository;

import com.example.formacomtrello.model.Task;
import com.example.formacomtrello.model.Project; // Import no estrictamente necesario aquí, pero sí en el modelo Task
import com.example.formacomtrello.model.User; // Import no estrictamente necesario aquí, pero sí en el modelo Task
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Repositorio para la entidad Task
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Busca todas las tareas pertenecientes a un ID de proyecto específico
    List<Task> findByProjectId(Long projectId);

    // Busca todas las tareas asignadas a un ID de usuario específico
    List<Task> findByAssignedUserId(Long userId);

    // Busca todas las tareas de un proyecto específico asignadas a un usuario específico
    List<Task> findByProjectIdAndAssignedUserId(Long projectId, Long userId);

    // Podrías añadir métodos para buscar por estado, fecha de vencimiento, etc.
    // List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
}