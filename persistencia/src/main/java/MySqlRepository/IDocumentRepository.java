package MySqlRepository;

import JsonSchema.DocumentInfo;
import JsonSchema.DownloadDetails;

import java.util.List;

/**
 * Contrato de persistencia para la entidad Documento.
 *
 * Principio aplicado: ISP — DocumentManager solo ve operaciones de documentos.
 */
public interface IDocumentRepository {

    long registrarDocumento(String name, long sizeBytes, String extension, String mimeType,
                            String docType, String originalPath, long ownerUserId, String ownerIp) throws Exception;

    void registrarHashDocumento(long documentId, String algorithm, String hashValue) throws Exception;

    void registrarCifradoDocumento(long documentId, String algorithm, String encryptedPath,
                                   String keyReference) throws Exception;

    DownloadDetails obtenerDetallesDescarga(long documentId) throws Exception;

    String obtenerRutaOriginal(long documentId) throws Exception;

    String obtenerHashValue(long documentId) throws Exception;

    List<DocumentInfo> listarArchivosDisponibles() throws Exception;

    List<DocumentInfo> listarMensajesDisponibles() throws Exception;

    List<DocumentInfo> listarDocumentosDisponibles() throws Exception;
}
