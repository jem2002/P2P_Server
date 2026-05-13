package udpsocketserver;

import RequestRouter.MainRouter;
import executor.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPSocketServer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(UDPSocketServer.class);

    private final int port;
    private final ThreadPoolManager threadPool;
    private final MainRouter router;
    private volatile boolean running;
    private DatagramSocket datagramSocket;

    public UDPSocketServer(int port, ThreadPoolManager threadPool, MainRouter router) {
        this.port = port;
        this.threadPool = threadPool;
        this.router = router;
        this.running = true;
    }

    public void stopServer() {
        this.running = false;
        if (datagramSocket != null && !datagramSocket.isClosed()) {
            datagramSocket.close();
        }
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(port);
            logger.info("udpsocketserver.UDPSocketServer escuchando en el puerto UDP: {}", port);

            byte[] buffer = new byte[65507]; // Tamaño máximo seguro de payload UDP

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet); // Bloqueante hasta recibir un paquete

                // Extraer el String del paquete JSON
                String rawJson = new String(packet.getData(), 0, packet.getLength());
                String clientIp = packet.getAddress().getHostAddress();

                // Delegar el procesamiento al ThreadPool para no bloquear la recepción de UDP
                threadPool.execute(() -> {
                    logger.debug("Procesando paquete UDP de {}", clientIp);
                    try {
                        // Enviar al router (Negocio)
                        String jsonResponse = router.routeRequest(rawJson, clientIp);

                        // Enviar respuesta de vuelta
                        byte[] responseBytes = jsonResponse.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseBytes, responseBytes.length, packet.getAddress(), packet.getPort()
                        );
                        datagramSocket.send(responsePacket);
                    } catch (Exception e) {
                        logger.error("Error procesando datagrama UDP de {}", clientIp, e);
                    }
                });
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error en el bucle principal de udpsocketserver.UDPSocketServer", e);
            } else {
                logger.info("udpsocketserver.UDPSocketServer detenido correctamente.");
            }
        }
    }
}