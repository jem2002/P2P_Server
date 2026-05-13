package CryptoService;

import DocumentService.CryptoResult;
import EncryptionUtils.IEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * Servicio de criptografía para cifrado/descifrado y hashing de archivos.
 *
 * Principios aplicados:
 *   - DIP: recibe IEncryptionUtils por constructor (antes creaba new EncryptionUtils() internamente).
 *   - Clean Code: eliminados imports duplicados, secretKey inicializada de forma segura.
 */
public class CryptoManager {

    private static final Logger logger = LoggerFactory.getLogger(CryptoManager.class);

    private final IEncryptionUtils cryptoUtils;
    private final SecretKey secretKey;

    /**
     * Constructor con inyección de dependencia.
     * La llave se genera una sola vez al crear la instancia.
     */
    public CryptoManager(IEncryptionUtils cryptoUtils) {
        this.cryptoUtils = cryptoUtils;
        try {
            this.secretKey = cryptoUtils.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Error inicializando motor AES desde Shared", e);
        }
    }

    public CryptoResult procesarArchivo(String originalPath, String targetEncryptedDir) throws Exception {
        logger.info("Iniciando Hash y Cifrado en un solo pase para: {}", originalPath);

        MessageDigest digest = cryptoUtils.getHashDigest();
        Cipher cipher = cryptoUtils.getEncryptionCipher(secretKey);

        Path sourcePath = Paths.get(originalPath);
        Path finalEncryptedPath = Paths.get(targetEncryptedDir, UUID.randomUUID().toString() + ".enc");

        try (InputStream is = Files.newInputStream(sourcePath);
             DigestInputStream dis = new DigestInputStream(is, digest);
             OutputStream os = Files.newOutputStream(finalEncryptedPath);
             CipherOutputStream cos = new CipherOutputStream(os, cipher)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }

        String hashResult = cryptoUtils.bytesToHex(digest.digest());

        logger.debug("Procesamiento exitoso. Hash: {}", hashResult);
        return new CryptoResult(hashResult, finalEncryptedPath.toAbsolutePath().toString());
    }

    public void desencriptarYEnviarAlSocket(String encryptedPath, OutputStream networkOut) throws Exception {
        logger.info("Iniciando descifrado y envío (Streaming) para: {}", encryptedPath);

        Cipher cipher = cryptoUtils.getDecryptionCipher(secretKey);
        Path sourcePath = Paths.get(encryptedPath);

        try (InputStream is = Files.newInputStream(sourcePath);
             javax.crypto.CipherInputStream cis = new javax.crypto.CipherInputStream(is, cipher)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = cis.read(buffer)) != -1) {
                networkOut.write(buffer, 0, bytesRead);
            }
            networkOut.flush();
        }
        logger.debug("Envío del archivo completado exitosamente.");
    }
}