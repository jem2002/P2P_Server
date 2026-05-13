package FileStorageService;

import FileSystemStorage.LocalFileManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

public class StorageManager {
    private static final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    private final File originalDir = new File("storage/original");
    private final File encryptedDir = new File("storage/encrypted");

    public StorageManager() {
        // Aseguramos que las carpetas existan al arrancar el servidor
        if (!originalDir.exists()) originalDir.mkdirs();
        if (!encryptedDir.exists()) encryptedDir.mkdirs();
    }

    /**
     * Lee un flujo de bytes (socket) y lo guarda en disco usando poca memoria RAM.
     */
    public File guardarArchivoDesdeSocket(InputStream socketInput, String filename) throws IOException {
        File destFile = new File(originalDir, filename);

        // Usamos try-with-resources para asegurar que el archivo se cierre al terminar
        try (FileOutputStream fileOutput = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192]; // Nuestro balde de 8 KB
            int bytesRead;

            // Leemos del socket y escribimos al disco duro simultáneamente
            while ((bytesRead = socketInput.read(buffer)) != -1) {
                fileOutput.write(buffer, 0, bytesRead);
            }
        }
        logger.info("Archivo {} guardado exitosamente en disco ({} bytes).", filename, destFile.length());
        return destFile;
    }
}