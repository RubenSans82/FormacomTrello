package com.example.formacomtrello.controller;

import com.example.formacomtrello.dto.UserProfileDto;
import com.example.formacomtrello.exception.ResourceNotFoundException;
import com.example.formacomtrello.model.User;
import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/user") // Base path para las acciones de perfil de usuario
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Muestra la página del perfil del usuario autenticado.
     * Pre-rellena el formulario con los datos actuales del usuario.
     *
     * @param model          El modelo para pasar datos a la vista.
     * @param authentication Objeto de autenticación de Spring Security.
     * @return El nombre de la vista del perfil.
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()") // Solo usuarios autenticados
    public String showUserProfile(Model model, Authentication authentication) {
        String userEmail = authentication.getName();

        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userEmail)); // No debería pasar si está autenticado

        // Crear el DTO a partir de los datos del usuario actual
        UserProfileDto profileDto = new UserProfileDto(
                currentUser.getNombre(),
                currentUser.getApellidos(),
                currentUser.getTelefono(),
                currentUser.getFotoUrl()
        );

        model.addAttribute("userProfileDto", profileDto);
        // Pasar también datos no editables para mostrarlos si es necesario
        model.addAttribute("userEmail", currentUser.getEmail());
        model.addAttribute("userRole", currentUser.getRole().name());

        return "user/profile"; // Nombre de la plantilla Thymeleaf: templates/user/profile.html
    }

    /**
     * Procesa la actualización del perfil del usuario.
     *
     * @param userProfileDto     DTO con los datos del formulario validados.
     * @param bindingResult      Resultado de la validación.
     * @param model              El modelo (por si hay errores y hay que volver a mostrar el form).
     * @param authentication     Objeto de autenticación.
     * @param redirectAttributes Para añadir mensajes flash en la redirección.
     * @return Redirección a la página de perfil o de vuelta al formulario si hay errores.
     */
    @PostMapping("/profile/update")
    @PreAuthorize("isAuthenticated()") // Solo usuarios autenticados
    public String updateUserProfile(@Valid @ModelAttribute("userProfileDto") UserProfileDto userProfileDto,
                                    BindingResult bindingResult,
                                    Model model,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        String userEmail = authentication.getName();

        // --- Re-obtener datos no editables si hay errores de validación ---
        if (bindingResult.hasErrors()) {
            // Es necesario volver a añadir los datos no editables al modelo
            // ya que no vienen en el DTO del formulario y la vista los necesita.
            User currentUser = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userEmail));
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", currentUser.getRole().name());
            // El userProfileDto con los errores ya está en el modelo gracias a @ModelAttribute
            return "user/profile"; // Volver a mostrar el formulario con los errores
        }

        // --- Si la validación es correcta, proceder a actualizar ---
        try {
            // Necesitamos una forma de actualizar usando el email y el DTO.
            // Añadimos un método al UserService o adaptamos el existente.
            // Asumamos que existe: userService.updateProfile(userEmail, userProfileDto);

            // 1. Obtener el usuario completo
            User currentUser = userService.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userEmail));

            // 2. Actualizar los campos permitidos desde el DTO
            currentUser.setNombre(userProfileDto.getNombre());
            currentUser.setApellidos(userProfileDto.getApellidos());
            currentUser.setTelefono(userProfileDto.getTelefono());
            currentUser.setFotoUrl(userProfileDto.getFotoUrl());
            // NO actualizamos email, rol, contraseña, firstLogin aquí.

            // 3. Guardar el usuario actualizado
            userService.save(currentUser); // Usamos el método save genérico

            redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado correctamente.");
            return "redirect:/user/profile"; // Redirigir a la misma página de perfil

        } catch (ResourceNotFoundException e) {
            // Muy improbable si ya estaba autenticado, pero por si acaso.
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Usuario no encontrado.");
            return "redirect:/home"; // O a una página de error
        } catch (Exception e) {
            // Capturar otros posibles errores del servicio o base de datos
            redirectAttributes.addFlashAttribute("errorMessage", "Error al actualizar el perfil: " + e.getMessage());
            // Volver al formulario puede ser útil para que el usuario vea qué pasó
            // Necesitamos recargar los datos no editables
            User currentUser = userService.findByEmail(userEmail).orElse(null); // Intenta recargar
            if (currentUser != null) {
                model.addAttribute("userEmail", currentUser.getEmail());
                model.addAttribute("userRole", currentUser.getRole().name());
            }
            model.addAttribute("userProfileDto", userProfileDto); // Pasar el DTO de vuelta
            return "user/profile";
        }
    }
}