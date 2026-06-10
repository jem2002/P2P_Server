package com.universidad.messaging.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;


@Component
public class ServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    private final Properties properties;

    public ServerConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            } else {
                logger.warn("No se encontró config.properties, usando valores por defecto.");
            }
        } catch (Exception e) {
            logger.error("Error leyendo config.properties", e);
        }
    }

    public int getPort() { return Integer.parseInt(System.getProperty("socket.port", properties.getProperty("server.port", "8081"))); }
    public String getProtocol() { return System.getProperty("server.protocol", properties.getProperty("server.protocol", "TCP")); }
    public int getMaxConnections() { return Integer.parseInt(System.getProperty("server.maxConnections", properties.getProperty("server.maxConnections", "100"))); }
    public String getHost() { return System.getProperty("server.host", properties.getProperty("server.host", "localhost")); }

    public String getNodeId() { return System.getProperty("cluster.nodeId", properties.getProperty("cluster.nodeId", "auto")); }
    public int getClusterPort() { return Integer.parseInt(System.getProperty("cluster.port", properties.getProperty("cluster.port", "9090"))); }
    public long getHeartbeatIntervalMs() { return Long.parseLong(System.getProperty("cluster.heartbeatIntervalMs", properties.getProperty("cluster.heartbeatIntervalMs", "2000"))); }
    public long getFailureTimeoutMs() { return Long.parseLong(System.getProperty("cluster.failureTimeoutMs", properties.getProperty("cluster.failureTimeoutMs", "10000"))); }

    /**
     * Retorna la lista de seed nodes como array de Strings ("host:port").
     */
    public String[] getSeedNodes() {
        String seeds = System.getProperty("cluster.seedNodes", properties.getProperty("cluster.seedNodes", ""));
        if (seeds.isEmpty()) return new String[0];
        return seeds.split(",");
    }

    public int getSentimentApiPort() { return Integer.parseInt(System.getProperty("sentiment.api.port", properties.getProperty("sentiment.api.port","9000")));}

    public void overrideProperty(String key, String value) {
        System.setProperty(key, value);
    }
}