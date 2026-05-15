package JsonSchema;

public final class JsonSchema {

    private JsonSchema() {
        // Prevenir instanciación
    }

    // --- Estructura base del JSON ---
    public static final String KEY_ACTION = "action";
    public static final String KEY_PAYLOAD = "payload";

    // --- Acciones cliente ↔ servidor ---
    public static final String ACTION_CONNECT           = "CONNECT";
    public static final String ACTION_ERROR             = "ERROR";
    public static final String ACTION_UPLOAD_INIT       = "UPLOAD_INIT";
    public static final String ACTION_DOWNLOAD_INIT     = "DOWNLOAD_INIT";
    public static final String ACTION_SEND_MESSAGE      = "SEND_MESSAGE";      // Cliente envía (broadcast o dirigido)
    public static final String ACTION_NEW_MESSAGE       = "NEW_MESSAGE";       // Servidor retransmite
    public static final String ACTION_LIST_CLIENTS      = "LIST_CLIENTS";      // Clientes locales + remotos
    public static final String ACTION_LIST_DOCUMENTS    = "LIST_DOCUMENTS";
    public static final String ACTION_LIST_MESSAGES     = "LIST_MESSAGES";
    public static final String ACTION_LIST_LOGS         = "LIST_LOGS";

    // --- Acciones de información de servidores peers ---
    public static final String ACTION_LIST_PEER_INFO    = "LIST_PEER_INFO";    // Info/estado de todos los peers
    public static final String ACTION_LIST_PEER_LOGS    = "LIST_PEER_LOGS";    // Logs de otros servidores

    // --- Notificaciones push del cluster a clientes ---
    public static final String ACTION_SERVER_JOINED     = "SERVER_JOINED";     // Un servidor se unió
    public static final String ACTION_SERVER_LEFT       = "SERVER_LEFT";       // Un servidor se desconectó
    public static final String ACTION_SERVER_SUSPECTED  = "SERVER_SUSPECTED";  // Un servidor es sospechoso

    // --- Llaves comunes en payload ---
    public static final String PAYLOAD_USERNAME         = "username";
    public static final String PAYLOAD_REASON           = "reason";
    public static final String PAYLOAD_TARGET_USERNAME  = "targetUsername";    // Destinatario específico (o null/"ALL")

    // --- Acciones P2P Inter-Servidor (protocolo interno) ---
    public static final String ACTION_PEER_HEARTBEAT    = "PEER_HEARTBEAT";
    public static final String ACTION_PEER_SYNC         = "PEER_SYNC";
    public static final String ACTION_PEER_REPLICATE    = "PEER_REPLICATE";
    public static final String ACTION_PEER_ROUTE        = "PEER_ROUTE";        // Enrutar mensaje a cliente remoto
    public static final String ACTION_PEER_HEALTH       = "PEER_HEALTH";
    public static final String ACTION_PEER_LOGS_REQUEST = "PEER_LOGS_REQUEST"; // Solicitar logs a un peer
    public static final String ACTION_PEER_LOGS_RESPONSE= "PEER_LOGS_RESPONSE";// Respuesta con logs del peer
    public static final String ACTION_PEER_BROADCAST    = "PEER_BROADCAST";    // Retransmitir broadcast a clientes locales
}