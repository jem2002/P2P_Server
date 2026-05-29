package RequestRouter.handlers;

import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import RequestRouter.ActionHandler;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AnalyzeMessageHandler implements ActionHandler {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzeMessageHandler.class);
    private final ResponseBuilder serializer;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiUrl;

    public AnalyzeMessageHandler(ResponseBuilder serializer, int apiPort) {
        this.serializer = serializer;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
        this.apiUrl = "http://localhost:" + apiPort + "/analizar";
    }

    @Override
    public String handle(JsonNode payload, String clientIp) {
        if (!payload.has("mensaje")) {
            return serializer.buildErrorResponse("Falta el campo 'mensaje' en el payload.");
        }
        String mensaje = payload.get("mensaje").asText();

        try {
            // Preparar el request para la API
            String requestBody = mapper.writeValueAsString(new ResenaRequest(mensaje));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode responseNode = mapper.readTree(response.body());
                String sentimiento = responseNode.get("sentimiento").asText();
                double confianza = responseNode.get("confianza_porcentaje").asDouble();
                return serializer.buildAnalyzeResponse(sentimiento, confianza);
            } else {
                logger.error("Error al consumir SentimentApi. Status: {}, Body: {}", response.statusCode(), response.body());
                return serializer.buildErrorResponse("Error al analizar el mensaje con la IA.");
            }
        } catch (Exception e) {
            logger.error("Excepción al consumir SentimentApi", e);
            return serializer.buildErrorResponse("Error interno al comunicar con SentimentApi.");
        }
    }

    private static class ResenaRequest {
        public String texto;
        public ResenaRequest(String texto) { this.texto = texto; }
    }
}
