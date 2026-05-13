package JsonSchema;

/**
 * Enum que tipifica los tipos de documento del sistema.
 * Reemplaza los magic strings "FILE" y "MESSAGE" dispersos en el código.
 */
public enum DocType {

    FILE("FILE"),
    MESSAGE("MESSAGE");

    private final String value;

    DocType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
