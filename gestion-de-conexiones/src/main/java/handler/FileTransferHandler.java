package handler;

import DocumentService.DocumentManager;
import JsonSchema.DownloadMode;
import MessageParser.BroadcastManager;
import RequestRouter.MainRouter;
import RequestRouter.TransferManager;
import RequestRouter.TransferTicket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handler para transferencias de archivos (subida y descarga).
 * Usa DownloadMode enum para despacho polimórfico en vez de cadena if-else.
 *
 * Principio aplicado: Polymorphism (GRASP) — dispatch por enum en vez de String prefixes.
 */
public class FileTransferHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(FileTransferHandler.class);

    private final Socket socket;
    private final String token;
    private final TransferManager transferManager;
    private final DocumentManager documentManager;
    private final MainRouter router;
    private final BroadcastManager broadcastManager;
    private final LogService.LogManager logManager;

    public FileTransferHandler(Socket socket, String token, TransferManager transferManager,
            DocumentManager documentManager, MainRouter router, BroadcastManager broadcastManager,
            LogService.LogManager logManager) {
        this.socket = socket;
        this.token = token;
        this.transferManager = transferManager;
        this.documentManager = documentManager;
        this.router = router;
        this.broadcastManager = broadcastManager;
        this.logManager = logManager;
    }

    @Override
    public void run() {
        try (InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {

            TransferTicket ticket = transferManager.validarYConsumirTicket(token);

            if (ticket == null) {
                logger.warn("Intento de transferencia con token inválido: {}", token);
                return;
            }

            if (token.startsWith("DWN-")) {
                procesarDescarga(ticket, out);
            } else {
                procesarSubida(ticket, in, out);
            }

        } catch (Exception e) {
            logger.error("Error en transferencia de archivo para el token: {}", token, e);
        } finally {
            cerrarSocket();
        }
    }

    /**
     * Procesa una descarga usando el enum DownloadMode para dispatch polimórfico.
     * Reemplaza la cadena de if-else por prefijos de String.
     */
    private void procesarDescarga(TransferTicket ticket, OutputStream out) throws Exception {
        DownloadMode mode = DownloadMode.fromToken(token);
        long docIdToLog = 0;

        if (ticket.getMimeType() != null && ticket.getMimeType().startsWith("PEER:")) {
            logger.info("Proxy Descarga P2P. Token: {} | Target: {}", token, ticket.getMimeType());
            String[] parts = ticket.getMimeType().split(":");
            String peerHost = parts[1];
            int peerPort = Integer.parseInt(parts[2]);
            long remoteDocId = Long.parseLong(parts[3]);
            
            String realUsername = ticket.getTargetUsername() != null ? ticket.getTargetUsername() : "UsuarioDesconocido";
            ejecutarProxyDescarga(peerHost, peerPort, remoteDocId, getFormatString(mode), out, realUsername);
            
            logManager.registrarAccion(null, ticket.getOwnerUserId(), "DOWNLOAD_COMPLETE", "SUCCESS", "Descarga proxy P2P finalizada");
            broadcastManager.broadcast(router.handleListLogs());
            return;
        }

        switch (mode) {
            case ORIGINAL:
                logger.info("Enviando ARCHIVO ORIGINAL. Token: {}", token);
                docIdToLog = Long.parseLong(ticket.getMimeType());
                documentManager.enviarDocumentoOriginal(docIdToLog, out);
                break;

            case ENCRYPTED:
                logger.info("Enviando ARCHIVO ENCRIPTADO. Token: {}", token);
                docIdToLog = Long.parseLong(ticket.getMimeType());
                documentManager.enviarDocumentoEncriptado(docIdToLog, out);
                break;

            case HASH:
                logger.info("Enviando HASH. Token: {}", token);
                docIdToLog = Long.parseLong(ticket.getMimeType());
                documentManager.enviarDocumentoHash(docIdToLog, out);
                break;

            case DECRYPTED:
                logger.info("Enviando ARCHIVO DESCIFRADO. Token: {}", token);
                String encryptedPath = ticket.getMimeType();
                documentManager.enviarDocumentoAlCliente(encryptedPath, out);
                break;
        }

        logManager.registrarAccion(docIdToLog > 0 ? docIdToLog : null, ticket.getOwnerUserId(),
                "DOWNLOAD_COMPLETE", "SUCCESS", "Descarga finalizada en modo: " + mode.name());
        broadcastManager.broadcast(router.handleListLogs());
    }

    /**
     * Procesa una subida de archivo y notifica a todos los clientes.
     */
    private void procesarSubida(TransferTicket ticket, InputStream in, OutputStream out) throws Exception {
        logger.info("Recibiendo archivo pesado. Token: {}", token);
        
        String docType = "FILE";
        if (ticket.getTargetUsername() != null && !ticket.getTargetUsername().trim().isEmpty()) {
            docType = "PRIVATE_FILE_TO:" + ticket.getTargetUsername().trim();
        }
        
        boolean exito = documentManager.procesarRecepcionDocumento(
                in, ticket.getFilename(), ticket.getSizeBytes(), ticket.getExtension(),
                ticket.getMimeType(), ticket.getOwnerUserId(), ticket.getOwnerIp(), docType);

        if (exito) {
            broadcastManager.broadcast(router.handleListDocuments());
            broadcastManager.broadcast(router.handleListMessages());
            broadcastManager.broadcast(router.handleListLogs());
        }

        String status = exito ? "{\"status\":\"UPLOAD_SUCCESS\"}\n" : "{\"status\":\"UPLOAD_FAILED\"}\n";
        out.write(status.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private void cerrarSocket() {
        try {
            if (!socket.isClosed()) socket.close();
        } catch (Exception ignored) {
            // Ignorar errores al cerrar — el recurso ya no es necesario
        }
    }

    private String getFormatString(DownloadMode mode) {
        switch(mode) {
            case ORIGINAL: return "ORG";
            case ENCRYPTED: return "ENC";
            case HASH: return "HSH";
            default: return "";
        }
    }

    private void ejecutarProxyDescarga(String peerHost, int peerPort, long remoteDocId, String format, OutputStream clientOut, String downloaderUsername) throws Exception {
        try (Socket controlSocket = new Socket(peerHost, peerPort)) {
            String req = "{\"action\":\"DOWNLOAD_INIT\", \"payload\":{\"document_id\":" + remoteDocId + ", \"format\":\"" + format + "\", \"username\":\"" + downloaderUsername + "\"}}\n";
            controlSocket.getOutputStream().write(req.getBytes(StandardCharsets.UTF_8));
            controlSocket.getOutputStream().flush();
            String remoteToken = null;
            String res;
            while ((res = LineReader.readLine(controlSocket.getInputStream())) != null) {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(res);
                String action = root.has("action") ? root.get("action").asText() : "";
                
                if ("DOWNLOAD_INIT_ACK".equals(action)) {
                    if (root.has("payload") && root.get("payload").has("message")) {
                        remoteToken = root.get("payload").get("message").asText();
                    }
                    break;
                } else if ("ERROR_ACK".equals(action)) {
                    logger.error("Error devuelto por peer remoto: {}", root.path("payload").path("reason").asText());
                    break;
                }
                // Si es LIST_LOGS_ACK, LIST_CLIENTS_ACK, u otro broadcast, lo ignoramos
            }
            
            if (remoteToken != null) {
                try (Socket dataSocket = new Socket(peerHost, peerPort)) {
                    dataSocket.getOutputStream().write((remoteToken + "\n").getBytes(StandardCharsets.UTF_8));
                    dataSocket.getOutputStream().flush();
                    
                    InputStream peerIn = dataSocket.getInputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = peerIn.read(buffer)) != -1) {
                        clientOut.write(buffer, 0, read);
                    }
                    clientOut.flush();
                }
            } else {
                logger.error("Error en proxy P2P: No se obtuvo token del peer");
            }
        }
    }
}
