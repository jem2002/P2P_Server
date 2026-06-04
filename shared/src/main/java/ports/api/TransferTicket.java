package ports.api;

import JsonSchema.FileAction;

/**
 * Ticket de transferencia de archivos.
 * Representa la autorización para una subida o descarga.
 */
public class TransferTicket {

    private final String token;
    private final String filename;
    private final long sizeBytes;
    private final String extension;
    private final String mimeType;
    private final long ownerUserId;
    private final String ownerIp;
    private final String targetUsername;
    private final String ownerUsername;
    private final FileAction fileAction; // Cambiado a minúscula por convención

    public TransferTicket(String token, String filename, long sizeBytes, String extension,
                          String mimeType, long ownerUserId, String ownerIp, String targetUsername, String ownerUsername) {
        this.token = token;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.extension = extension;
        this.mimeType = mimeType;
        this.ownerUserId = ownerUserId;
        this.ownerIp = ownerIp;
        this.targetUsername = targetUsername;
        this.ownerUsername = ownerUsername;

        // Inicializamos el enum automáticamente al construir el objeto
        this.fileAction = parseFileAction(token);
    }

    public TransferTicket(String token, String filename, long sizeBytes, String extension,
                          String mimeType, long ownerUserId, String ownerIp, String targetUsername) {
        this(token, filename, sizeBytes, extension, mimeType, ownerUserId, ownerIp, targetUsername, null);
    }

    public TransferTicket(String token, String filename, long sizeBytes, String extension,
                          String mimeType, long ownerUserId, String ownerIp) {
        this(token, filename, sizeBytes, extension, mimeType, ownerUserId, ownerIp, null, null);
    }

    /**
     * Método helper privado para extraer el prefijo base ("UPL" o "DWN")
     * y convertirlo en el Enum correspondiente.
     */
    private FileAction parseFileAction(String token) {
        if (token == null) {
            throw new IllegalArgumentException("El token no puede ser nulo.");
        }

        // Buscamos el primer guion
        int firstDash = token.indexOf("-");

        // Si hay guion, cortamos hasta ahí. Si no, usamos el token completo.
        String rawAction = (firstDash != -1) ? token.substring(0, firstDash) : token;

        try {
            return FileAction.valueOf(rawAction.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token inválido. No se reconoce la acción: " + rawAction);
        }
    }

    /**
     * Retorna la acción principal (DWN o UPL) detectada en el token.
     */
    public FileAction getFileAction() {
        return fileAction;
    }

    /**
     * Extrae el modo de descarga completo (Ej: "DWN-ORG").
     */
    public String getDownloadMode() {
        if (token == null || token.length() <= 36) {
            return token;
        }
        String prefix = token.substring(0, token.length() - 36);
        if (prefix.endsWith("-")) {
            return prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    public String getToken() {
        return token;
    }

    public String getFilename() {
        return filename;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getExtension() {
        return extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }

    public String getOwnerIp() {
        return ownerIp;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }
}