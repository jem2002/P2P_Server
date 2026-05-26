package orchestrators;

import JsonSchema.ClientAddress;
import LogService.LogManager;
import UserService.UserManager;
import MessageParser.BroadcastManager;
import registry.LocalClientRegistry;
import replication.ReplicationEvent;
import replication.ReplicationManager;
import topology.RoutingTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ports.api.IDisconnectionHandler;

import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Servicio que orquesta la desconexión limpia de un cliente.
 * Implementado en 'main' para acceder a todos los módulos sin violar dependencias.
 */
public class DisconnectionOrchestrator implements IDisconnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DisconnectionOrchestrator.class);

    private final UserManager userManager;
    private final LogManager logManager;
    private final BroadcastManager broadcastManager;

    // Dependencias de cluster (null si deshabilitado)
    private RoutingTable routingTable;
    private LocalClientRegistry localClientRegistry;
    private ReplicationManager replicationManager;
    private String localNodeId;

    // Suppliers para obtener listas actualizadas sin acoplamiento circular
    private Supplier<String> clientsListSupplier;
    private Supplier<String> logsListSupplier;

    public DisconnectionOrchestrator(UserManager userManager, LogManager logManager, BroadcastManager broadcastManager) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.broadcastManager = broadcastManager;
    }

    public void enableCluster(RoutingTable rt, LocalClientRegistry lcr,
                              ReplicationManager rm, String nodeId) {
        this.routingTable = rt;
        this.localClientRegistry = lcr;
        this.replicationManager = rm;
        this.localNodeId = nodeId;
    }

    public void setListSuppliers(Supplier<String> clientsListSupplier, Supplier<String> logsListSupplier) {
        this.clientsListSupplier = clientsListSupplier;
        this.logsListSupplier = logsListSupplier;
    }

    @Override
    public void procesarDesconexion(String rawClientIp, OutputStream out) {
        try {
            ClientAddress address = ClientAddress.parse(rawClientIp);
            if (out != null) broadcastManager.removeStream(out);

            long userId = userManager.desconectarPorCaidaDeRed(address.getIp(), address.getPort());
            String username = userManager.obtenerNombreUsuario(userId);

            // Fallback: si MySQL no encontró la sesión por IP/Puerto (puede pasar con IPv6 o NAT local),
            // buscamos el username directamente en el LocalClientRegistry usando el OutputStream.
            if (("UsuarioDesconocido".equals(username) || username == null) && localClientRegistry != null && out != null) {
                String fallbackName = localClientRegistry.getUsernameByStream(out);
                if (fallbackName != null) {
                    username = fallbackName;
                    logger.info("Resolución de usuario por fallback (Stream) exitosa: {}", username);
                    try {
                        userManager.cerrarSesionPorUsername(username);
                    } catch (Exception e) {
                        logger.warn("No se pudo cerrar sesión en BD por username: {}", e.getMessage());
                    }
                }
            }

            if (!"UsuarioDesconocido".equals(username) && username != null) {
                logManager.registrarAccion(null, userId > 0 ? userId : -1, "DISCONNECT", "SUCCESS",
                        "Desconexión física del usuario " + username + " (" + address + ")");
                
                if (logsListSupplier != null) {
                    broadcastManager.broadcast(logsListSupplier.get());
                }

                // --- Integración P2P: limpiar registros del cliente ───────────────
                if (routingTable != null) {
                    routingTable.unregisterClient(username);
                }
                if (localClientRegistry != null) {
                    localClientRegistry.unregister(username);
                }
                if (replicationManager != null && localNodeId != null) {
                    replicationManager.propagate(
                            ReplicationEvent.clientDisconnected(localNodeId, username));
                }
                // ──────────────────────────────────────────────────────────────────
            }

            if (clientsListSupplier != null) {
                String listaTrasDesconexion = clientsListSupplier.get();
                broadcastManager.broadcast(listaTrasDesconexion);
            }

        } catch (Exception e) {
            logger.error("Error procesando desconexión física", e);
        }
    }
}
