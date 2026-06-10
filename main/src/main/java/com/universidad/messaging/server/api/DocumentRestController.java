package com.universidad.messaging.server.api;

import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import com.universidad.messaging.server.shared.schema.documentSchema.DownloadDetails;
import com.universidad.messaging.server.shared.schema.documentSchema.DocumentInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/listar")
    public ResponseEntity<?> listarDocumentos(@RequestParam(name = "username", required = false) String username) {
        try {
            List<DocumentInfo> documentos = documentRepository.listarDocumentosDisponibles(username);
            return ResponseEntity.ok(documentos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al listar los documentos: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerDocumentoPorId(@PathVariable("id") Long id) {
        try {
            DownloadDetails documento = documentRepository.obtenerDetallesDescarga(id);
            if (documento == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(documento);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al obtener el documento: " + e.getMessage());
        }
    }
}
