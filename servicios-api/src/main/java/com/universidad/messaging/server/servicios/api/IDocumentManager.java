package com.universidad.messaging.server.servicios.api;

import com.universidad.messaging.server.shared.schema.documentSchema.DocumentInfo;
import com.universidad.messaging.server.shared.schema.documentSchema.DownloadDetails;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface IDocumentManager {


    DownloadDetails obtenerDetallesDescarga(long documentId) throws Exception;

    void enviarDocumentoAlCliente(String encryptedPath, OutputStream out) throws Exception;

    void enviarDocumentoOriginal(long documentId, OutputStream out) throws Exception;

    void enviarDocumentoEncriptado(long documentId, OutputStream out) throws Exception;

    void enviarDocumentoHash(long documentId, OutputStream out) throws Exception;

    long obtenerTamanoHash(long documentId) throws Exception;

    long obtenerTamanoEncriptado(long documentId) throws Exception;

    List<DocumentInfo> obtenerDocumentosDisponibles();

    List<DocumentInfo> obtenerArchivosDisponibles();

    List<DocumentInfo> obtenerArchivosDisponibles(String requestingUsername);

    List<Map<String, String>> obtenerMensajesDisponibles(String requestingUsername);

    Long procesarRecepcionDocumento(InputStream redStream, String nombre, long sizeBytes,
                                    String extension, String mimeType, long ownerUserId,
                                    String ownerIp, String docType);

    List<File> listarRutasOriginales();

    List<File> listarRutasEncriptadas();

}
