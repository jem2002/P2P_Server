package com.universidad.messaging.server.api;


import com.universidad.messaging.server.persistencia.api.ICommentRepository;
import com.universidad.messaging.server.shared.api.dto.CommentDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/comments")
@RestController
public class CommentRestController {


    private final ICommentRepository commentRepository;


    public CommentRestController(ICommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @GetMapping
    public ResponseEntity<?> listarComentarios(
            @RequestParam(name = "username",     required = false)            String username,
            @RequestParam(name = "documentName", required = false)            String documentName,
            @RequestParam(name = "sentiment",    required = false)            String sentiment,
            @RequestParam(name = "fromDate",     required = false)            String fromDate,
            @RequestParam(name = "toDate",       required = false)            String toDate,
            @RequestParam(name = "sortBy",       defaultValue = "created_at") String sortBy,
            @RequestParam(name = "sortDir",      defaultValue = "desc")       String sortDir
    ) {
        try {
            List<CommentDTO> comentarios = commentRepository.buscarComentarios(
                    username, documentName, sentiment,
                    fromDate, toDate,
                    sortBy, sortDir
            );
            return ResponseEntity.ok(comentarios);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Parámetro inválido: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al listar los comentarios: " + e.getMessage());
        }
    }


}
