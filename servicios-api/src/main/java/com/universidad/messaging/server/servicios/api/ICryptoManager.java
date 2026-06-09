package com.universidad.messaging.server.servicios.api;

import com.universidad.messaging.server.shared.schema.documentSchema.CryptoResult;

import java.io.OutputStream;

public interface ICryptoManager {
    CryptoResult procesarArchivo(String originalPath, String targetEncryptedDir) throws Exception;

    void desencriptarYEnviarAlSocket(String encryptedPath, OutputStream networkOut) throws Exception;

}
