package handler;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utilidad para lectura de líneas desde streams binarios.
 * Centraliza la lógica que antes estaba duplicada en TCPSocketServer y ClientHandler.
 *
 * Principio aplicado: DRY + Pure Fabrication (GRASP).
 */
public final class LineReader {

    private LineReader() {
        // Prevenir instanciación — clase utilitaria
    }

    /**
     * Lee una línea completa (hasta \n) desde un InputStream.
     * Ignora caracteres \r para compatibilidad con diferentes SO.
     *
     * @return la línea leída, o null si se alcanzó el fin del stream sin datos
     */
    public static String readLine(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') break;
            if (c != '\r') baos.write(c);
        }
        if (c == -1 && baos.size() == 0) return null;
        return baos.toString(StandardCharsets.UTF_8.name()).trim();
    }
}
