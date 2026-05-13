package EncryptionUtils;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.MessageDigest;
public interface IEncryptionUtils {
    String ALGORITHM_AES = "AES";
    String CIPHER_TRANSFORMATION = "AES"; // En producción idealmente "AES/CBC/PKCS5Padding"
    String ALGORITHM_HASH = "SHA-256";
    int KEY_SIZE = 256;

    /**
     * Genera una llave simétrica AES estándar.
     */
    SecretKey generateKey() throws Exception;

    /**
     * Configura el motor de cifrado para encriptar.
     */
    Cipher getEncryptionCipher(SecretKey key) throws Exception;

    /**
     * Configura el motor de cifrado para desencriptar.
     */
    Cipher getDecryptionCipher(SecretKey key) throws Exception;

    /**
     * Obtiene el motor de Hash estándar de la arquitectura.
     */
    MessageDigest getHashDigest() throws Exception;

    /**
     * Utilidad transversal para convertir bytes de hash a formato Hexadecimal legible.
     */
    String bytesToHex(byte[] bytes);
}