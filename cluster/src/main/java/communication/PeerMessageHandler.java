package communication;

import replication.ReplicationEventApplier;
import service.DatabaseBackupManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.universidad.messaging.server.shared.events.ReplicationEvent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handler para mensajes entrantes de servidores peer.
 *
 * Procesa las acciones del protocolo inter-servidor:
 *   - PEER_REPLICATE:       Aplica evento de replicación (clientes conectados/desconectados, mensajes, docs)
 *   - PEER_SYNC:            Sincroniza tabla de enrutamiento con datos del peer
 *   - PEER_HEALTH:          Responde con estado de salud (no-op, el heartbeat ya lo maneja)
 *   - PEER_ROUTE:           Reenvía un mensaje JSON directamente al socket de un cliente local
 *   - PEER_BROADCAST:       Retransmite un mensaje a todos los clientes locales (BroadcastManager)
 *   - PEER_LOGS_REQUEST:    Devuelve los logs locales al peer solicitante
 *   - PEER_LOGS_RESPONSE:   Almacena los logs recibidos para que LIST_PEER_LOGS los lea
 *
 * Principio aplicado: Controller (GRASP) — punto de entrada coordinador
 * para los mensajes inter-servidor.
 */
public class PeerMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(PeerMessageHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ReplicationEventApplier replicationEventApplier;
    private final DatabaseBackupManager databaseBackupManager;

    public PeerMessageHandler(ReplicationEventApplier replicationEventApplier, DatabaseBackupManager databaseBackupManager) {
        this.replicationEventApplier = replicationEventApplier;
        this.databaseBackupManager = databaseBackupManager;
    }
    
    /**
     * Gestiona una conexión peer entrante. Lee mensajes JSON línea por línea
     * hasta que la conexión se cierre.
     */
    public void handlePeerConnection(Socket peerSocket) {
        String peerAddress = peerSocket.getRemoteSocketAddress().toString();
        OutputStream peerOut = null;
        try {
            peerOut = peerSocket.getOutputStream();
            InputStream in = peerSocket.getInputStream();
            logger.info("Procesando conexión peer desde {}", peerAddress);

            ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
            int c;
            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    String line = lineBuffer.toString(StandardCharsets.UTF_8.name()).trim();
                    lineBuffer.reset();
                    if (!line.isEmpty()) {
                        processMessage(line, peerAddress, peerOut);
                    }
                } else if (c != '\r') {
                    lineBuffer.write(c);
                }
            }
        } catch (Exception e) {
            logger.debug("Conexión peer cerrada desde {}: {}", peerAddress, e.getMessage());
        } finally {
            try {
                if (!peerSocket.isClosed()) peerSocket.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Despacha un mensaje JSON al handler correspondiente según su acción.
     */
    private void processMessage(String jsonMessage, String peerAddress, OutputStream peerOut) {
        try {
            JsonNode root = mapper.readTree(jsonMessage);
            String action = root.has("action") ? root.get("action").asText() : "";
            JsonNode payload = root.has("payload") ? root.get("payload") : null;

            switch (action) {
                case "PEER_REPLICATE":
                    handleReplicate(payload);
                    break;
                case "PEER_SYNC":
                    handleSync(payload);
                    break;
                default:
                    logger.warn("Acción peer desconocida: {} desde {}", action, peerAddress);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje peer desde {}", peerAddress, e);
        }
    }

    private void handleReplicate(JsonNode payload) {
        if (payload == null) return;
        try {
            ReplicationEvent event = ReplicationEvent.fromJson(payload.toString());
            replicationEventApplier.route(event);

        } catch (Exception e) {
            logger.error("Error procesando evento de replicación", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSync(JsonNode payload) {
        if (payload == null || !payload.has("syncHost") || !payload.has("syncPort")) {
            logger.warn("Payload de PEER_SYNC no contiene syncHost o syncPort");
            return;
        }

        String syncHost = payload.get("syncHost").asText();
        int syncPort = payload.get("syncPort").asInt();
        String sourceNodeId = payload.has("sourceNodeId") ? payload.get("sourceNodeId").asText() : "unknown";

        new Thread(() -> {
            try (Socket socket = new Socket(syncHost, syncPort);
                 java.io.DataInputStream dis = new java.io.DataInputStream(socket.getInputStream())) {

                logger.info("Conectado al peer {} en {}:{} para recibir sincronización de archivos.", sourceNodeId, syncHost, syncPort);

                // Sección 1: ORIGINALES
                String sec1 = dis.readUTF();
                if ("SECTION:ORIGINALES".equals(sec1)) {
                    int count = dis.readInt();
                    File dir = new File("./storage/original");
                    if (!dir.exists()) dir.mkdirs();
                    for (int i = 0; i < count; i++) {
                        receiveFile(dis, dir, false);
                    }
                    logger.info("Recibidos {} documentos originales.", count);
                }

                // Sección 2: ENCRIPTADOS
                String sec2 = dis.readUTF();
                if ("SECTION:ENCRIPTADOS".equals(sec2)) {
                    int count = dis.readInt();
                    File dir = new File("./storage/encrypted");
                    if (!dir.exists()) dir.mkdirs();
                    for (int i = 0; i < count; i++) {
                        receiveFile(dis, dir, false);
                    }
                    logger.info("Recibidos {} documentos encriptados.", count);
                }

                // Sección 3: SQL
                String sec3 = dis.readUTF();
                if ("SECTION:SQL".equals(sec3)) {
                    int hasSql = dis.readInt();
                    if (hasSql == 1) {
                        File dir = new File("./exports");
                        if (!dir.exists()) dir.mkdirs();
                        File sqlFile = receiveFile(dis, dir, true);
                        logger.info("Script SQL de respaldo recibido exitosamente.");
                        
                        logger.info("Importando base de datos a partir del script recibido...");
                        try {
                            databaseBackupManager.importarBaseDeDatos(sqlFile);
                            logger.info("Base de datos importada exitosamente.");
                        } catch (Exception e) {
                            logger.error("Error crítico al importar la base de datos recibida: {}", e.getMessage(), e);
                        }
                    }
                }

                logger.info("Sincronización de archivos desde el peer {} completada.", sourceNodeId);

            } catch (Exception e) {
                logger.error("Error recibiendo archivos de sincronización desde {}:{}", syncHost, syncPort, e);
            }
        }, "SyncReceiver-" + sourceNodeId).start();
    }

    private File receiveFile(java.io.DataInputStream dis, File targetDir, boolean overwrite) throws java.io.IOException {
        String fileName = dis.readUTF();
        long size = dis.readLong();
        File file = new File(targetDir, fileName);
        
        if (file.exists() && !overwrite) {
            logger.info("El archivo '{}' ya existe. Omitiendo escritura en disco...", fileName);
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                remaining -= read;
            }
            return file;
        }

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            long remaining = size;
            while (remaining > 0) {
                int read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                fos.write(buffer, 0, read);
                remaining -= read;
            }
            fos.getFD().sync(); // Fuerza a que el OS escriba físicamente el archivo en disco
        }
        return file;
    }


}
