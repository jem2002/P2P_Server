package events.Impl;

import DocumentService.DocumentManager;
import DocumentService.DatabaseBackupManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import communication.PeerConnectionPool;
import events.NetworkEventListener;
import models.RemoteNodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Escucha eventos de la red y orquesta la conexión/desconexión
 * utilizando el pool de conexiones.
 */
public class NodeConnector implements NetworkEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NodeConnector.class);


    private final PeerConnectionPool peerConnectionPool;
    private final String identityNodeId;
    private final DocumentManager documentManager;
    private final DatabaseBackupManager databaseBackupManager;
    private static final ObjectMapper mapper = new ObjectMapper();

    public NodeConnector(PeerConnectionPool peerConnectionPool, String identityNodeId, DocumentManager documentManager, DatabaseBackupManager databaseBackupManager){
        this.peerConnectionPool = peerConnectionPool;
        this.identityNodeId = identityNodeId;
        this.documentManager = documentManager;
        this.databaseBackupManager = databaseBackupManager;
    }

    @Override
    public void onNodeJoined(RemoteNodeInfo node) {
        boolean newConnection = peerConnectionPool.connectToPeer(node);

        if(newConnection){
            synchronizeNewNode(node);
        }

    }


    private void synchronizeNewNode(RemoteNodeInfo node) {

        // Ejecutamos en un hilo aparte para dar tiempo a que el pool abra la conexión TCP
        // y para no bloquear el hilo principal/EventBus que notificó el evento.
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                String host = InetAddress.getLocalHost().getHostAddress();
                int port = serverSocket.getLocalPort();

                int maxRetries = 5;
                long delay = 200; // ms iniciales
                boolean msgSent = false;

                for (int i = 0; i < maxRetries; i++) {
                    try {
                        // Espera inicial/entre reintentos
                        Thread.sleep(delay);

                        String syncMsg = buildSyncMessage(identityNodeId, host, port);
                        peerConnectionPool.sendToPeer(node.getNodeId(), syncMsg);

                        logger.info("Sincronizacion iniciada con  '{}' (bootstrap sync en intento {})",
                                node.getNodeId(), (i + 1));
                        msgSent = true;
                        break; // ¡Éxito! Salimos del loop.

                    } catch (InterruptedException ie) {
                        logger.error("La sincronización con '{}' fue interrumpida abruptamente", node.getNodeId(), ie);
                        Thread.currentThread().interrupt(); // Restablecer el estado de interrupción
                        return;
                    } catch (Exception e) {
                        if (i == maxRetries - 1) {
                            // Último intento fallido: Log de error crítico
                            logger.error("No se pudo enviar bootstrap sync a '{}' tras {} intentos. Error: {}",
                                    node.getNodeId(), maxRetries, e.getMessage(), e);
                        } else {
                            // Intentos intermedios: Log de advertencia y aplicamos backoff exponencial
                            logger.warn("Fallo en intento {}/{} enviando sync a '{}'. Reintentando en {} ms...",
                                    (i + 1), maxRetries, node.getNodeId(), delay);
                        }

                        delay *= 2; // Duplica el tiempo de espera para el siguiente intento
                    }
                }

                if (msgSent) {
                    logger.info("Esperando conexión de '{}' en {}:{} para enviar archivos...", node.getNodeId(), host, port);
                    serverSocket.setSoTimeout(30000); // 30 segundos de timeout para evitar bloqueo eterno
                    try (Socket clientSocket = serverSocket.accept();
                         DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
                        
                        logger.info("Peer '{}' conectado. Iniciando transferencia por secciones.", node.getNodeId());

                        // Sección 1: Documentos originales
                        List<File> originales = documentManager.listarRutasOriginales();
                        dos.writeUTF("SECTION:ORIGINALES");
                        dos.writeInt(originales.size());
                        for (File f : originales) {
                            sendFile(f, dos);
                        }

                        // Sección 2: Documentos encriptados
                        List<File> encriptados = documentManager.listarRutasEncriptadas();
                        dos.writeUTF("SECTION:ENCRIPTADOS");
                        dos.writeInt(encriptados.size());
                        for (File f : encriptados) {
                            sendFile(f, dos);
                        }

                        // Sección 3: Script SQL
                        File sql = databaseBackupManager.exportarDatosBaseDeDatos();
                        dos.writeUTF("SECTION:SQL");
                        if (sql != null && sql.exists()) {
                            dos.writeInt(1);
                            sendFile(sql, dos);
                        } else {
                            dos.writeInt(0);
                        }

                        logger.info("Transferencia de archivos finalizada con éxito a '{}'.", node.getNodeId());

                    } catch (SocketTimeoutException ste) {
                        logger.error("Timeout esperando que '{}' se conecte al puerto de transferencia.", node.getNodeId());
                    } catch (Exception ex) {
                        logger.error("Error durante la transferencia de archivos a '{}': {}", node.getNodeId(), ex.getMessage(), ex);
                    }
                }

            } catch (Exception e) {
                logger.error("Error general al preparar el socket para sincronización", e);
            }
        }, "BootstrapSync-" + node.getNodeId()).start();

    }

    private void sendFile(File file, DataOutputStream dos) throws IOException {
        if (!file.isFile()) {
            dos.writeUTF(file.getName());
            dos.writeLong(0);
            return;
        }
        
        long length = file.length();
        dos.writeUTF(file.getName());
        dos.writeLong(length);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            long remaining = length;
            int bytesRead;
            
            while (remaining > 0 && (bytesRead = fis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                dos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
            
            // Si el archivo resultó ser más corto de lo que indicó file.length(), se rellena con 0s
            while (remaining > 0) {
                dos.writeByte(0);
                remaining--;
            }
        }
    }

    public static String buildSyncMessage(String sourceNodeId, String syncHost, int syncPort) {
        ObjectNode root = mapper.createObjectNode();
        root.put("action", "PEER_SYNC");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("sourceNodeId", sourceNodeId);
        payload.put("syncHost", syncHost);
        payload.put("syncPort", syncPort);
        
        root.set("payload", payload);

        return root.toString();
    }


}