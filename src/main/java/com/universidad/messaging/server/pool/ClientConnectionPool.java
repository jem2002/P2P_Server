package com.universidad.messaging.server.pool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Implementación concreta del patrón Object Pool para conexiones de clientes TCP.
 *
 * <p><b>PUNTO CRÍTICO — Concurrencia:</b> Las estructuras internas son
 * inherentemente thread-safe:
 * <ul>
 *   <li>{@link ArrayBlockingQueue} para el pool de objetos disponibles — operaciones
 *       {@code poll()} y {@code offer()} son atómicas sin necesidad de {@code synchronized} externo.</li>
 *   <li>{@link ConcurrentHashMap}-backed {@code Set} para el tracking de conexiones activas.</li>
 * </ul>
 * <b>Prohibido</b> usar {@code ArrayList} con bloqueos {@code synchronized} manuales
 * para evitar deadlocks. Se apoya exclusivamente en las estructuras concurrentes nativas de Java.</p>
 *
 * <p>Pre-warming: el constructor crea las {@code maxConnections} instancias de
 * {@link PooledClientConnection} al inicio. Los objetos se crean una sola vez y se reutilizan.</p>
 */
public class ClientConnectionPool implements ObjectPool<PooledClientConnection> {
    private static final Logger logger = LoggerFactory.getLogger(ClientConnectionPool.class);

    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30;

    private final int maxConnections;
    private final ArrayBlockingQueue<PooledClientConnection> availablePool;
    private final Set<PooledClientConnection> inUsePool;
    private final ExecutorService executor;

    /**
     * Inicializa el pool con la capacidad indicada, pre-creando todas las instancias.
     *
     * @param maxConnections número máximo de conexiones simultáneas soportadas.
     */
    public ClientConnectionPool(int maxConnections) {
        this.maxConnections = maxConnections;
        this.availablePool = new ArrayBlockingQueue<>(maxConnections);
        this.inUsePool = ConcurrentHashMap.newKeySet();
        this.executor = Executors.newCachedThreadPool();

        // Pre-warming: instanciar todos los objetos PooledClientConnection y agregarlos al pool
        for (int i = 0; i < maxConnections; i++) {
            availablePool.offer(new PooledClientConnection());
        }

        logger.info("ClientConnectionPool inicializado. Capacidad máxima: {} conexiones pre-creadas.", maxConnections);
    }

    /**
     * Adquiere una {@link PooledClientConnection} disponible del pool.
     * Usa {@code queue.poll()} (no bloqueante, fail-fast).
     * Si el pool está agotado, retorna {@code null} para que el servidor rechace la conexión.
     *
     * @return una instancia reutilizable, o {@code null} si no hay disponibles.
     */
    @Override
    public synchronized PooledClientConnection acquire() {
        PooledClientConnection connection = availablePool.poll();

        if (connection == null) {
            logger.warn("Pool de conexiones agotado. Disponibles: 0/{}", maxConnections);
            return null;
        }

        inUsePool.add(connection);
        logger.debug("Conexión adquirida del pool. Disponibles: {}/{} | Activas: {}",
                availablePool.size(), maxConnections, inUsePool.size());

        return connection;
    }

    /**
     * Devuelve una {@link PooledClientConnection} al pool después de su uso.
     * Invoca {@link PooledClientConnection#reset()} para limpiar el estado
     * (cerrar socket, limpiar streams) antes de reinsertar en la cola.
     *
     * @param connection la instancia a reciclar.
     */
    @Override
    public synchronized void release(PooledClientConnection connection) {
        if (connection == null) {
            logger.warn("Intento de liberar una conexión null. Ignorando.");
            return;
        }

        // 1. Limpiar el estado del objeto (PUNTO CRÍTICO)
        connection.reset();

        // 2. Remover del set de activas
        inUsePool.remove(connection);

        // 3. Reinsertar en la cola de disponibles
        boolean reinserted = availablePool.offer(connection);

        if (reinserted) {
            logger.debug("Conexión devuelta al pool. Disponibles: {}/{} | Activas: {}",
                    availablePool.size(), maxConnections, inUsePool.size());
        } else {
            logger.warn("No se pudo reinsertar la conexión en el pool (cola llena). Esto no debería ocurrir.");
        }
    }

    /**
     * Retorna la cantidad de conexiones actualmente disponibles (no en uso).
     *
     * @return número de objetos en la cola de disponibles.
     */
    @Override
    public int getPoolSize() {
        return availablePool.size();
    }

    /**
     * Retorna la cantidad de conexiones actualmente en uso.
     *
     * @return número de objetos en el set de activos.
     */
    @Override
    public int getActiveCount() {
        return inUsePool.size();
    }

    /**
     * Envía una tarea {@link Runnable} al {@code ExecutorService} interno del pool.
     * Utilizado por el servidor TCP para lanzar un {@code ClientHandler} sobre
     * una conexión adquirida previamente.
     *
     * @param task la tarea a ejecutar (típicamente un {@code ClientHandler}).
     */
    public void submit(Runnable task) {
        executor.submit(task);
    }

    /**
     * Despacha el datagrama recibido por el servidor UDP.
     * Placeholder — la implementación UDP se completará en un hito posterior.
     *
     * @param packet el paquete UDP recibido.
     * @param socket socket para enviar respuestas si es necesario.
     */
    public void dispatchDatagram(DatagramPacket packet, DatagramSocket socket, com.universidad.messaging.server.business.MessageProcessor processor) {
        executor.submit(() -> {
            try {
                String clientAddress = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                String receivedPayload = new String(packet.getData(), 0, packet.getLength(), java.nio.charset.StandardCharsets.UTF_8);
                logger.debug("Datagrama UDP recibido de {}: {}", clientAddress, receivedPayload);
                
                String responseJson = processor.processMessage(clientAddress, receivedPayload);
                
                byte[] responseBytes = responseJson.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
                socket.send(responsePacket);
                logger.debug("Respuesta UDP (ACK) enviada a {}", clientAddress);
            } catch (Exception e) {
                logger.error("Error procesando datagrama UDP", e);
            }
        });
    }

    /**
     * Inicia el apagado ordenado (Graceful Shutdown) del pool.
     * <ol>
     *   <li>Señaliza al {@code ExecutorService} que no acepte nuevas tareas.</li>
     *   <li>Espera hasta {@value #SHUTDOWN_TIMEOUT_SECONDS} segundos para que las tareas activas finalicen.</li>
     *   <li>Si excede el timeout, fuerza la terminación con {@code shutdownNow()}.</li>
     * </ol>
     */
    @Override
    public void shutdown() {
        logger.info("Iniciando apagado del ClientConnectionPool...");
        executor.shutdown();

        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("El ExecutorService no finalizó en {} segundos. Forzando apagado...",
                        SHUTDOWN_TIMEOUT_SECONDS);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupción durante el apagado del ExecutorService. Forzando cierre.", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("ClientConnectionPool apagado. Disponibles restantes: {} | Activas restantes: {}",
                availablePool.size(), inUsePool.size());
    }
}
