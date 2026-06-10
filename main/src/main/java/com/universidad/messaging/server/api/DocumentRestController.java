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


    @GetMapping
    public ResponseEntity<?> listDocuments(
            @RequestParam(name = "owner",     required = false)            String owner,
            @RequestParam(name = "extension", required = false)            String extension,
            @RequestParam(name = "keyword",   required = false)            String keyword,
            @RequestParam(name = "fromDate",  required = false)            String fromDate,
            @RequestParam(name = "toDate",    required = false)            String toDate,
            @RequestParam(name = "sortBy",    defaultValue = "created_at") String sortBy,
            @RequestParam(name = "sortDir",   defaultValue = "desc")       String sortDir
    ) {
        try {
            List<DocumentDTO> documentos = documentRepository.buscarDocumentos(
                    owner, extension, keyword,
                    fromDate, toDate,
                    sortBy, sortDir
            );
            return ResponseEntity.ok(documentos);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Parámetro inválido: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al listar los documentos: " + e.getMessage());
        }
    }


}
