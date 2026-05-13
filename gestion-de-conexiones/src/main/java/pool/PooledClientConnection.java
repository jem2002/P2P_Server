package pool;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class PooledClientConnection {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public void setSocket(Socket socket) throws Exception {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    public Socket getSocket() { return socket; }
    public InputStream getInputStream() { return inputStream; }
    public OutputStream getOutputStream() { return outputStream; }

    /**
     * Se llama cuando la conexión se devuelve al pool.
     */
    public void reset() {
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {
        } finally {
            this.socket = null;
            this.inputStream = null;
            this.outputStream = null;
        }
    }
}