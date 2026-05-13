package MySqlRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Utilidad para leer archivos SQL y ejecutar sus sentencias una por una.
 */
public class SqlScriptRunner {
    private static final Logger logger = LoggerFactory.getLogger(SqlScriptRunner.class);

    public static void runScript(Connection conn, String resourcePath) throws Exception {
        logger.info("Leyendo script SQL: {}", resourcePath);

        InputStream is = SqlScriptRunner.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("No se encontró el archivo: " + resourcePath);
        }

        String content = new BufferedReader(new InputStreamReader(is))
                .lines()
                .collect(Collectors.joining("\n"));

        // Eliminar comentarios de bloque y de línea simple para evitar errores de parseo
        String cleanedContent = content.replaceAll("--.*", "").replaceAll("/\\*.*?\\*/", "");

        // Separar por punto y coma (;)
        String[] statements = cleanedContent.split(";");

        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    logger.debug("Ejecutando: {}...", trimmedSql.substring(0, Math.min(trimmedSql.length(), 50)));
                    stmt.execute(trimmedSql);
                }
            }
        }
        logger.info("Script {} ejecutado correctamente.", resourcePath);
    }
}