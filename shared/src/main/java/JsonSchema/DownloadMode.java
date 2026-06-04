package JsonSchema;

import ports.api.TransferTicket; // Asegúrate de importar el paquete correcto

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
     * Determina el modo de descarga evaluando directamente el ticket de transferencia.
     * El orden importa: se evalúan primero los prefijos más específicos.
     * * @param ticket El ticket de transferencia que contiene el token.
     * @return El modo de descarga correspondiente.
     */
    public static DownloadMode fromTicket(TransferTicket ticket) {
        if (ticket == null || ticket.getToken() == null) {
            throw new IllegalArgumentException("El ticket o el token no pueden ser nulos.");
        }

        String token = ticket.getToken();

        if (token.startsWith(ORIGINAL.tokenPrefix)) return ORIGINAL;
        if (token.startsWith(ENCRYPTED.tokenPrefix)) return ENCRYPTED;
        if (token.startsWith(HASH.tokenPrefix)) return HASH;
        if (token.startsWith(DECRYPTED.tokenPrefix)) return DECRYPTED;

        throw new IllegalArgumentException("Modo de descarga no reconocido para el token: " + token);
    }
}