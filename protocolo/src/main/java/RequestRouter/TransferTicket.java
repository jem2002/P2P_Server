package RequestRouter;

/**
 * Ticket de transferencia de archivos.
 * Representa la autorización para una subida o descarga.
 *
 * Refactorizado:
 *   - Campos privados con getters (encapsulación).
 *   - Campo 'transferData' separado de 'mimeType' para eliminar
 *     la sobrecarga semántica que violaba LSP.
 */
public class TransferTicket {

    private final String token;
    private final String filename;
    private final long sizeBytes;
    private final String extension;
    private final String mimeType;
    private final long ownerUserId;
    private final String ownerIp;

    public TransferTicket(String token, String filename, long sizeBytes, String extension,
                          String mimeType, long ownerUserId, String ownerIp) {
        this.token = token;
        this.filename = filename;
        this.sizeBytes = sizeBytes;
        this.extension = extension;
        this.mimeType = mimeType;
        this.ownerUserId = ownerUserId;
        this.ownerIp = ownerIp;
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
}