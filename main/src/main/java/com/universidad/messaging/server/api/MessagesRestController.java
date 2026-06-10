package com.universidad.messaging.server.api;


import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import com.universidad.messaging.server.shared.api.dto.MessageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

@RequestMapping("/api/messages")
@RestController
public class MessagesRestController {

    private final IDocumentRepository documentRepository;


    public MessagesRestController(IDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public ResponseEntity<?> listMessages(
            @RequestParam(name = "owner",      required = false)            String owner,
            @RequestParam(name = "target",     required = false)            String target,
            @RequestParam(name = "type",       required = false)            String type,
            @RequestParam(name = "keyword",    required = false)            String keyword,
            @RequestParam(name = "fromDate",   required = false)            String fromDate,
            @RequestParam(name = "toDate",     required = false)            String toDate,
            @RequestParam(name = "page",       defaultValue = "0")          int    page,
            @RequestParam(name = "size",       defaultValue = "20")         int    size,
            @RequestParam(name = "sortBy",     defaultValue = "created_at") String sortBy,
            @RequestParam(name = "sortDir",    defaultValue = "desc")       String sortDir
    ) {
        try {
            List<MessageDTO> mensajes = documentRepository.buscarMensajes(
                    owner, target, type, keyword,
                    fromDate, toDate,
                    page, size, sortBy, sortDir
            );
            return ResponseEntity.ok(mensajes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Parámetro inválido: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al listar los mensajes procesados: " + e.getMessage());
        }
    }



}
