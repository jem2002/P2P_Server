package handler;

import RequestRouter.MainRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.IConnectionPool;
import pool.PooledClientConnection;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import MessageParser.BroadcastManager;

/**
 * Handler para conexiones de control (protocolo JSON persistente).
 * Gestiona el ciclo de vida de una conexión de cliente.
 *
 * Refactorizado: usa LineReader centralizado (DRY), constructor simplificado.
 * Extensión P2P: notifica al router el OutputStream del cliente antes de cada
 * petición para que ConnectHandler pueda registrarlo en LocalClientRegistry.
 */
public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final PooledClientConnection connection;
    private final IConnectionPool pool;
    private final MainRouter router;
    private final BroadcastManager broadcastManager;
    private final String primeraLinea;

    public ClientHandler(PooledClientConnection connection, IConnectionPool pool, MainRouter router,
                         BroadcastManager broadcastManager, String primeraLinea) {
        this.connection = connection;
        this.pool = pool;
        this.router = router;
        this.broadcastManager = broadcastManager;
        this.primeraLinea = primeraLinea;
    }

    @Override
    public void run() {
        String clientIp = "UNKNOWN";
        OutputStream out = null;
        try {
            clientIp = connection.getSocket().getRemoteSocketAddress().toString();
            InputStream in = connection.getInputStream();
            out = connection.getOutputStream();

            broadcastManager.addStream(out);
            logger.info("Atendiendo conexión de CONTROL desde {}", clientIp);

            // Registrar el OutputStream actual en el router (para ConnectHandler → LocalClientRegistry)
            router.setCurrentClientOutputStream(out);

            // 1. Procesar la primera línea que ya leímos en el Triage
            enviarRespuestaJson(primeraLinea, out, clientIp);

            // 2. Bucle para seguir recibiendo comandos JSON
            while (true) {
                String linea = LineReader.readLine(in);
                if (linea == null) {
                    break;
                }

                if (linea.isEmpty()) {
                    continue;
                }

                if (linea.startsWith("{")) {
                    enviarRespuestaJson(linea, out, clientIp);
                } else {
                    logger.warn("Recibido dato no-JSON en conexión de CONTROL desde {}: {}", clientIp, linea);
                }
            }

        } catch (Exception e) {
            logger.error("Conexión de CONTROL perdida con {}", clientIp);
        } finally {
            router.notificarDesconexionFisica(clientIp, out);
            if (out != null) broadcastManager.removeStream(out);
            pool.release(connection);
        }
    }

    private void enviarRespuestaJson(String json, OutputStream out, String clientIp) throws Exception {
        // Actualizar el OutputStream antes de routear (por si es CONNECT)
        router.setCurrentClientOutputStream(out);
        String jsonResponse = router.routeRequest(json, clientIp);
        out.write((jsonResponse + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}