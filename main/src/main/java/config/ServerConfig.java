package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

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

    public int getPort() { return Integer.parseInt(properties.getProperty("server.port", "8080")); }
    public String getProtocol() { return properties.getProperty("server.protocol", "TCP"); }
    public int getMaxConnections() { return Integer.parseInt(properties.getProperty("server.maxConnections", "100")); }
    public String getHost() { return properties.getProperty("server.host", "localhost"); }

    // --- Configuración del Cluster P2P ---
    public boolean isClusterEnabled() { return Boolean.parseBoolean(properties.getProperty("cluster.enabled", "false")); }
    public String getNodeId() { return properties.getProperty("cluster.nodeId", "auto"); }
    public int getClusterPort() { return Integer.parseInt(properties.getProperty("cluster.port", "9090")); }
    public long getHeartbeatIntervalMs() { return Long.parseLong(properties.getProperty("cluster.heartbeatIntervalMs", "2000")); }
    public long getFailureTimeoutMs() { return Long.parseLong(properties.getProperty("cluster.failureTimeoutMs", "10000")); }

    /**
     * Retorna la lista de seed nodes como array de Strings ("host:port").
     */
    public String[] getSeedNodes() {
        String seeds = properties.getProperty("cluster.seedNodes", "");
        if (seeds.isEmpty()) return new String[0];
        return seeds.split(",");
    }
}