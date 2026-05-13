package protocolSelector;

import DocumentService.DocumentManager;
import MessageParser.BroadcastManager;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import executor.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pool.IConnectionPool;
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
     * @param router     Router del protocolo JSON
     */
    public void iniciarServidor(String protocol, int port, IConnectionPool pool,
            ThreadPoolManager threadPool, MainRouter router, BroadcastManager broadcastManager,
            TransferManager transferManager, DocumentManager documentManager, LogService.LogManager logManager) {

        if ("TCP".equalsIgnoreCase(protocol)) {
            logger.info("Iniciando servicio en modo TCP...");
            tcpServer = new TCPSocketServer(port, pool, threadPool, router, broadcastManager, transferManager,
                    documentManager, logManager);
            new Thread(tcpServer, "Thread-TCPServer").start();

        } else if ("UDP".equalsIgnoreCase(protocol)) {
            logger.info("Iniciando servicio en modo UDP...");
            udpServer = new UDPSocketServer(port, threadPool, router);
            new Thread(udpServer, "Thread-UDPServer").start();

        } else {
            logger.error("Protocolo no soportado: {}. Use TCP o UDP.", protocol);
            throw new IllegalArgumentException("Protocolo inválido");
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