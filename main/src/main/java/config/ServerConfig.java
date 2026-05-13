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
}