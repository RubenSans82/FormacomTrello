package com.example.formacomtrello.repository;

import com.example.formacomtrello.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Repositorio para la entidad Comment
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Busca todos los comentarios asociados a un ID de tarea específico,
    // ordenados por fecha de creación ascendente (gracias a 'OrderByCreatedAtAsc')
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}