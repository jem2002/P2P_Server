package com.universidad.messaging.api;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import io.javalin.Javalin;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentimentApi {
    private static final Logger logger = LoggerFactory.getLogger(SentimentApi.class);

    record ResenaRequest(String texto) {}
    record RespuestaSentimiento(String estado, String sentimiento, double confianza_porcentaje) {}
    
    private static Map<String, Integer> wordIndex = new HashMap<>();
    private static OrtEnvironment env;
    private static OrtSession session;
    private static final int MAX_LEN = 150;

    public static void startApi(int port) {
        try {
            logger.info("Iniciando SentimentApi (IA) en el puerto {}...", port);
            cargarRecursos();

            var app = Javalin.create(config -> {
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> it.anyHost());
                });
            }).start(port);

            app.post("/analizar", ctx -> {
                ResenaRequest resena = ctx.bodyAsClass(ResenaRequest.class);
                float[] secuenciaPad = procesarTexto(resena.texto());
                double prediccionProb = predecir(secuenciaPad);

                String etiqueta;
                double confianza;

                if (prediccionProb > 0.5) {
                    etiqueta = "Positivo";
                    confianza = prediccionProb;
                } else {
                    etiqueta = "Negativo";
                    confianza = 1.0 - prediccionProb;
                }

                double confianzaPorcentaje = Math.round((confianza * 100) * 100.0) / 100.0;
                ctx.json(new RespuestaSentimiento("exito", etiqueta, confianzaPorcentaje));
            });
            logger.info("SentimentApi inicializada correctamente en el puerto {}", port);
        } catch (Exception e) {
            logger.error("Error crítico al iniciar SentimentApi.", e);
        }
    }

    private static void cargarRecursos() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = SentimentApi.class.getResourceAsStream("/tokenizador_diccionario_esp.json")) {
            wordIndex = mapper.readValue(is, new TypeReference<Map<String, Integer>>() {});
        }

        env = OrtEnvironment.getEnvironment();
        try (InputStream is = SentimentApi.class.getResourceAsStream("/modelo_sentimientos_gru_esp.onnx")) {
            byte[] modelBytes = is.readAllBytes();
            session = env.createSession(modelBytes, new OrtSession.SessionOptions());
        }
    }

    private static float[] procesarTexto(String texto) {
        String limpio = texto.toLowerCase().replaceAll("[^a-z0-9áéíóúñ ]", "");
        String[] palabras = limpio.split("\\s+");
        float[] secuenciaPad = new float[MAX_LEN];

        int inicioSecuencia = Math.max(0, palabras.length - MAX_LEN);
        int posicionDestino = Math.max(0, MAX_LEN - palabras.length);

        for (int i = inicioSecuencia; i < palabras.length; i++) {
            Integer id = wordIndex.get(palabras[i]);
            secuenciaPad[posicionDestino++] = (id != null) ? id.floatValue() : 0.0f;
        }
        return secuenciaPad;
    }

    private static double predecir(float[] secuenciaPad) throws Exception {
        float[][] inputData = new float[1][MAX_LEN];
        inputData[0] = secuenciaPad;

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
             OrtSession.Result results = session.run(Collections.singletonMap("input_text", inputTensor))) {
            float[][] output = (float[][] ) results.get(0).getValue();
            return output[0][0];
        }
    }
}
