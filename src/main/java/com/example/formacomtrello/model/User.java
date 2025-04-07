package com.example.formacomtrello.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
//@NoArgsConstructor
//@AllArgsConstructor
//@Data // Genera getters, setters, toString, equals, hashCode
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // Usuario

    @Column(nullable = true) // Nullable al inicio para colaboradores
    private String password;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellidos;

    private String telefono;

    private String fotoUrl; // Ruta o URL de la foto

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Relación: Proyectos donde el usuario es colaborador
    @ManyToMany(mappedBy = "collaborators", fetch = FetchType.LAZY)
    private Set<Project> projectsCollaborating = new HashSet<>();

    // Relación: Tareas asignadas al usuario (si es colaborador)
    @OneToMany(mappedBy = "assignedUser", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Set<Task> assignedTasks = new HashSet<>();

    // Relación: Comentarios creados por el usuario
    @OneToMany(mappedBy = "author", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Set<Comment> comments = new HashSet<>();

    // Flag para indicar si es la primera vez que inicia sesión (para colaboradores)
    private boolean firstLogin = true;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Set<Project> getProjectsCollaborating() {
        return projectsCollaborating;
    }

    public void setProjectsCollaborating(Set<Project> projectsCollaborating) {
        this.projectsCollaborating = projectsCollaborating;
    }

    public Set<Task> getAssignedTasks() {
        return assignedTasks;
    }

    public void setAssignedTasks(Set<Task> assignedTasks) {
        this.assignedTasks = assignedTasks;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public void setComments(Set<Comment> comments) {
        this.comments = comments;
    }

    public boolean isFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(boolean firstLogin) {
        this.firstLogin = firstLogin;
    }
}