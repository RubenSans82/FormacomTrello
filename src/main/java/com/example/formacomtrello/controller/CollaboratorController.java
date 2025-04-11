package com.example.formacomtrello.controller;

import com.example.formacomtrello.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/collaborators")
public class CollaboratorController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public String listCollaborators(Model model) {
        model.addAttribute("collaborators", userService.findAllCollaborators());
        return "collaborators/collaborator-list";
    }
}