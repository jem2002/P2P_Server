package DocumentService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Servicio encargado de gestionar los respaldos (exportación e importación)
 * de la base de datos MySQL mediante comandos nativos de Docker y volúmenes internos.
 */
public class DatabaseBackupManager {

    private final Properties dbProperties;
    private static final String DOCKER_CONTAINER_NAME = "messaging-db";

    // Rutas temporales dentro del contenedor Linux de Docker
    private static final String CONTAINER_BACKUP_PATH = "/tmp/backup_temp.sql";

    public DatabaseBackupManager() {
        this.dbProperties = loadProperties();
    }

    /**
     * Importa (restaura) la base de datos a partir de un archivo .sql proporcionado.
     */
    public void importarBaseDeDatos(File sqlFile) throws Exception {
        if (sqlFile == null || !sqlFile.exists() || !sqlFile.isFile()) {
            throw new IllegalArgumentException("El archivo SQL proporcionado no existe o no es válido.");
        }

        String jdbcUrl = dbProperties.getProperty("db.url");
        String username = dbProperties.getProperty("db.user");
        String password = dbProperties.getProperty("db.password");
        String databaseName = extraerNombreBaseDatos(jdbcUrl);

        // PASO 1: Copiar el archivo desde Windows hacia el contenedor Linux (docker cp)
        ejecutarComando(new ProcessBuilder(
                "docker", "cp",
                sqlFile.getAbsolutePath(),
                DOCKER_CONTAINER_NAME + ":" + CONTAINER_BACKUP_PATH
        ), "Error al copiar el archivo SQL al contenedor.");

        // PASO 2: Ejecutar la importación internamente en Linux (evita problemas de formato Windows)
        String importCmd = String.format("mysql -u%s -p%s %s < %s",
                username, password, databaseName, CONTAINER_BACKUP_PATH);

        ejecutarComando(new ProcessBuilder(
                "docker", "exec", DOCKER_CONTAINER_NAME, "sh", "-c", importCmd
        ), "Falló la importación de MySQL dentro del contenedor.");
    }

    /**
     * Exporta únicamente los datos (sentencias INSERT) a un archivo .sql.
     */
    public File exportarDatosBaseDeDatos() throws Exception {
        String jdbcUrl = dbProperties.getProperty("db.url");
        String username = dbProperties.getProperty("db.user");
        String password = dbProperties.getProperty("db.password");
        String databaseName = extraerNombreBaseDatos(jdbcUrl);

        Path exportDirPath = Paths.get("exports").toAbsolutePath();
        if (!Files.exists(exportDirPath)) {
            Files.createDirectories(exportDirPath);
        }
        File outputFile = exportDirPath.resolve("backup_datos_database.sql").toFile();

        // PASO 1: Ejecutar mysqldump agregando --insert-ignore
        String dumpCmd = String.format("mysqldump -u%s -p%s --no-create-info --insert-ignore %s > %s",
                username, password, databaseName, CONTAINER_BACKUP_PATH);

        ejecutarComando(new ProcessBuilder(
                "docker", "exec", DOCKER_CONTAINER_NAME, "sh", "-c", dumpCmd
        ), "Falló la exportación de datos en el contenedor.");

        // PASO 2: Extraer el archivo generado...
        ejecutarComando(new ProcessBuilder(
                "docker", "cp",
                DOCKER_CONTAINER_NAME + ":" + CONTAINER_BACKUP_PATH,
                outputFile.getAbsolutePath()
        ), "Error al extraer el archivo SQL del contenedor a Windows.");

        return outputFile;
    }

    /**
     * Método auxiliar para ejecutar comandos de ProcessBuilder y manejar errores limpiamente.
     */
    private void ejecutarComando(ProcessBuilder processBuilder, String mensajeError) throws Exception {
        // Combinamos la salida estándar y de error para ver detalles si falla
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            throw new RuntimeException(mensajeError + " Código: " + exitCode + ". Detalle: " + output);
        }
    }

    /**
     * Extrae el nombre de la base de datos de la URL JDBC.
     */
    private String extraerNombreBaseDatos(String jdbcUrl) {
        try {
            String cleanUrl = jdbcUrl.substring(5);
            URI uri = new URI(cleanUrl);
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) path = path.substring(1);
            if (path != null && path.contains("?")) path = path.substring(0, path.indexOf("?"));
            if (path == null || path.isBlank()) throw new IllegalArgumentException("BD no encontrada en URL.");
            return path;
        } catch (Exception e) {
            String dbName = jdbcUrl.substring(jdbcUrl.lastIndexOf("/") + 1);
            if (dbName.contains("?")) dbName = dbName.substring(0, dbName.indexOf("?"));
            return dbName;
        }
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) throw new RuntimeException("No config.properties");
            properties.load(input);
        } catch (IOException ex) {
            throw new RuntimeException("Error al leer config.properties", ex);
        }
        return properties;
    }
}