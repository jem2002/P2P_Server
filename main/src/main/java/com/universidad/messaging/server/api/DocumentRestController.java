package com.universidad.messaging.server.api;



import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import com.universidad.messaging.server.shared.api.dto.DocumentDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RequestMapping("/api/documents")
@RestController
public class DocumentRestController {

    private final IDocumentRepository documentRepository;

    public DocumentRestController(IDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }


    @GetMapping("/list")
    public ResponseEntity<?> listarDocumentos() {
        try {


            return ResponseEntity.ok(null);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al listar los mensajes procesados: " + e.getMessage());
        }
    }




}
