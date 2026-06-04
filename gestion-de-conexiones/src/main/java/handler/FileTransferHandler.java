package handler;

import DocumentService.DocumentManager;
import JsonSchema.DownloadMode;
import ports.api.ActionHandler;
import ports.api.IBroadcastManager;
import ports.api.ITransferDispatcher;
import ports.api.TransferTicket;
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
    private final ITransferDispatcher transferManager;
    private final DocumentManager documentManager;
    private final ActionHandler listLogsHandler;
    private final ActionHandler listDocumentsHandler;
    private final IBroadcastManager broadcastManager;
    private final LogService.LogManager logManager;

    public FileTransferHandler(Socket socket, String token, ITransferDispatcher transferManager,
            DocumentManager documentManager, ActionHandler listLogsHandler,ActionHandler listDocumentsHandler, IBroadcastManager broadcastManager,
            LogService.LogManager logManager) {
        this.socket = socket;
        this.token = token;
        this.transferManager = transferManager;
        this.documentManager = documentManager;
        this.listLogsHandler = listLogsHandler;
        this.listDocumentsHandler = listDocumentsHandler;

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
        broadcastManager.broadcast(listLogsHandler.handle(null, null));
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
            broadcastManager.broadcast(listDocumentsHandler.handle(null, null));
            broadcastManager.broadcast(listLogsHandler.handle(null, null));
        }

        String status;
        if (exito) {
            status = String.format("{\"status\":\"UPLOAD_SUCCESS\",\"payload\":{\"filename\":\"%s\",\"size\":%d,\"extension\":\"%s\",\"mimeType\":\"%s\",\"username\":\"%s\"}}\n",
                    ticket.getFilename() != null ? ticket.getFilename() : "",
                    ticket.getSizeBytes(),
                    ticket.getExtension() != null ? ticket.getExtension() : "",
                    ticket.getMimeType() != null ? ticket.getMimeType() : "",
                    ticket.getOwnerUsername() != null ? ticket.getOwnerUsername() : "");
        } else {
            status = "{\"status\":\"UPLOAD_FAILED\"}\n";
        }
        
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


}
