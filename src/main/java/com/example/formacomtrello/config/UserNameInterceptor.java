package com.example.formacomtrello.config;

import com.example.formacomtrello.model.User;
import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

@Component
public class UserNameInterceptor implements HandlerInterceptor {


    private UserService userService;

    @Autowired
    public UserNameInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() &&
                    !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
                String email = auth.getName();
                Optional<User> userOpt = userService.findByEmail(email);
                userOpt.ifPresent(user -> modelAndView.getModel().put("userName", user.getNombre()));
            }
        }
    }
}