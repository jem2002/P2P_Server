package JsonSchema;

import java.time.LocalDateTime;

/**
 * Representa la información completa y detallada de un documento o mensaje
 * mapeado directamente desde la base de datos sin transformaciones.
 */
public class DocumentFullInfo {

    private final long id;
    private final String name;
    private final long sizeBytes;
    private final String extension;
    private final String mimeType;
    private final String docType;
    private final String originalPath;
    private final long ownerUserId;
    private final String ownerIp;
    private final LocalDateTime createdAt;

    public DocumentFullInfo(long id, String name, long sizeBytes, String extension, String mimeType,
                            String docType, String originalPath, long ownerUserId, String ownerIp,
                            LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.extension = extension;
        this.mimeType = mimeType;
        this.docType = docType;
        this.originalPath = originalPath;
        this.ownerUserId = ownerUserId;
        this.ownerIp = ownerIp;
        this.createdAt = createdAt;
    }

    // ── Getters puros (Inmutabilidad) ────────────────────────────────────────

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
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

    public String getDocType() {
        return docType;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public long getOwnerUserId() {
        return ownerUserId;
    }

    public String getOwnerIp() {
        return ownerIp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}