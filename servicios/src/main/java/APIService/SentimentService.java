package APIService;

import com.universidad.messaging.server.shared.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SentimentService {
    private static final Logger logger = LoggerFactory.getLogger(SentimentService.class);
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiUrl;

    public SentimentService(int apiPort) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
        this.apiUrl = "http://localhost:" + apiPort + "/analizar";
    }

    public ApiResponse process(String content){
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("Error, el contenido para analizar es nulo o vacío");
        }

        try {
            // Preparar el request para la API
            String requestBody = mapper.writeValueAsString(new SentimentRequest(content));

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
                return new ApiResponse(sentimiento, confianza);
            } else {
                logger.error("Error al consumir SentimentApi. Status: {}, Body: {}", response.statusCode(), response.body());
                return null;
            }
        } catch (Exception e) {
            logger.error("Excepción al consumir SentimentApi", e);
            return null;
        }
    }

    private static class SentimentRequest {
        public String texto;
        public SentimentRequest(String texto) { this.texto = texto; }
    }
}
