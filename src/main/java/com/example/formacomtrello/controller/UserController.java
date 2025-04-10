package com.example.formacomtrello.controller;

import com.example.formacomtrello.dto.UserProfileDto;
import com.example.formacomtrello.exception.ResourceNotFoundException;
import com.example.formacomtrello.model.User;
import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequestMapping("/user") // Base path para las acciones de perfil de usuario
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${app.upload.dir:${user.home}/uploads/profiles}")
    private String uploadDir;

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
        User currentUser = userService.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userEmail));
        
        // Variables para validación manual
        boolean passwordChangeRequested = false;
        boolean hasValidationErrors = bindingResult.hasErrors();
        
        // Comprobar si se está intentando cambiar la contraseña
        String newPassword = userProfileDto.getPassword();
        String confirmPassword = userProfileDto.getConfirmPassword();
        String currentPassword = userProfileDto.getCurrentPassword();
        
        // 1. Verificar si se está intentando cambiar la contraseña
        // Modificamos la condición para verificar que al menos uno de los campos tenga contenido
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            passwordChangeRequested = true;
            
            // Validaciones adicionales solo si se intenta cambiar la contraseña
            if (newPassword.length() < 6) {
                model.addAttribute("passwordError", "La contraseña debe tener al menos 6 caracteres");
                hasValidationErrors = true;
            }
            
            // 1.1 Verificar que se ha proporcionado la contraseña actual
            if (currentPassword == null || currentPassword.trim().isEmpty()) {
                model.addAttribute("currentPasswordError", "Debes proporcionar tu contraseña actual para cambiarla");
                hasValidationErrors = true;
            } else if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
                // 1.2 Verificar que la contraseña actual es correcta
                model.addAttribute("currentPasswordError", "La contraseña actual no es correcta");
                hasValidationErrors = true;
            }
            
            // 1.3 Verificar que la nueva contraseña y la confirmación coinciden
            if (confirmPassword == null || !confirmPassword.equals(newPassword)) {
                model.addAttribute("confirmPasswordError", "Las contraseñas no coinciden");
                hasValidationErrors = true;
            }
        } else {
            // Si la nueva contraseña está vacía pero hay contenido en otros campos de contraseña
            if ((currentPassword != null && !currentPassword.trim().isEmpty()) || 
                (confirmPassword != null && !confirmPassword.trim().isEmpty())) {
                model.addAttribute("passwordError", "Si deseas cambiar la contraseña, debes completar todos los campos");
                hasValidationErrors = true;
            }
        }

        // --- Re-obtener datos no editables si hay errores de validación ---
        if (hasValidationErrors) {
            // Es necesario volver a añadir los datos no editables al modelo
            // ya que no vienen en el DTO del formulario y la vista los necesita.
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", currentUser.getRole().name());
            // El userProfileDto con los errores ya está en el modelo gracias a @ModelAttribute
            return "user/profile"; // Volver a mostrar el formulario con los errores
        }

        // --- Si la validación es correcta, proceder a actualizar ---
        try {
            // Procesar la foto subida, si existe
            MultipartFile fotoFile = userProfileDto.getFotoFile();
            if (fotoFile != null && !fotoFile.isEmpty()) {
                String fotoUrl = saveProfileImage(fotoFile, currentUser.getEmail());
                currentUser.setFotoUrl(fotoUrl);
            } else if (userProfileDto.getFotoUrl() != null && !userProfileDto.getFotoUrl().trim().isEmpty()) {
                // Si no hay archivo subido pero sí hay URL, usar la URL
                currentUser.setFotoUrl(userProfileDto.getFotoUrl());
            }
            
            // 2. Actualizar los campos permitidos desde el DTO
            currentUser.setNombre(userProfileDto.getNombre());
            currentUser.setApellidos(userProfileDto.getApellidos());
            currentUser.setTelefono(userProfileDto.getTelefono());
            
            // 3. Actualizar la contraseña solo si se solicitó el cambio
            if (passwordChangeRequested) {
                currentUser.setPassword(passwordEncoder.encode(newPassword));
            }

            // 4. Guardar el usuario actualizado
            userService.save(currentUser);

            redirectAttributes.addFlashAttribute("successMessage", "Perfil actualizado correctamente.");
            return "redirect:/user/profile"; // Redirigir a la misma página de perfil

        } catch (ResourceNotFoundException e) {
            // Muy improbable si ya estaba autenticado, pero por si acaso.
            redirectAttributes.addFlashAttribute("errorMessage", "Error: Usuario no encontrado.");
            return "redirect:/home"; // O a una página de error
        } catch (IOException e) {
            // Error al manejar el archivo subido
            model.addAttribute("fotoFileError", "Error al procesar la imagen: " + e.getMessage());
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", currentUser.getRole().name());
            return "user/profile";
        } catch (Exception e) {
            // Capturar otros posibles errores del servicio o base de datos
            redirectAttributes.addFlashAttribute("errorMessage", "Error al actualizar el perfil: " + e.getMessage());
            
            // Necesitamos recargar los datos no editables
            model.addAttribute("userEmail", currentUser.getEmail());
            model.addAttribute("userRole", currentUser.getRole().name());
            model.addAttribute("userProfileDto", userProfileDto); // Pasar el DTO de vuelta
            return "user/profile";
        }
    }
    
    /**
     * Guarda la imagen de perfil en el servidor y devuelve la URL relativa.
     * 
     * @param file El archivo de imagen subido
     * @param email El email del usuario para nombrar el archivo
     * @return La URL relativa de la imagen guardada
     * @throws IOException Si hay problemas al guardar la imagen
     */
    private String saveProfileImage(MultipartFile file, String email) throws IOException {
        // Crear el directorio si no existe
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Limpiar el nombre de archivo y asegurarse de que sea único
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        // Generar un nombre único para evitar colisiones
        String uniqueFileName = email.replaceAll("[^a-zA-Z0-9]", "_") + "_" 
                + UUID.randomUUID().toString().substring(0, 8) + "_" 
                + fileName;
        
        // Guardar el archivo
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Devolver la URL relativa para acceder desde la web
        return "/uploads/profiles/" + uniqueFileName;
    }
}
