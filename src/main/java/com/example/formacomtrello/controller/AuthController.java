package com.example.formacomtrello.controller;

import com.example.formacomtrello.dto.UserRegistrationDto;
import com.example.formacomtrello.model.User;
import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.Optional;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;



    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        // Comprobar si el usuario ya está autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            // Si ya está logueado, redirigir a home
            return "redirect:/home";
        }

        // Comprobar si viene de un logout o un error de login
        if (error != null) {
            model.addAttribute("loginError", "Usuario o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "Has cerrado sesión correctamente.");
        }

        // Comprobar si necesita establecer contraseña
        // Esto es más complejo, normalmente se redirige aquí *después* de un intento de login fallido
        // O se maneja con un filtro personalizado. Simplificación:
        // Si hay un parámetro ?setpassword=email@ejemplo.com
        if (model.containsAttribute("needsSetPassword")) {
            model.addAttribute("emailToSetPassword", model.getAttribute("emailToSetPassword"));
            return "auth/set-password";
        }

        return "auth/login";
    }

    // Redirección después del login exitoso
    @GetMapping("/home-redirect")
    public String homeAfterLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Optional<User> userOpt = userService.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Eliminar esta condición:
            // if (user.isFirstLogin() && user.getRole() == Role.COLABORADOR) {
            //     return "redirect:/set-password";
            // }

            // Redirigir al dashboard correspondiente
            if (user.getRole() == com.example.formacomtrello.model.Role.GESTOR) {
                return "redirect:/manager/dashboard";
            } else {
                return "redirect:/collaborator/dashboard";
            }
        }
        return "redirect:/login?error=true";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/process-register")
    public String processRegistration(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                                      BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        
        // Añadir verificación de coincidencia de contraseñas
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            model.addAttribute("registrationError", "Las contraseñas no coinciden");
            return "auth/register";
        }
        
        try {
            userService.registerManager(userDto);
            redirectAttributes.addFlashAttribute("registrationSuccess", "¡Registro exitoso! Ahora puedes iniciar sesión.");
            return "redirect:/login";
        } catch (RuntimeException ex) { // Ser más específico con excepciones
            model.addAttribute("registrationError", ex.getMessage());
            model.addAttribute("userDto", userDto); // Devolver datos para no rellenar
            return "auth/register";
        }
    }

    @GetMapping("manager/collaborators")
    public String showCollaboratorForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "manager/collaborators";
    }

    @PostMapping("/invite-collaborator")
    public String collaboratorRegistration(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                                      BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "manager/collaborators";
        }
        
        try {
            // Verificar si las contraseñas coinciden
            if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
                model.addAttribute("registrationError", "Las contraseñas no coinciden");
                return "manager/collaborators";
            }
            
            User newCollaborator = userService.inviteCollaborator(userDto);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Colaborador " + newCollaborator.getNombre() + " " + 
                newCollaborator.getApellidos() + " registrado correctamente.");
            
            return "redirect:/manager/dashboard";
        } catch (RuntimeException ex) {
            model.addAttribute("registrationError", ex.getMessage());
            return "manager/collaborators";
        }
    }

    @GetMapping("/set-password")
    public String showSetPasswordForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        String email = auth.getName();
        model.addAttribute("email", email);
        return "auth/set-password";

        // Eliminar esta validación:
        // Optional<User> userOpt = userService.findByEmail(email);
        // if (userOpt.isPresent() && userOpt.get().isFirstLogin()) {
        //     model.addAttribute("email", email);
        //     return "auth/set-password";
        // }
        // return "redirect:/home";
    }

    @PostMapping("/process-set-password")
    public String processSetPassword(@RequestParam String password, @RequestParam String confirmPassword,
                                     RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        String email = auth.getName();

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("passwordError", "Las contraseñas no coinciden.");
            return "redirect:/set-password"; // Vuelve al formulario
        }
        if (password.length() < 6) { // Añadir validación más robusta
            redirectAttributes.addFlashAttribute("passwordError", "La contraseña debe tener al menos 6 caracteres.");
            return "redirect:/set-password";
        }

        try {
            userService.setPasswordForCollaborator(email, password);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Contraseña establecida correctamente. Ahora puedes usar la aplicación.");
            // Forzar un re-login o simplemente redirigir a home (Spring Security debería mantener la sesión)
            return "redirect:/home";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("passwordError", "Error al establecer la contraseña: " + ex.getMessage());
            return "redirect:/set-password";
        }
    }
}
