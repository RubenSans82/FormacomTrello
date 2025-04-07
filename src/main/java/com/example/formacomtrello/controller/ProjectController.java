package com.example.formacomtrello.controller;

import com.example.formacomtrello.dto.ProjectDto;
import com.example.formacomtrello.model.Project;
import com.example.formacomtrello.model.Task;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // Para control fino
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.formacomtrello.service.ProjectService;
import com.example.formacomtrello.service.UserService;
import com.example.formacomtrello.service.TaskService;

import java.util.List;
import java.util.Optional;
// ... otros imports

@Controller
@RequestMapping("/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserService userService;
    @Autowired
    private TaskService taskService;

    // --- Rutas de Gestor ---

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String showCreateProjectForm(Model model) {
        // Añadir ProjectDto vacío y el flag isEditMode
        model.addAttribute("projectDto", new ProjectDto());
        model.addAttribute("isEditMode", false);
        return "manager/project-form";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GESTOR')")
    public String createProject(@Valid @ModelAttribute("projectDto") ProjectDto projectDto,
                               BindingResult result,
                               Authentication auth,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isEditMode", false);
            return "manager/project-form";
        }
        
        String userEmail = auth.getName();
        projectService.createProject(projectDto, userEmail);
        return "redirect:/manager/project-list"; // O a donde corresponda después de crear
    }

    @GetMapping("/{projectId}/edit")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String showEditProjectForm(@PathVariable Long projectId, Model model, Authentication auth) {
        String userEmail = auth.getName();
        Optional<Project> projectOpt = projectService.findProjectById(projectId, userEmail);
        
        if (!projectOpt.isPresent()) {
            return "error/404"; // Proyecto no encontrado o sin acceso
        }
        
        Project project = projectOpt.get();
        
        // Verificar que el usuario es propietario
        if (!project.getOwner().getEmail().equals(userEmail)) {
            return "error/403"; // Acceso prohibido
        }
        
        // Convertir Project a ProjectDto
        ProjectDto projectDto = new ProjectDto();
        projectDto.setTitle(project.getTitle());
        projectDto.setDescription(project.getDescription());
        
        // Añadir al modelo
        model.addAttribute("projectDto", projectDto);
        model.addAttribute("isEditMode", true);
        model.addAttribute("projectId", projectId);
        
        return "manager/project-form";
    }

    @PostMapping("/{projectId}/edit") // O usar PUT si prefieres RESTful
    @PreAuthorize("hasAuthority('GESTOR')")
    public String updateProject(@PathVariable Long projectId,
                               @Valid @ModelAttribute("projectDto") ProjectDto projectDto,
                               BindingResult result,
                               Authentication auth,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("isEditMode", true);
            model.addAttribute("projectId", projectId);
            return "manager/project-form";
        }
        
        String userEmail = auth.getName();
        projectService.updateProject(projectId, projectDto, userEmail);
        return "redirect:/projects/" + projectId;
    }

    @PostMapping("/{projectId}/close")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String closeProject(@PathVariable Long projectId, Authentication auth, RedirectAttributes redirectAttributes) {
        // ... verificar propiedad, llamar al servicio para cerrar proyecto
        String userEmail = auth.getName();
        Optional<Project> projectOpt = projectService.findProjectById(projectId, userEmail);
        if (!projectOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Proyecto no encontrado o sin acceso.");
            return "redirect:/manager/project-list";
        }
        Project project = projectOpt.get();
        // Verificar que el usuario es propietario
        if (!project.getOwner().getEmail().equals(userEmail)) {
            redirectAttributes.addFlashAttribute("error", "Acceso prohibido.");
            return "redirect:/manager/project-list";
        }
        // Cerrar el proyecto
        projectService.closeProject(projectId, userEmail);
        // Redirigir con mensaje de éxito
        redirectAttributes.addFlashAttribute("message", "Proyecto cerrado.");
        return "redirect:/manager/dashboard";
    }

    @PostMapping("/{projectId}/invite")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String inviteCollaborator(@PathVariable Long projectId,
                                    @RequestParam String email,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        // 1. Verificar que el gestor actual es dueño del proyecto projectId.
        String userEmail = auth.getName();
        Optional<Project> projectOpt = projectService.findProjectById(projectId, userEmail);
        if (!projectOpt.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Proyecto no encontrado o sin acceso.");
            return "redirect:/manager/project-list";
        }
        Project project = projectOpt.get();
        // 2. Verificar que el email es válido y no está ya en el proyecto.
        if (!userService.isValidEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email inválido.");
            return "redirect:/projects/" + projectId;
        }
        if (project.getCollaborators().stream().anyMatch(c -> c.getEmail().equals(email))) {
            redirectAttributes.addFlashAttribute("error", "El colaborador ya está en el proyecto.");
            return "redirect:/projects/" + projectId;
        }
        // 3. Invitar al colaborador.
        boolean success = projectService.inviteCollaborator(projectId, email);
        if (!success) {
            redirectAttributes.addFlashAttribute("error", "Error al invitar al colaborador.");
            return "redirect:/projects/" + projectId;
        }
        // 4. Redirigir con mensaje de éxito.
        redirectAttributes.addFlashAttribute("message", "Colaborador invitado con éxito.");




        return "redirect:/projects/" + projectId;
    }


    // --- Rutas Comunes (con control de acceso interno) ---

    @GetMapping("/{projectId}")
    @PreAuthorize("isAuthenticated()") // Solo usuarios logueados
    public String viewProjectDetails(@PathVariable Long projectId, Model model, Authentication auth) {
        // 1. Obtener el usuario actual (email/id) y su rol.
        String userEmail = auth.getName();
        String userRole = auth.getAuthorities().iterator().next().getAuthority();
        
        // 2. Cargar el proyecto 'projectId' y verificar acceso del usuario
        Optional<Project> projectOpt = projectService.findProjectById(projectId, userEmail);
        
        // 3. Verificar si el usuario tiene acceso
        if (!projectOpt.isPresent()) {
            return "error/404"; // Proyecto no encontrado o sin acceso
        }
        
        Project project = projectOpt.get();
        
        // 4. Si tiene acceso, preparar datos para la vista
        model.addAttribute("project", project);
        model.addAttribute("userRole", userRole);
        model.addAttribute("isOwner", project.getOwner().getEmail().equals(userEmail));
        
        // 5. Cargar tareas asociadas al proyecto - LÍNEA CORREGIDA
        List<Task> tasks = taskService.findTasksByProjectId(projectId, userEmail);
        model.addAttribute("tasks", tasks);
        
        // 6. Los colaboradores ya están en el proyecto
        model.addAttribute("collaborators", project.getCollaborators());
        
        // 7. Determinar la vista según el rol del usuario
        if ("GESTOR".equals(userRole)) {
            return "manager/project-detail";
        } else {
            return "collaborator/project-detail";
        }
    }

    // Otros métodos para listar proyectos (uno para manager, otro para colaborador)
    @GetMapping("/manager/list")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String listManagerProjects(Model model, Authentication auth) {
        String userEmail = auth.getName();
        List<Project> projects = projectService.findProjectsByOwner(userEmail);
        model.addAttribute("projects", projects);
        return "manager/project-list";
    }

    // Método adicional para manejar la ruta utilizada en las vistas
    @GetMapping("/manager/project-list")
    @PreAuthorize("hasAuthority('GESTOR')")
    public String managerProjectList(Model model, Authentication auth) {
        return listManagerProjects(model, auth);
    }

    @GetMapping("/collaborator/list")
    @PreAuthorize("hasAuthority('COLABORADOR')")
    public String listCollaboratorProjects(Model model, Authentication auth) {
        String userEmail = auth.getName();
        List<Project> projects = projectService.findProjectsByCollaborator(userEmail);
        model.addAttribute("projects", projects);
        return "collaborator/project-list";
    }
}
