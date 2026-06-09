package Services;

import com.universidad.messaging.server.shared.schema.userSchema.ActiveClient;
import com.universidad.messaging.server.persistencia.api.ISessionRepository;
import com.universidad.messaging.server.persistencia.api.IUserRepository;
import com.universidad.messaging.server.servicios.api.IUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de dominio para la gestión de usuarios.
 *
 * Principio aplicado: DIP — depende de IUserRepository e ISessionRepository
 * (abstracciones), no de MySqlDao (implementación concreta).
 */
public class UserManager implements IUserManager {

    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private final IUserRepository userRepository;
    private final ISessionRepository sessionRepository;

    public UserManager(IUserRepository userRepository, ISessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    public long conectarUsuario(String username, String ipAddress, int port, String nodeId) throws Exception {
        long userId = userRepository.obtenerORegistrarUsuario(username, ipAddress);
        sessionRepository.registrarSesionActiva(userId, ipAddress, port, "TCP", nodeId);
        return userId;
    }

    public long obtenerORegistrarUsuario(String username, String ipAddress) throws Exception {
        return userRepository.obtenerORegistrarUsuario(username, ipAddress);
    }

    public long desconectarPorCaidaDeRed(String ipAddress, int port) {
        try {
            long userId = sessionRepository.cerrarSesionPorIpYPuerto(ipAddress, port);
            logger.info("Estado actualizado: Sesión cerrada en BD para {}:{} (User ID: {})", ipAddress, port, userId);
            return userId;
        } catch (Exception e) {
            logger.error("Error al cerrar sesión por caída de red", e);
            return 0;
        }
    }

    public void cerrarSesionPorUsername(String username) {
        try {
            sessionRepository.cerrarSesionPorUsername(username);
            logger.info("Estado actualizado: Sesión cerrada en BD para username: {}", username);
        } catch (Exception e) {
            logger.error("Error al cerrar sesión por username", e);
        }
    }

    public void desconectarClientesPorNodo(String nodeId) {
        try {
            sessionRepository.cerrarSesionesPorNodo(nodeId);
            logger.info("Estado actualizado: Sesiones cerradas en BD para todos los clientes del nodo: {}", nodeId);
        } catch (Exception e) {
            logger.error("Error al cerrar las sesiones del nodo desconectado: {}", nodeId, e);
        }
    }

    public List<ActiveClient> obtenerClientesActivos() {
        try {
            return userRepository.listarClientesActivos();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de clientes activos", e);
            return new ArrayList<>();
        }
    }

    public long obtenerIdUsuario(String username) throws Exception {
        return userRepository.obtenerIdUsuarioPorUsername(username);
    }

    public String obtenerNombreUsuario(long userId) {
        try {
            return userRepository.obtenerNombreUsuario(userId);
        } catch (Exception e) {
            logger.error("Error al obtener nombre de usuario", e);
            return "UsuarioDesconocido";
        }
    }
}