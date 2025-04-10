package com.example.formacomtrello.controller;

import com.example.formacomtrello.model.Project;
import com.example.formacomtrello.model.Task;
import com.example.formacomtrello.model.TaskStatus;
import com.example.formacomtrello.model.User;
import com.example.formacomtrello.service.ProjectService;
import com.example.formacomtrello.service.TaskService;
import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;
import java.util.List;

@Controller
@RequestMapping("/")
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    /**
     * Muestra el dashboard del gestor
     */
    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String managerDashboard(Model model, Authentication auth) {
        String email = auth.getName();
        Optional<User> user = userService.findByEmail(email);

        // Aquí puedes añadir más datos al modelo como proyectos recientes, etc.
        // model.addAttribute("projects", projectService.findProjectsByOwnerEmail(email));
        user.ifPresent(value -> model.addAttribute("userName", value.getNombre()));

        return "manager/dashboard";
    }

    /**
     * Muestra el dashboard del colaborador
     */
    @GetMapping("/collaborator/dashboard")
    @PreAuthorize("hasAuthority('COLABORADOR')")
    public String collaboratorDashboard(Model model, Authentication auth) {
        String email = auth.getName();
        Optional<User> user = userService.findByEmail(email);

        // Aquí puedes añadir datos relevantes para colaboradores
        // model.addAttribute("assignedTasks", taskService.findTasksByAssignedUserEmail(email));
        user.ifPresent(value -> model.addAttribute("userName", value.getNombre()));

        return "collaborator/dashboard";
    }

    /**
     * Redirige a la lista de proyectos del gestor
     */
    @GetMapping("/manager/project-list")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String managerProjectList(Model model, Authentication auth) {
        String email = auth.getName();
        List<Project> projects = projectService.findProjectsByOwner(email);
        model.addAttribute("projects", projects);
        return "manager/project-list";
    }

    //Redirige a la vista de tareas del colaborador
    @GetMapping("/collaborator/tasks")
    @PreAuthorize("hasAuthority('COLABORADOR')")
    public String collaboratorTasks(Model model, Authentication auth) {
        String userEmail = auth.getName();
        List<Task> tasks = taskService.findTasksByAssignedUser(userEmail);
        tasks.forEach(task -> task.setStatus(TaskStatus.valueOf(task.getStatus().name())));
        model.addAttribute("tasks", tasks);
        return "collaborator/task-list";
    }


}
