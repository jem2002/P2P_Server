package com.universidad.messaging.server;

import com.universidad.messaging.server.business.DefaultMessageProcessor;
import com.universidad.messaging.server.business.MessageProcessor;
import com.universidad.messaging.server.config.AppConfig;
import com.universidad.messaging.server.network.TCPServer;
import com.universidad.messaging.server.network.UDPServer;
import com.universidad.messaging.server.pool.ClientConnectionPool;
import com.universidad.messaging.server.repository.pool.DatabaseConnectionManager;
import com.universidad.messaging.server.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    private static TCPServer tcpServer;
    private static UDPServer udpServer;

    public static void main(String[] args) {
        logger.info("Iniciando Messaging Server...");

        // 1. Capa de Configuración (Centralizada)
        AppConfig config = new AppConfig("config.properties");
        int port = config.getIntProperty("server.port", 8080);
        String protocol = config.getProperty("server.protocol", "TCP").toUpperCase();
        int maxPoolSize = config.getIntProperty("server.pool.size", 100);

        // 2. Capa de Infraestructura: Base de Datos y FileSystem
        DatabaseConnectionManager dbManager = null;
        try {
            dbManager = new DatabaseConnectionManager(config);
            logger.info("Conexión a la base de datos inicializada correctamente.");
            
            StorageService.initDirectories(config.getProperty("storage.dir", "./storage"));
        } catch (Exception e) {
            logger.error("Error crítico al inicializar infraestructura. Abortando inicio.", e);
            System.exit(1);
        }

        // 3. Capa de Aplicación (Business Logic) y Core
        MessageProcessor messageProcessor = new DefaultMessageProcessor();
        ClientConnectionPool connectionPool = new ClientConnectionPool(maxPoolSize);
        logger.info("Pool de conexiones inicializado con tamaño máximo: {}", maxPoolSize);

        // 4. Capa de Red (Presentación/Transporte)
        if ("TCP".equals(protocol)) {
            tcpServer = new TCPServer(port, connectionPool, messageProcessor);
            tcpServer.start();
        } else if ("UDP".equals(protocol)) {
            udpServer = new UDPServer(port, connectionPool, messageProcessor);
            udpServer.start();
        } else {
            logger.error("Protocolo configurado no está soportado: {}. Use TCP o UDP.", protocol);
            System.exit(1);
        }

        // 5. Cierre Limpio (Graceful Shutdown)
        registerGracefulShutdown(connectionPool, dbManager);

        // Prevenir que la app termine prematuramente
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.warn("El hilo principal fue interrumpido.", e);
            Thread.currentThread().interrupt();
        }
    }

    private static void registerGracefulShutdown(ClientConnectionPool pool, DatabaseConnectionManager dbManager) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("=========================================");
            logger.info("Iniciando secuencia de apagado seguro (Graceful Shutdown)...");
            
            if (tcpServer != null) {
                tcpServer.stop();
                logger.info("TCPServer detenido.");
            }
            if (udpServer != null) {
                udpServer.stop();
                logger.info("UDPServer detenido.");
            }
            
            if (pool != null) {
                pool.shutdown();
                logger.info("ClientConnectionPool detenido.");
            }
            
            if (dbManager != null) {
                try {
                    dbManager.close();
                    logger.info("Conexión a la base de datos cerrada de forma segura.");
                } catch (Exception e) {
                    logger.error("Error al cerrar la base de datos durante el apagado", e);
                }
            }
            
            logger.info("Servidor apagado de forma segura.");
            logger.info("=========================================");
        }, "Shutdown-Hook-Thread"));
    }
}
