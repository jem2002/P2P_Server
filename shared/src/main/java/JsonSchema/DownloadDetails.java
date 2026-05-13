package JsonSchema;

/**
 * DTO inmutable con los detalles necesarios para iniciar una descarga.
 * Reemplaza Map<String, String> con tipado fuerte.
 */
public final class DownloadDetails {

    private final String nombre;
    private final long sizeBytes;
    private final String rutaCifrada;

    public DownloadDetails(String nombre, long sizeBytes, String rutaCifrada) {
        this.nombre = nombre;
        this.sizeBytes = sizeBytes;
        this.rutaCifrada = rutaCifrada;
    }

    public String getNombre() {
        return nombre;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getRutaCifrada() {
        return rutaCifrada;
    }
}
