package MySqlRepository;

import com.universidad.messaging.server.shared.api.dto.ConnectionDTO;
import com.universidad.messaging.server.shared.schema.userSchema.ActiveClient;
import com.universidad.messaging.server.shared.schema.userSchema.UserRecord;
import MySqlRepository.db.IDatabaseConnectionManager;
import com.universidad.messaging.server.persistencia.api.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación MySQL del repositorio de usuarios.
 * Extraída de MySqlDao para cumplir SRP — cada DAO implementa una sola interfaz.
 *
 * Principios aplicados:
 * - SRP: solo operaciones de usuarios y sus conexiones.
 * - DIP: DatabaseConnectionManager inyectado por constructor.
 * - ISP: implementa únicamente IUserRepository.
 */
public class MySqlUserDao implements IUserRepository {

    private static final Logger logger = LoggerFactory.getLogger(MySqlUserDao.class);
    private final IDatabaseConnectionManager dbManager;

    public MySqlUserDao(IDatabaseConnectionManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public long obtenerORegistrarUsuario(String username, String ipAddress) throws SQLException {
        String selectSql = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, ip_address) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, username);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("id");
                    }
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, ipAddress);
                insertStmt.executeUpdate();

                try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    } else {
                        throw new SQLException("Fallo al obtener el ID del usuario insertado.");
                    }
                }
            }
        }
    }

    @Override
    public String obtenerNombreUsuario(long userId) throws Exception {
        String sql = "SELECT username FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }
        return "UsuarioDesconocido";
    }

    @Override
    public long obtenerIdUsuarioPorUsername(String username) throws Exception {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                } else {
                    throw new Exception("El usuario " + username + " no existe en la base de datos.");
                }
            }
        }
    }

    @Override
    public List<ActiveClient> listarClientesActivos() throws Exception {
        List<ActiveClient> activos = new ArrayList<>();
        // Modificado para extraer también la columna 'node_id'
        String sql = "SELECT u.username, c.ip_address, c.connected_at, c.node_id " +
                "FROM users u " +
                "JOIN client_connections c ON u.id = c.user_id " +
                "WHERE c.is_active = TRUE";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Modificado para pasar el node_id al constructor de ActiveClient
                activos.add(new ActiveClient(
                        rs.getString("username"),
                        rs.getString("ip_address"),
                        rs.getString("connected_at"),
                        rs.getString("node_id") // Extraemos el nodo guardado
                ));
            }
        }

        return activos;
    }


    @Override
    public List<UserRecord> listarUsuariosRegistrados() throws SQLException {
        List<UserRecord> usuarios = new ArrayList<>();
        String sql = "SELECT id, username, ip_address, created_at FROM users ORDER BY id ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("ip_address"),
                        rs.getString("created_at")
                ));
            }
        }
        return usuarios;
    }
    public List<ConnectionDTO> buscarConexiones(
            String username,
            String ipAddress,
            String nodeId,
            String protocol,
            Boolean isActive,
            String fromDate,
            String toDate,
            String sortBy,
            String sortDir
    ) throws SQLException {

        StringBuilder sql = new StringBuilder("""
            SELECT u.username,
                   cc.ip_address,
                   cc.node_id,
                   cc.port,
                   cc.connected_at,
                   cc.disconnected_at,
                   cc.protocol,
                   cc.is_active
            FROM client_connections cc
            INNER JOIN users u ON u.id = cc.user_id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append(" AND LOWER(u.username) = LOWER(?) ");
            params.add(username.trim());
        }

        if (ipAddress != null && !ipAddress.isBlank()) {
            sql.append(" AND cc.ip_address = ? ");
            params.add(ipAddress.trim());
        }

        if (nodeId != null && !nodeId.isBlank()) {
            sql.append(" AND LOWER(cc.node_id) LIKE LOWER(?) ");
            params.add("%" + nodeId.trim() + "%");
        }

        if (protocol != null && !protocol.isBlank()) {
            switch (protocol.toUpperCase()) {
                case "TCP", "UDP" -> sql.append(" AND cc.protocol = ? ");
                default           -> throw new IllegalArgumentException(
                        "protocol inválido: usa 'TCP' o 'UDP'");
            }
            params.add(protocol.toUpperCase().trim());
        }

        // isActive es Boolean (no primitivo) para distinguir null = sin filtro
        if (isActive != null) {
            sql.append(" AND cc.is_active = ? ");
            params.add(isActive);
        }

        if (fromDate != null && !fromDate.isBlank()) {
            sql.append(" AND cc.connected_at >= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(fromDate).atStartOfDay()));
        }

        if (toDate != null && !toDate.isBlank()) {
            sql.append(" AND cc.connected_at <= ? ");
            params.add(Timestamp.valueOf(LocalDate.parse(toDate).atTime(23, 59, 59)));
        }

        String column = switch (sortBy != null ? sortBy : "connected_at") {
            case "username"                     -> "u.username";
            case "ipAddress",  "ip_address"     -> "cc.ip_address";
            case "nodeId",     "node_id"        -> "cc.node_id";
            case "protocol"                     -> "cc.protocol";
            case "isActive",   "is_active"      -> "cc.is_active";
            case "connectedAt","connected_at"   -> "cc.connected_at";
            case "disconnectedAt",
                 "disconnected_at"             -> "cc.disconnected_at";
            default                             -> "cc.connected_at";
        };
        String direction = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        sql.append(" ORDER BY ").append(column).append(" ").append(direction);

        List<ConnectionDTO> result = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                setParam(stmt, i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                    Timestamp disconnectedAt = rs.getTimestamp("disconnected_at");

                    result.add(new ConnectionDTO(
                            rs.getString("username"),
                            rs.getString("ip_address"),
                            rs.getString("node_id"),
                            rs.getInt("port"),
                            rs.getString("connected_at"),
                            disconnectedAt != null ? disconnectedAt.toString() : null,
                            rs.getString("protocol"),
                            rs.getBoolean("is_active")
                    ));
                }
            }
        }

        return result;
    }


    private void setParam(PreparedStatement stmt, int index, Object value)
            throws SQLException {
        switch (value) {
            case String s    -> stmt.setString(index, s);
            case Integer n   -> stmt.setInt(index, n);
            case Timestamp t -> stmt.setTimestamp(index, t);
            default          -> stmt.setObject(index, value);
        }
    }


}