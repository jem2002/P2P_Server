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

    public interface LocalDocumentUploadedListener {
        void onUploaded(long docId, String filename, long sizeBytes, String extension,
                        String mimeType, String ownerUsername, String ownerIp, String docType);
    }
    private LocalDocumentUploadedListener onLocalDocumentUploaded;

    public DocumentManager(LocalFileManager fileManager, CryptoManager cryptoManager,
                           IDocumentRepository documentRepository, IUserRepository userRepository,
                           LogManager logManager) {
        this.fileManager = fileManager;
        this.cryptoManager = cryptoManager;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.logManager = logManager;
    }

    public void setOnLocalDocumentUploaded(LocalDocumentUploadedListener listener) {
        this.onLocalDocumentUploaded = listener;
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
        return obtenerArchivosDisponibles(null);
    }

    public List<DocumentInfo> obtenerArchivosDisponibles(String requestingUsername) {
        try {
            return (requestingUsername != null && !requestingUsername.isBlank())
                    ? documentRepository.listarDocumentosDisponibles(requestingUsername)
                    : documentRepository.listarDocumentosDisponibles();
        } catch (Exception e) {
            logger.error("Error al obtener la lista de archivos de la BD", e);
            return new ArrayList<>();
        }
    }

    public List<Map<String, String>> obtenerMensajesDisponibles() {
        return obtenerMensajesDisponibles(null);
    }

    /**
     * Retorna mensajes visibles para el usuario solicitante:
     *  - Todos los broadcasts (doc_type = 'MESSAGE')
     *  - Sus mensajes privados enviados y recibidos (doc_type = 'PRIVATE_TO:X')
     */
    public List<Map<String, String>> obtenerMensajesDisponibles(String requestingUsername) {
        try {
            List<DocumentInfo> crudo = (requestingUsername != null && !requestingUsername.isBlank())
                    ? documentRepository.listarMensajesDisponibles(requestingUsername)
                    : documentRepository.listarMensajesDisponibles();

            List<Map<String, String>> mensajesFinales = new ArrayList<>();

            for (DocumentInfo item : crudo) {
                // Verificar que el archivo exista en ESTE servidor antes de intentar leerlo.
                // En un cluster con MySQL compartido, la BD puede tener referencias a archivos
                // que solo existen en el sistema de archivos de otro nodo.
                String pathStr = item.getRutaOriginal();
                if (pathStr == null || !java.nio.file.Files.exists(java.nio.file.Paths.get(pathStr))) {
                    logger.debug("Mensaje omitido (archivo en otro nodo): {}", pathStr);
                    continue;
                }

                Map<String, String> msg = new HashMap<>();
                msg.put("id", String.valueOf(item.getId()));
                msg.put("propietario", item.getPropietario());
                String contenido = leerContenidoMensaje(pathStr);
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
     * Si el archivo no existe (pertenece a otro nodo), retorna un placeholder.
     */
    private String leerContenidoMensaje(String pathStr) {
        try {
            return java.nio.file.Files.readString(java.nio.file.Paths.get(pathStr));
        } catch (java.nio.file.NoSuchFileException e) {
            // Archivo en otro nodo — no es un error
            return "[Mensaje en otro servidor]";
        } catch (Exception e) {
            logger.warn("No se pudo leer el archivo de mensaje: {}", pathStr);
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
            
            if (onLocalDocumentUploaded != null) {
                String ownerUsername = userRepository.obtenerNombreUsuario(ownerUserId);
                onLocalDocumentUploaded.onUploaded(docId, nombre, sizeBytes, extension, mimeType, ownerUsername, ownerIp, docType);
            }
            
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

    public void registrarDocumentoReplicado(String nombre, long sizeBytes, String extension, String mimeType,
                                            String docType, long ownerUserId, String ownerIp,
                                            String remoteHost, int remoteClientPort, long remoteDocId) {
        try {
            String proxyPath = "PEER:" + remoteHost + ":" + remoteClientPort + ":" + remoteDocId;
            long docId = documentRepository.registrarDocumento(nombre, sizeBytes, extension, mimeType,
                    docType, proxyPath, ownerUserId, ownerIp);
            documentRepository.registrarHashDocumento(docId, "SHA256", proxyPath);
            documentRepository.registrarCifradoDocumento(docId, "AES256", proxyPath, "SERVER_STATIC_KEY");
            logger.info("Documento replicado registrado. ID local: {} -> Remoto: {}", docId, proxyPath);
        } catch (Exception e) {
            logger.error("Error registrando documento replicado", e);
        }
    }
}