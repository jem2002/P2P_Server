package com.universidad.messaging.server.api;
import com.universidad.messaging.server.shared.api.dto.ConnectionDTO;
import com.universidad.messaging.server.shared.api.dto.UserDTO;
import com.universidad.messaging.server.persistencia.api.IUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RequestMapping("api/users")
@RestController
public class UserRestController {

    private final IUserRepository userRepository;

    public UserRestController(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> listUsers(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate) {
        try {
            LocalDate start = (fromDate != null && !fromDate.isBlank()) ? LocalDate.parse(fromDate) : null;
            LocalDate end = (toDate != null && !toDate.isBlank()) ? LocalDate.parse(toDate) : null;

            List<UserDTO> usuarios = userRepository.buscarUsuarios(username, start, end);
            return ResponseEntity.ok(usuarios);

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Formato de fecha inválido. Por favor usa el formato YYYY-MM-DD (Ej: 2026-06-10)");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al buscar usuarios: " + e.getMessage());
        }
    }

    @GetMapping("/connections")
    public ResponseEntity<?> listConnections(
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "nodeId", required = false) String nodeId,
            @RequestParam(name = "protocol", required = false) String protocol,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate) {
        try {
            LocalDate start = (fromDate != null && !fromDate.isBlank()) ? LocalDate.parse(fromDate) : null;
            LocalDate end = (toDate != null && !toDate.isBlank()) ? LocalDate.parse(toDate) : null;

            List<ConnectionDTO> conexiones = userRepository.buscarConexiones(
                    username, nodeId, protocol, isActive, start, end
            );
            return ResponseEntity.ok(conexiones);

        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Formato de fecha inválido. Por favor usa el formato YYYY-MM-DD (Ej: 2026-06-10)");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al buscar conexiones: " + e.getMessage());
        }
    }
}
