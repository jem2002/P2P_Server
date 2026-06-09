package Services;

import com.universidad.messaging.server.servicios.api.ILocalFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Clase encargada de persistir los binarios en el disco físico usando rutas relativas.
 */
public class LocalFileManager implements ILocalFileManager {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileManager.class);

    private final Path originalDir = Paths.get("./storage/original");
    private final Path encryptedDir = Paths.get("./storage/encrypted");

    public LocalFileManager() {
        crearDirectoriosSiNoExisten();
    }

    private void crearDirectoriosSiNoExisten() {
        try {
            Files.createDirectories(originalDir);
            Files.createDirectories(encryptedDir);
        } catch (IOException e) {
            logger.error("Error crítico al inicializar directorios de almacenamiento", e);
            throw new RuntimeException("No se pueden inicializar los directorios de disco", e);
        }
    }

    public String guardarOriginal(InputStream inputStream, String extension, long expectedSize) throws IOException {
        String fileName = generarNombreUnico(extension);
        Path targetLocation = originalDir.resolve(fileName);

        try (OutputStream out = new FileOutputStream(targetLocation.toFile())) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int read;
            while (totalRead < expectedSize) {
                int toRead = (int) Math.min(buffer.length, expectedSize - totalRead);
                read = inputStream.read(buffer, 0, toRead);
                if (read == -1) {
                    throw new EOFException("Conexión cerrada prematuramente durante la subida. Leidos: " + totalRead + " de " + expectedSize);
                }
                out.write(buffer, 0, read);
                totalRead += read;
            }
        }

        // MODIFICACIÓN: Guardar como ruta relativa estándar (ej: storage/original/archivo.ext)
        String relativePath = targetLocation.normalize().toString().replace("\\", "/");
        logger.debug("Archivo original guardado en ruta relativa: {}", relativePath);

        return relativePath;
    }

    public String guardarCifrado(InputStream inputStream, String extension) throws IOException {
        String fileName = generarNombreUnico(extension) + ".enc";
        Path targetLocation = encryptedDir.resolve(fileName);

        Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // MODIFICACIÓN: Guardar como ruta relativa estándar
        String relativePath = targetLocation.normalize().toString().replace("\\", "/");
        logger.debug("Archivo cifrado guardado en ruta relativa: {}", relativePath);

        return relativePath;
    }

    public InputStream leerArchivo(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("El archivo no existe en la ruta: " + filePath);
        }
        return new BufferedInputStream(new FileInputStream(file));
    }

    private String generarNombreUnico(String extension) {
        String ext = (extension != null && !extension.isEmpty()) ? extension : "";
        if (!ext.isEmpty() && !ext.startsWith(".")) {
            ext = "." + ext;
        }
        return UUID.randomUUID().toString() + ext;
    }
}