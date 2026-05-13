package MessageParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonInputParser {
    private static final Logger logger = LoggerFactory.getLogger(JsonInputParser.class);
    private final ObjectMapper mapper;

    public JsonInputParser() {
        this.mapper = new ObjectMapper();
    }

    public MessageWrapper parse(String jsonInput) {
        try {
            // Leer el árbol JSON
            JsonNode rootNode = mapper.readTree(jsonInput);

            if (rootNode == null || !rootNode.has("action")) {
                logger.warn("JSON inválido o sin acción: {}", jsonInput);
                return null;
            }

            String action = rootNode.get("action").asText();
            JsonNode payload = rootNode.has("payload") ? rootNode.get("payload") : null;

            return new MessageWrapper(action, payload);

        } catch (Exception e) {
            logger.error("Error de sintaxis JSON con Jackson: {}", jsonInput, e);
            return null;
        }
    }
}