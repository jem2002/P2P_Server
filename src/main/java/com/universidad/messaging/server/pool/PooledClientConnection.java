package com.universidad.messaging.server.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Representación de una conexión de cliente que puede ser reciclada por el Object Pool.
 * Encapsula el {@link Socket} TCP y sus streams de I/O asociados.
 *
 * <p><b>PUNTO CRÍTICO — {@link #reset()}:</b> Cierra el socket anterior si está abierto,
 * vacía las referencias a {@code InputStream} y {@code OutputStream}, y establece
 * {@code isActive = false}. Esto previene:
 * <ul>
 *   <li>{@code SocketException} por intentar operar sobre un socket cerrado del ciclo anterior.</li>
 *   <li>Fuga de datos: un nuevo cliente podría leer datos residuales en memoria del cliente anterior.</li>
 * </ul>
 */
public class PooledClientConnection implements Poolable {
    private static final Logger logger = LoggerFactory.getLogger(PooledClientConnection.class);

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isActive;

    /**
     * Construye una conexión pooleable en estado inactivo, lista para ser asignada.
     */
    public PooledClientConnection() {
        this.socket = null;
        this.inputStream = null;
        this.outputStream = null;
        this.isActive = false;
    }

    /**
     * Asigna un socket de cliente TCP a esta conexión pooleable.
     * Inicializa los streams de I/O y marca la conexión como activa.
     *
     * @param socket el socket TCP del cliente recién aceptado por el servidor.
     * @throws IOException si ocurre un error al obtener los streams del socket.
     */
    public void setSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
        this.isActive = true;
    }

    /**
     * Restablece completamente el estado de esta conexión para su reutilización.
     *
     * <p><b>Orden de operaciones (crítico):</b>
     * <ol>
     *   <li>Marca como inactivo para que ningún otro hilo opere sobre los streams.</li>
     *   <li>Cierra los streams de I/O (flushing implícito en OutputStream).</li>
     *   <li>Cierra el socket TCP subyacente.</li>
     *   <li>Anula todas las referencias para permitir el GC y evitar fugas.</li>
     * </ol>
     */
    @Override
    public void reset() {
        this.isActive = false;

        // Cerrar el OutputStream primero (flush implícito)
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                logger.error("Error al cerrar OutputStream durante reset().", e);
            }
        }

        // Cerrar el InputStream
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.error("Error al cerrar InputStream durante reset().", e);
            }
        }

        // Cerrar el socket subyacente
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error al cerrar Socket durante reset().", e);
            }
        }

        // Anular referencias para evitar fugas de estado entre ciclos
        this.outputStream = null;
        this.inputStream = null;
        this.socket = null;
    }

    // ─── Accessors ───────────────────────────────────────────────────────

    public Socket getSocket() {
        return socket;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isActive() {
        return isActive;
    }
}
