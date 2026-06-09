package com.universidad.messaging.server.shared.schema;

public final class JsonSchema {

    private JsonSchema() {
        // Prevenir instanciación
    }

    // --- Estructura base del JSON ---
    public static final String KEY_ACTION = "action";
    public static final String KEY_PAYLOAD = "payload";

    // --- Acciones cliente ↔ servidor ---
    public static final String ACTION_CONNECT           = "CONNECT";
    public static final String ACTION_DISCONNECT        = "DISCONNECT";
    public static final String ACTION_ERROR             = "ERROR";
    public static final String ACTION_UPLOAD_INIT       = "UPLOAD_INIT";
    public static final String ACTION_DOWNLOAD_INIT     = "DOWNLOAD_INIT";
    public static final String ACTION_UPLOAD_CONFIRMATION = "UPLOAD_CONFIRMATION";
    public static final String ACTION_SEND_MESSAGE      = "SEND_MESSAGE";      // Cliente envía (broadcast o dirigido)
    public static final String ACTION_NEW_MESSAGE       = "NEW_MESSAGE";       // Servidor retransmite
    public static final String ACTION_LIST_CLIENTS      = "LIST_CLIENTS";      // Clientes locales + remotos
    public static final String ACTION_LIST_DOCUMENTS    = "LIST_DOCUMENTS";
    public static final String ACTION_LIST_MESSAGES     = "LIST_MESSAGES";
    public static final String ACTION_LIST_LOGS         = "LIST_LOGS";
    public static final String ACTION_COMMENT_DOCUMENT  = "COMMENT_DOCUMENT";
    public static final String ACTION_LIST_COMMENTS     = "LIST_COMMENTS";

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



}