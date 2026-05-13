package MessageParser;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envoltorio inmutable para un mensaje JSON parseado.
 *
 * Refactorizado: campos ahora son final (inmutabilidad).
 */
public class MessageWrapper {

    private final String action;
    private final JsonNode payload;

    public MessageWrapper(String action, JsonNode payload) {
        this.action = action;
        this.payload = payload;
    }

    public String getAction() {
        return action;
    }

    public JsonNode getPayload() {
        return payload;
    }
}