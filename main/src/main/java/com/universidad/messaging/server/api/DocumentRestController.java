package com.universidad.messaging.server.api;


import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/documents")
@RestController
public class DocumentRestController {

    private final IDocumentRepository documentRepository;

    public DocumentRestController(IDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }




}
