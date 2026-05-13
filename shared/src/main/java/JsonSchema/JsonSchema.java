package JsonSchema;

public final class JsonSchema {

    private JsonSchema() {
        // Prevenir instanciación
    }

    // --- Estructura base del JSON ---
    public static final String KEY_ACTION = "action";
    public static final String KEY_PAYLOAD = "payload";

    // --- Valores permitidos para 'action' ---
    public static final String ACTION_CONNECT = "CONNECT";
    public static final String ACTION_ERROR = "ERROR";
    public static final String ACTION_UPLOAD_INIT = "UPLOAD_INIT";
    public static final String ACTION_DOWNLOAD_INIT = "DOWNLOAD_INIT";
    // Aquí agregaremos luego las demás (LIST_CLIENTS, SEND_DOC, etc.)
    public static final String ACTION_SEND_MESSAGE = "SEND_MESSAGE"; // El cliente lo envía
    public static final String ACTION_NEW_MESSAGE = "NEW_MESSAGE";   // El servidor lo retransmite
// Añade esto debajo de ACTION_CONNECT
    public static final String ACTION_LIST_CLIENTS = "LIST_CLIENTS";
    // Debajo de tus otras constantes ACTION_...
    public static final String ACTION_LIST_DOCUMENTS = "LIST_DOCUMENTS";
    public static final String ACTION_LIST_MESSAGES = "LIST_MESSAGES";
    public static final String ACTION_LIST_LOGS = "LIST_LOGS";
    // --- Llaves permitidas dentro del 'payload' ---
    public static final String PAYLOAD_USERNAME = "username";
    public static final String PAYLOAD_REASON = "reason";

    // --- Acciones P2P Inter-Servidor ---
    public static final String ACTION_PEER_HEARTBEAT = "PEER_HEARTBEAT";
    public static final String ACTION_PEER_SYNC = "PEER_SYNC";
    public static final String ACTION_PEER_REPLICATE = "PEER_REPLICATE";
    public static final String ACTION_PEER_ROUTE = "PEER_ROUTE";
    public static final String ACTION_PEER_HEALTH = "PEER_HEALTH";
}