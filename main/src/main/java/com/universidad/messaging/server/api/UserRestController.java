package com.universidad.messaging.server.api;

import com.universidad.messaging.server.persistencia.api.IUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequestMapping("/api/users")
@RestController
public class UserRestController {

    private final IUserRepository userRepository;


    public UserRestController(IUserRepository userRepository){

        this.userRepository = userRepository;

    }

    @GetMapping("listar-registrados")
    public ResponseEntity<?> listarRegistrados() {
        try {
            return ResponseEntity.ok(userRepository.listarUsuariosRegistrados());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }


    @GetMapping("listar-activos")
    public ResponseEntity<?> listarActivos() {
        try {
            return ResponseEntity.ok(userRepository.listarClientesActivos());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }



}
