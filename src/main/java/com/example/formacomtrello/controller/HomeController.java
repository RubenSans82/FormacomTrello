package com.example.formacomtrello.controller;

import com.example.formacomtrello.model.User;
import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomeController {
    @Autowired
    private UserService userService;

    // Método para la ruta raíz
    @GetMapping("/")
    public String root(Model model) {
        // Comprobar si el usuario está autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            // Si ya está logueado, añadimos información útil al modelo pero dejamos que la vista se encargue de la redirección
            String email = auth.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            userOpt.ifPresent(user -> model.addAttribute("userName", user.getNombre()));
        }
        // Mostrar directamente la página home
        return "home";
    }

    // Método específico para la ruta /home
    @GetMapping("/home")
    public String homePage(Model model) {
        // Mismo comportamiento que la ruta raíz
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            String email = auth.getName();
            Optional<User> userOpt = userService.findByEmail(email);
            userOpt.ifPresent(user -> model.addAttribute("userName", user.getNombre()));
        }
        return "home";
    }
}
