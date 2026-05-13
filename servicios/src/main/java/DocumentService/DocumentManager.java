package DocumentService;

import CryptoService.CryptoManager;
import FileSystemStorage.LocalFileManager;
import JsonSchema.DocumentInfo;
import JsonSchema.DownloadDetails;
import LogService.LogManager;
import MySqlRepository.IDocumentRepository;
import MySqlRepository.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de dominio para la gestión de documentos y mensajes.
 *
 * Principios aplicados:
 *   - DIP: depende de IDocumentRepository e IUserRepository (abstracciones).
 *   - SRP: coordina el flujo de procesamiento de documentos.
 */
public class DocumentManager {

    private static final Logger logger = LoggerFactory.getLogger(DocumentManager.class);
    private static final String ENCRYPTED_STORAGE_DIR = "./storage/encrypted";

    private final LocalFileManager fileManager;
    private final CryptoManager cryptoManager;
    private final IDocumentRepository documentRepository;
    private final IUserRepository userRepository;
    private final LogManager logManager;

    public DocumentManager(LocalFileManager fileManager, CryptoManager cryptoManager,
                           IDocumentRepository documentRepository, IUserRepository userRepository,
                           LogManager logManager) {
        this.fileManager = fileManager;
        this.cryptoManager = cryptoManager;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.logManager = logManager;
    }

    public DownloadDetails obtenerDetallesDescarga(long documentId) throws Exception {
        return documentRepository.obtenerDetallesDescarga(documentId);
    }

    public void enviarDocumentoAlCliente(String encryptedPath, OutputStream out) throws Exception {
        cryptoManager.desencriptarYEnviarAlSocket(encryptedPath, out);
    }

    public void enviarDocumentoOriginal(long documentId, OutputStream out) throws Exception {
        String path = documentRepository.obtenerRutaOriginal(documentId);
        Files.copy(Paths.get(path), out);
        out.flush();
    }

    public void enviarDocumentoEncriptado(long documentId, OutputStream out) throws Exception {
        DownloadDetails detalles = documentRepository.obtenerDetallesDescarga(documentId);
        String path = detalles.getRutaCifrada();
        Files.copy(Paths.get(path), out);
        out.flush();
    }

    public void enviarDocumentoHash(long documentId, OutputStream out) throws Exception {
        String hash = documentRepository.obtenerHashValue(documentId);
        if (hash == null) hash = "NO_HASH";
        out.write(hash.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public long obtenerTamanoHash(long documentId) throws Exception {
        String hash = documentRepository.obtenerHashValue(documentId);
        if (hash == null) return "NO_HASH".getBytes(StandardCharsets.UTF_8).length;
        return hash.getBytes(StandardCharsets.UTF_8).length;
    }

    public long obtenerTamanoEncriptado(long documentId) throws Exception {
        DownloadDetails detalles = documentRepository.obtenerDetallesDescarga(documentId);
        String path = detalles.getRutaCifrada();
        if (path != null) {
            return Files.size(Paths.get(path));
        }
        return 0;
    }

    public List<DocumentInfo> obtenerDocumentosDisponibles() {
        try {
            return documentRepository.listarDocumentosDisponibles();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de documentos de la BD", e);
            return new ArrayList<>();
        }
    }

    public List<DocumentInfo> obtenerArchivosDisponibles() {
        try {
            return documentRepository.listarArchivosDisponibles();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de archivos de la BD", e);
            return new ArrayList<>();
        }
    }

    public List<Map<String, String>> obtenerMensajesDisponibles() {
        try {
            List<DocumentInfo> crudo = documentRepository.listarMensajesDisponibles();
            List<Map<String, String>> mensajesFinales = new ArrayList<>();

            for (DocumentInfo item : crudo) {
                Map<String, String> msg = new HashMap<>();
                msg.put("id", String.valueOf(item.getId()));
                msg.put("propietario", item.getPropietario());

                String contenido = leerContenidoMensaje(item.getRutaOriginal());
                msg.put("contenido", contenido);
                mensajesFinales.add(msg);
            }

            return mensajesFinales;
        } catch (Exception e) {
            logger.error("Error al obtener la lista de mensajes de la BD", e);
            return new ArrayList<>();
        }
    }

    /**
     * Extrae el contenido de texto de un archivo de mensaje.
     * Método extraído para cumplir SRP (separar lectura de archivo del loop de mapeo).
     */
    private String leerContenidoMensaje(String pathStr) {
        try {
            return Files.readString(Paths.get(pathStr));
        } catch (Exception e) {
            logger.error("No se pudo leer el archivo .txt en {}", pathStr, e);
            return "[Error al leer el contenido del mensaje]";
        }
    }

    public boolean procesarRecepcionDocumento(InputStream redStream, String nombre, long sizeBytes,
                                              String extension, String mimeType, long ownerUserId,
                                              String ownerIp, String docType) {
        Long docId = null;
        try {
            // 1. Delegar I/O a LocalFileManager (Streaming puro al disco)
            logger.info("1. Guardando original en disco...");
            String originalPath = fileManager.guardarOriginal(redStream, extension, sizeBytes);

            // 2. Generar Hash y Cifrar en un solo pase (Single-Pass Streaming)
            logger.info("2. Generando Hash y cifrando...");
            CryptoResult cryptoResult = cryptoManager.procesarArchivo(originalPath, ENCRYPTED_STORAGE_DIR);

            // 3. Persistencia Transaccional en MySQL
            logger.info("3. Guardando metadatos en MySQL...");
            docId = documentRepository.registrarDocumento(nombre, sizeBytes, extension, mimeType,
                    docType, originalPath, ownerUserId, ownerIp);
            documentRepository.registrarHashDocumento(docId, "SHA256", cryptoResult.getHashResult());
            documentRepository.registrarCifradoDocumento(docId, "AES256", cryptoResult.getFinalEncryptedPath(),
                    "SERVER_STATIC_KEY");

            // 4. Registrar en la bitácora de auditoría
            String username = userRepository.obtenerNombreUsuario(ownerUserId);
            logManager.registrarAccion(docId, ownerUserId, "UPLOAD_COMPLETE", "SUCCESS",
                    "Archivo subido exitosamente por: " + username);

            logger.info("¡Documento procesado al 100%! ID asignado: {}", docId);
            return true;

        } catch (Exception e) {
            logger.error("Error crítico procesando el documento.", e);
            if (ownerUserId > 0) {
                try {
                    String username = userRepository.obtenerNombreUsuario(ownerUserId);
                    logManager.registrarAccion(docId, ownerUserId, "UPLOAD_COMPLETE", "FAILED",
                            "Fallo al subir por " + username + ". Error: " + e.getMessage());
                } catch (Exception ignore) {
                    // Evitar fallo en cascada al registrar el log de error
                }
            }
            return false;
        }
    }
}