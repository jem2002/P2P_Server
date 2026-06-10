package com.universidad.messaging.server.api;

import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import com.universidad.messaging.server.shared.schema.documentSchema.DocumentInfo;
import com.universidad.messaging.server.shared.schema.documentSchema.DownloadDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentRestController.class)
public class DocumentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IDocumentRepository documentRepository;

    @org.springframework.boot.test.context.TestConfiguration
    static class Config {
        @org.springframework.context.annotation.Bean
        public com.universidad.messaging.server.config.ServerConfig serverConfig() {
            return new com.universidad.messaging.server.config.ServerConfig();
        }
    }

    @Test
    void testListarDocumentos() throws Exception {
        // GIVEN
        DocumentInfo doc1 = new DocumentInfo(1L, "doc1.pdf", 1000L, "pdf", "/path/doc1.pdf", "user1");
        DocumentInfo doc2 = new DocumentInfo(2L, "doc2.txt", 500L, "txt", "/path/doc2.txt", "user2");
        when(documentRepository.listarDocumentosDisponibles(null)).thenReturn(Arrays.asList(doc1, doc2));

        // WHEN & THEN
        mockMvc.perform(get("/api/documents/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("doc1.pdf"))
                .andExpect(jsonPath("$[1].nombre").value("doc2.txt"));
    }

    @Test
    void testListarDocumentosConUsername() throws Exception {
        // GIVEN
        DocumentInfo doc1 = new DocumentInfo(1L, "doc1.pdf", 1000L, "pdf", "/path/doc1.pdf", "user1");
        when(documentRepository.listarDocumentosDisponibles("user1")).thenReturn(Arrays.asList(doc1));

        // WHEN & THEN
        mockMvc.perform(get("/api/documents/listar").param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("doc1.pdf"))
                .andExpect(jsonPath("$[0].propietario").value("user1"));
    }

    @Test
    void testObtenerDocumentoPorId() throws Exception {
        // GIVEN
        DownloadDetails details = new DownloadDetails("doc1.pdf", 1000L, "/path/encrypted/doc1.pdf");
        when(documentRepository.obtenerDetallesDescarga(1L)).thenReturn(details);

        // WHEN & THEN
        mockMvc.perform(get("/api/documents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("doc1.pdf"));
    }

    @Test
    void testObtenerDocumentoPorIdNotFound() throws Exception {
        // GIVEN
        when(documentRepository.obtenerDetallesDescarga(999L)).thenReturn(null);

        // WHEN & THEN
        mockMvc.perform(get("/api/documents/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testErrorInterno() throws Exception {
        // GIVEN
        when(documentRepository.listarDocumentosDisponibles(null)).thenThrow(new RuntimeException("Database error"));

        // WHEN & THEN
        mockMvc.perform(get("/api/documents/listar"))
                .andExpect(status().isInternalServerError());
    }
}
