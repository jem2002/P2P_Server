package handler;


import com.universidad.messaging.server.protocolo.api.dispatcher.files.ITransferDispatcher;
import ports.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import com.universidad.messaging.server.protocolo.api.dispatcher.files.IFileRequestDispatcher;



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
    private final IFileRequestDispatcher router;

    public FileTransferHandler(Socket socket, String token, ITransferDispatcher transferManager,
                               IFileRequestDispatcher router) {
        this.socket = socket;
        this.token = token;
        this.transferManager = transferManager;
        this.router = router;


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

            router.routeAndProcess(ticket, in, out);

        } catch (Exception e) {
            logger.error("Error en transferencia de archivo para el token: {}", token, e);
        } finally {
            cerrarSocket();
        }
    }


    private void cerrarSocket() {
        try {
            if (!socket.isClosed()) socket.close();
        } catch (Exception ignored) {
            // Ignorar errores al cerrar — el recurso ya no es necesario
        }
    }


}
