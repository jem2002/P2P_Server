package com.universidad.messaging.server.api;


import com.universidad.messaging.server.servicios.api.IDocumentManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RequestMapping("/api/messages")
@RestController
public class MessagesRestController {

    private final IDocumentManager documentManager;


    public MessagesRestController(IDocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    @GetMapping("/listar")
    public ResponseEntity<?> listarMensajes(@RequestParam(name = "username", required = false) String username) {
        try {
            List<Map<String, String>> mensajes = documentManager.obtenerMensajesDisponibles(username);
            return ResponseEntity.ok(mensajes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al listar los mensajes procesados: " + e.getMessage());
        }
    }
}
