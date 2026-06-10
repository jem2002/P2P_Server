package com.universidad.messaging.server.api;

import com.universidad.messaging.server.config.ServerConfig;
import com.universidad.messaging.server.persistencia.api.ICommentRepository;
import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import com.universidad.messaging.server.persistencia.api.IUserRepository;
import com.universidad.messaging.server.shared.api.dto.NodeInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/info")
public class InfoRestController {

    private final ServerConfig serverConfig;
    private final IUserRepository userRepository;
    private final IDocumentRepository documentRepository;
    private final ICommentRepository commentRepository;


    // Constructor para inyección automática de Spring
    public InfoRestController(ServerConfig serverConfig, IUserRepository userRepository,
                              IDocumentRepository documentRepository, ICommentRepository commentRepository) {
        this.serverConfig = serverConfig;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.commentRepository = commentRepository;


    }

    @GetMapping
    public ResponseEntity<NodeInfoDTO> getNodeInfo() {
        // Mapeamos los datos de ServerConfig directamente al DTO

        try {
            NodeInfoDTO info = new NodeInfoDTO(
                    serverConfig.getHost(),
                    serverConfig.getPort(),
                    serverConfig.getMaxConnections(),
                    serverConfig.getNodeId(),
                    serverConfig.getClusterPort(),
                    userRepository.contarUsuariosRegistrados(),
                    documentRepository.contarMensajesRegistrados(),
                    documentRepository.contarDocumentosRegistrados(),
                    commentRepository.contarComentariosRegistrados()
                    );

            // Retornamos un HTTP 200 OK con el cuerpo JSON
            return ResponseEntity.ok(info);
        } catch (Exception e){
            return null;
        }

        }
}
