package com.universidad.messaging.server.api;

import com.universidad.messaging.server.config.ServerConfig;
import com.universidad.messaging.server.shared.api.dto.NodeInfoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/info")
public class InfoRestController {

    private final ServerConfig serverConfig;

    // Constructor para inyección automática de Spring
    public InfoRestController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @GetMapping
    public ResponseEntity<NodeInfoDTO> getNodeInfo() {
        // Mapeamos los datos de ServerConfig directamente al DTO
        NodeInfoDTO info = new NodeInfoDTO(
                serverConfig.getHost(),
                serverConfig.getPort(),
                serverConfig.getMaxConnections(),
                serverConfig.getNodeId(),
                serverConfig.getClusterPort(),
                serverConfig.getHeartbeatIntervalMs(),
                serverConfig.getFailureTimeoutMs(),
                serverConfig.getSeedNodes()
        );

        // Retornamos un HTTP 200 OK con el cuerpo JSON
        return ResponseEntity.ok(info);
    }
}
