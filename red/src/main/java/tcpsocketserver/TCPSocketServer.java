package tcpsocketserver;

import DocumentService.DocumentManager;
import JsonSchema.JsonSchema;
import ports.api.IBroadcastManager;
import ports.api.IRequestDispatcher;
import ports.api.ITransferDispatcher;
import executor.ThreadPoolManager;
import handler.ClientHandler;
import handler.FileTransferHandler;
import handler.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.IConnectionPool;
import pool.PooledClientConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP que acepta conexiones y las despacha al handler adecuado
 * según el triage de la primera línea recibida.
 *
 * Refactorizado: usa LineReader centralizado (DRY), constante para timeout.
 */
public class TCPSocketServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(TCPSocketServer.class);
    private static final int TRIAGE_TIMEOUT_MS = 5000;

    private final int port;
    private final IConnectionPool pool;
    private final ThreadPoolManager threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;
    private final IRequestDispatcher router;
    private final IBroadcastManager broadcastManager;
    private final ITransferDispatcher transferManager;
    private final DocumentManager documentManager;
    private final LogService.LogManager logManager;

    public TCPSocketServer(int port, IConnectionPool pool, ThreadPoolManager threadPool, IRequestDispatcher router,
                           IBroadcastManager broadcastManager, ITransferDispatcher transferManager, DocumentManager documentManager,
                           LogService.LogManager logManager) {
        this.port = port;
        this.pool = pool;
        this.threadPool = threadPool;
        this.router = router;
        this.running = true;
        this.broadcastManager = broadcastManager;
        this.transferManager = transferManager;
        this.documentManager = documentManager;
        this.logManager = logManager;
    }

    public void stopServer() {
        this.running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error cerrando TCPSocketServer", e);
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            logger.info("TCPSocketServer escuchando en el puerto TCP: {}", port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                logger.debug("Nueva conexión TCP entrante desde {}", clientSocket.getRemoteSocketAddress());

                String primeraLinea = leerPrimeraLinea(clientSocket);
                if (primeraLinea == null || primeraLinea.isEmpty()) {
                    clientSocket.close();
                    continue;
                }

                if (primeraLinea.startsWith("{")) {
                    despacharConexionControl(clientSocket, primeraLinea);
                } else {
                    despacharTransferenciaArchivo(clientSocket, primeraLinea);
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error en el bucle principal de TCPSocketServer", e);
            } else {
                logger.info("TCPSocketServer detenido correctamente.");
            }
        } catch (Exception e) {
            logger.error("Error configurando la conexión del cliente.", e);
        }
    }

    /**
     * Lee la primera línea con timeout para determinar el tipo de conexión.
     */
    private String leerPrimeraLinea(Socket clientSocket) throws Exception {
        clientSocket.setSoTimeout(TRIAGE_TIMEOUT_MS);
        try {
            return LineReader.readLine(clientSocket.getInputStream());
        } catch (Exception e) {
            logger.warn("Error leyendo primera línea de {}, cerrando socket.",
                    clientSocket.getRemoteSocketAddress());
            clientSocket.close();
            return null;
        } finally {
            if (!clientSocket.isClosed()) {
                clientSocket.setSoTimeout(0);
            }
        }
    }

    /**
     * Despacha una conexión identificada como control (JSON) al pool de hilos.
     */
    private void despacharConexionControl(Socket clientSocket, String primeraLinea) throws Exception {
        logger.info("Detectada conexión de CONTROL desde {}", clientSocket.getRemoteSocketAddress());

        PooledClientConnection pooledConnection = pool.acquire();
        if (pooledConnection == null) {
            logger.warn("Rechazando conexión de control: Pool agotado.");
            // Cumplir requerimiento: "Informar al cliente que no puede aceptar la conexión"
            try (OutputStream rejectOut = clientSocket.getOutputStream()) {
                String rejection = "{\"action\":\"ERROR_ACK\",\"payload\":{\"reason\":\"Servidor lleno. Máximo de conexiones alcanzado.\"}}\n";
                rejectOut.write(rejection.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                rejectOut.flush();
            } catch (Exception e) {
                logger.debug("No se pudo enviar mensaje de rechazo al cliente: {}", e.getMessage());
            }
            clientSocket.close();
            return;
        }

        pooledConnection.setSocket(clientSocket);
        ClientHandler handler = new ClientHandler(pooledConnection, pool, router,
                broadcastManager, primeraLinea);
        threadPool.execute(handler);
    }

    /**
     * Despacha una conexión identificada como transferencia de archivo a un hilo dedicado.
     */
    private void despacharTransferenciaArchivo(Socket clientSocket, String token) {
        logger.info("Detectada conexión de ARCHIVO (Token: {}) desde {}", token,
                clientSocket.getRemoteSocketAddress());

        FileTransferHandler fileHandler = new FileTransferHandler(clientSocket, token,
                transferManager, documentManager, router.getHandler(JsonSchema.ACTION_LIST_LOGS), router.getHandler(JsonSchema.ACTION_LIST_DOCUMENTS), broadcastManager, logManager);
        new Thread(fileHandler,
                "FileTransfer-" + token.substring(0, Math.min(8, token.length()))).start();
    }
}