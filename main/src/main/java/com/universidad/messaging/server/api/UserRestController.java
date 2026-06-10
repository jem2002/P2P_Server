package com.universidad.messaging.server.api;

import com.universidad.messaging.server.persistencia.api.IUserRepository;
import com.universidad.messaging.server.shared.api.dto.ConnectionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RequestMapping("/api/users")
@RestController
public class UserRestController {

    private final IUserRepository userRepository;


    public UserRestController(IUserRepository userRepository){

        this.userRepository = userRepository;

    }

    @GetMapping
    public ResponseEntity<?> listarConexiones(
            @RequestParam(name = "username",  required = false)            String  username,
            @RequestParam(name = "ipAddress", required = false)            String  ipAddress,
            @RequestParam(name = "nodeId",    required = false)            String  nodeId,
            @RequestParam(name = "protocol",  required = false)            String  protocol,
            @RequestParam(name = "isActive",  required = false)            Boolean isActive,
            @RequestParam(name = "fromDate",  required = false)            String  fromDate,
            @RequestParam(name = "toDate",    required = false)            String  toDate,
            @RequestParam(name = "sortBy",    defaultValue = "connected_at") String sortBy,
            @RequestParam(name = "sortDir",   defaultValue = "desc")       String  sortDir
    ) {
        try {
            List<ConnectionDTO> conexiones = userRepository.buscarConexiones(
                    username, ipAddress, nodeId, protocol,
                    isActive, fromDate, toDate,
                    sortBy, sortDir
            );
            return ResponseEntity.ok(conexiones);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body("Parámetro inválido: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error al listar las conexiones: " + e.getMessage());
        }
    }



}
