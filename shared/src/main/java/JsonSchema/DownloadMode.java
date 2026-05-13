package JsonSchema;

/**
 * Enum que representa los modos de descarga soportados.
 * Reemplaza los magic strings "DWN-ORG-", "DWN-ENC-", "DWN-HSH-", "DWN-"
 * y elimina la cadena de if-else en FileTransferHandler.
 *
 * Principio aplicado: Polymorphism (GRASP) — dispatch por tipo en vez de Strings.
 */
public enum DownloadMode {

    ORIGINAL("DWN-ORG-"),
    ENCRYPTED("DWN-ENC-"),
    HASH("DWN-HSH-"),
    DECRYPTED("DWN-");

    private final String tokenPrefix;

    DownloadMode(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    /**
     * Determina el modo de descarga basado en el prefijo del token.
     * El orden importa: se evalúan primero los prefijos más específicos.
     */
    public static DownloadMode fromToken(String token) {
        if (token.startsWith(ORIGINAL.tokenPrefix)) return ORIGINAL;
        if (token.startsWith(ENCRYPTED.tokenPrefix)) return ENCRYPTED;
        if (token.startsWith(HASH.tokenPrefix)) return HASH;
        if (token.startsWith(DECRYPTED.tokenPrefix)) return DECRYPTED;
        throw new IllegalArgumentException("Token de descarga no reconocido: " + token);
    }
}
