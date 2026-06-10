package MySqlRepository;

import com.universidad.messaging.server.shared.api.dto.ConnectionDTO;
import com.universidad.messaging.server.shared.api.dto.UserDTO;
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





    private void setParam(PreparedStatement stmt, int index, Object value)
            throws SQLException {
        switch (value) {
            case String s    -> stmt.setString(index, s);
            case Integer n   -> stmt.setInt(index, n);
            case Timestamp t -> stmt.setTimestamp(index, t);
            default          -> stmt.setObject(index, value);
        }
    }


    @Override
    public int contarUsuariosRegistrados() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    @Override
    public List<UserDTO> buscarUsuarios(String username, LocalDate fromDate, LocalDate toDate) throws SQLException {
        List<UserDTO> usuarios = new ArrayList<>();
        // Nota: Asumimos que si la tabla users no tiene node_id de forma directa, se puede cruzar o dejar vacío.
        // Aquí añadí un LEFT JOIN simulado con la última conexión para mapear el 'nodeId' que exige tu UserDTO.
        StringBuilder sql = new StringBuilder(
                "SELECT u.username, " +
                        "      (SELECT c.node_id FROM client_connections c WHERE c.user_id = u.id ORDER BY c.connected_at DESC LIMIT 1) as node_id, " +
                        "      u.created_at " +
                        "FROM users u WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append(" AND u.username LIKE ?");
            params.add("%" + username + "%");
        }
        if (fromDate != null) {
            sql.append(" AND u.created_at >= ?");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND u.created_at <= ?");
            params.add(Date.valueOf(toDate));
        }

        sql.append(" ORDER BY u.created_at DESC");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                setParam(stmt, i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    usuarios.add(new UserDTO(
                            rs.getString("username"),
                            rs.getString("node_id"), // Mapeado dinámicamente desde la subquery
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return usuarios;
    }

    @Override
    public List<ConnectionDTO> buscarConexiones(String username, String nodeId, String protocol, Boolean isActive, LocalDate fromDate, LocalDate toDate) throws SQLException {
        List<ConnectionDTO> conexiones = new ArrayList<>();
        // Query adaptada para seleccionar todas las columnas requeridas por tu nuevo ConnectionDTO
        StringBuilder sql = new StringBuilder(
                "SELECT u.username, c.ip_address, c.node_id, c.port, c.protocol, c.connected_at, c.disconnected_at, c.is_active " +
                        "FROM client_connections c " +
                        "JOIN users u ON c.user_id = u.id " +
                        "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            sql.append(" AND u.username = ?");
            params.add(username);
        }
        if (nodeId != null && !nodeId.isBlank()) {
            sql.append(" AND c.node_id = ?");
            params.add(nodeId);
        }
        if (protocol != null && !protocol.isBlank()) {
            sql.append(" AND c.protocol = ?");
            params.add(protocol);
        }
        if (isActive != null) {
            sql.append(" AND c.is_active = ?");
            params.add(isActive);
        }
        if (fromDate != null) {
            sql.append(" AND c.connected_at >= ?");
            params.add(Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND c.connected_at <= ?");
            params.add(Date.valueOf(toDate));
        }

        sql.append(" ORDER BY c.connected_at DESC");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                setParam(stmt, i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    conexiones.add(new ConnectionDTO(
                            rs.getString("username"),
                            rs.getString("ip_address"),
                            rs.getString("node_id"),
                            rs.getInt("port"),              // Nuevo mapeo int primitivo
                            rs.getString("protocol"),
                            rs.getString("connected_at"),
                            rs.getString("disconnected_at"),// Nuevo mapeo String
                            rs.getBoolean("is_active")      // boolean primitivo
                    ));
                }
            }
        }
        return conexiones;
    }



}