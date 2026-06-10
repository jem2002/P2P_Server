package com.universidad.messaging.server.persistencia.api;

import com.universidad.messaging.server.shared.api.dto.ConnectionDTO;
import com.universidad.messaging.server.shared.schema.userSchema.ActiveClient;
import com.universidad.messaging.server.shared.schema.userSchema.UserRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrato de persistencia para la entidad Usuario.
 *
 * Principio aplicado: ISP — los servicios que solo necesitan operaciones
 * de usuario no ven métodos de documentos, logs ni sesiones.
 */
public interface IUserRepository {

    long obtenerORegistrarUsuario(String username, String ipAddress) throws SQLException;

    String obtenerNombreUsuario(long userId) throws Exception;

    long obtenerIdUsuarioPorUsername(String username) throws Exception;

    List<ActiveClient> listarClientesActivos() throws Exception;

    List<UserRecord> listarUsuariosRegistrados() throws SQLException;

    List<ConnectionDTO> buscarConexiones(
            String  username,
            String  ipAddress,
            String  nodeId,
            String  protocol,
            Boolean isActive,
            String  fromDate,
            String  toDate,
            String  sortBy,
            String  sortDir
    ) throws SQLException;

}
