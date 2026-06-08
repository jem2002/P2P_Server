package protocolSelector;

import com.universidad.messaging.server.gestion.de.conexiones.api.executor.IThreadPoolManager;
import com.universidad.messaging.server.gestion.de.conexiones.api.handler.IClientHandlerFactory;
import com.universidad.messaging.server.gestion.de.conexiones.api.handler.IFileHandlerFactory;
import com.universidad.messaging.server.gestion.de.conexiones.api.pool.IConnectionPool;
import com.universidad.messaging.server.protocolo.api.broadcast.IBroadcastManager;
import com.universidad.messaging.server.protocolo.api.dispatcher.clients.IClientRequestDispatcher;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.IFileRequestDispatcher;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.ITransferDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tcpsocketserver.TCPSocketServer;
import udpsocketserver.UDPSocketServer;

public class ProtocolSelector {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolSelector.class);

    private TCPSocketServer tcpServer;
    private UDPSocketServer udpServer;

    /**
     * Inicia el servidor de red en el protocolo especificado.
     * * @param protocol "TCP" o "UDP"
     * 
     * @param port       Puerto a escuchar
     * @param pool       Gestor de conexiones de TCP
     * @param threadPool Gestor de concurrencia
     */
    public void iniciarServidor(String protocol, int port, IConnectionPool pool,
                                IThreadPoolManager threadPool, IClientRequestDispatcher clientRouter, IBroadcastManager broadcastManager,
                                ITransferDispatcher transferManager, IFileRequestDispatcher fileRouter,
                                IClientHandlerFactory clientHandlerFactory,
                                IFileHandlerFactory fileHandlerFactory) {

        boolean startTcp = "TCP".equalsIgnoreCase(protocol) || "BOTH".equalsIgnoreCase(protocol);
        boolean startUdp = "UDP".equalsIgnoreCase(protocol) || "BOTH".equalsIgnoreCase(protocol);

        if (!startTcp && !startUdp) {
            logger.error("Protocolo no soportado: {}. Use TCP, UDP o BOTH.", protocol);
            throw new IllegalArgumentException("Protocolo inválido");
        }

        if (startTcp) {
            logger.info("Iniciando servicio en modo TCP...");
            tcpServer = new TCPSocketServer(port, pool, threadPool, clientRouter, broadcastManager, transferManager,
                    fileRouter, clientHandlerFactory, fileHandlerFactory
                        );
            new Thread(tcpServer, "Thread-TCPServer").start();
        }
        
        if (startUdp) {
            logger.info("Iniciando servicio en modo UDP...");
            udpServer = new UDPSocketServer(port, threadPool, clientRouter);
            new Thread(udpServer, "Thread-UDPServer").start();
        }
    }

    public void detenerServidores() {
        if (tcpServer != null) {
            tcpServer.stopServer();
            logger.info("Se solicitó detención del servidor TCP.");
        }
        if (udpServer != null) {
            udpServer.stopServer();
            logger.info("Se solicitó detención del servidor UDP.");
        }
    }
}