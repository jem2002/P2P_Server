package com.universidad.messaging.server.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class StorageService {
    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    public static void initDirectories(String baseDir) {
        File dirOriginal = new File(baseDir + File.separator + "original");
        File dirEncrypted = new File(baseDir + File.separator + "encrypted");

        try {
            if (!dirOriginal.exists() && !dirOriginal.mkdirs()) {
                throw new IOException("No se pudo crear el directorio de storage: " + dirOriginal.getAbsolutePath());
            }
            if (!dirEncrypted.exists() && !dirEncrypted.mkdirs()) {
                throw new IOException("No se pudo crear el directorio de storage cifrado: " + dirEncrypted.getAbsolutePath());
            }
            logger.info("Directorios de almacenamiento verificados e inicializados en: {}", baseDir);
        } catch (IOException e) {
            logger.error("Error crítico al inicializar los directorios de disco. Abortando inicio.", e);
            throw new RuntimeException("Fallo al inicializar almacenamiento", e);
        }
    }
}
