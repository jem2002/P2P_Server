package EncryptionUtils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.MessageDigest;

/**
 * Implementación concreta de la utilería criptográfica.
 */
public class EncryptionUtils implements IEncryptionUtils {

    @Override
    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
        keyGen.init(KEY_SIZE);
        return keyGen.generateKey();
    }

    @Override
    public Cipher getEncryptionCipher(SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher;
    }

    @Override
    public Cipher getDecryptionCipher(SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher;
    }

    @Override
    public MessageDigest getHashDigest() throws Exception {
        return MessageDigest.getInstance(ALGORITHM_HASH);
    }

    @Override
    public String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}