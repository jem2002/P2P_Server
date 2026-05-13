package JsonSchema;

/**
 * DTO inmutable para información de documentos.
 * Reemplaza Map<String, String> con tipado fuerte.
 */
public final class DocumentInfo {

    private final long id;
    private final String nombre;
    private final long sizeBytes;
    private final String extension;
    private final String rutaOriginal;
    private final String propietario;

    public DocumentInfo(long id, String nombre, long sizeBytes, String extension,
                        String rutaOriginal, String propietario) {
        this.id = id;
        this.nombre = nombre;
        this.sizeBytes = sizeBytes;
        this.extension = extension;
        this.rutaOriginal = rutaOriginal;
        this.propietario = propietario;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getExtension() {
        return extension;
    }

    public String getRutaOriginal() {
        return rutaOriginal;
    }

    public String getPropietario() {
        return propietario;
    }
}
