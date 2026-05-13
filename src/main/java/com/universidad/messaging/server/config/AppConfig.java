package com.universidad.messaging.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private final Properties properties;

    public AppConfig(String configFilename) {
        properties = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFilename)) {
            if (is != null) {
                properties.load(is);
            } else {
                logger.warn("Archivo '{}' no encontrado en el classpath. Usando valores por defecto.", configFilename);
            }
        } catch (IOException e) {
            logger.error("Error al cargar la configuración desde {}.", configFilename, e);
            throw new RuntimeException("Error cargando configuración", e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            logger.warn("Propiedad '{}' no es un número entero válido (valor: {}). Usando default: {}", key, val, defaultValue);
            return defaultValue;
        }
    }
}
