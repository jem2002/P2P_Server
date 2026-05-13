/**
 * Prueba de Humo para verificar la capa de Protocolo (Parser, Router y Serializer).
 */

import LogService.LogManager;
import CryptoService.CryptoManager;
import DocumentService.DocumentManager;
import FileStorageService.StorageManager;

import MySqlRepository.*;
import RequestRouter.MainRouter;
import UserService.UserManager;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtocolTester {
 /*   private static final Logger logger = LoggerFactory.getLogger(ProtocolTester.class);

    public static void main(String[] args) {
        logger.info("=== INICIANDO SMOKE TEST DE PROTOCOLO ===");

        try {
            // 1. Inicializar BD y Capa de Negocio (Igual que en la prueba anterior)
            logger.info("Inicializando BD y Capa de Servicios...");
            DatabaseInitializer.initializeSchema();
            MySqlDao dao = new MySqlDao();

            UserManager userManager = new UserManager(dao);
            DocumentManager documentManager = new DocumentManager(
                    new StorageManager(), new CryptoManager(), dao, new LogManager(dao));

            // 2. Inicializar la Capa de Protocolo
            logger.info("Inicializando MainRouter (Capa de Protocolo)...");
            MainRouter router = new MainRouter(userManager, documentManager);

            String clientIp = "192.168.1.100";

            // --- PRUEBA 1: JSON Válido y Acción Soportada (CONNECT) ---
            logger.info("--------------------------------------------------");
            logger.info("PRUEBA 1: Simulando petición de conexión válida");
            String peticionConnect = "{\"action\":\"CONNECT\", \"payload\":{\"username\":\"estudiante_01\"}}";
            logger.info(" -> Recibido desde Red : {}", peticionConnect);

            String respuestaConnect = router.routeRequest(peticionConnect, clientIp);
            logger.info(" <- Respuesta generada : {}", respuestaConnect);

            // --- PRUEBA 2: JSON Válido pero falta el Payload (Error de negocio) ---
            logger.info("--------------------------------------------------");
            logger.info("PRUEBA 2: Simulando petición CONNECT sin username");
            String peticionIncompleta = "{\"action\":\"CONNECT\", \"payload\":{}}";
            logger.info(" -> Recibido desde Red : {}", peticionIncompleta);

            String respuestaIncompleta = router.routeRequest(peticionIncompleta, clientIp);
            logger.info(" <- Respuesta generada : {}", respuestaIncompleta);

            // --- PRUEBA 3: JSON Malformado (Error de sintaxis) ---
            logger.info("--------------------------------------------------");
            logger.info("PRUEBA 3: Simulando JSON malformado (llave sin cerrar)");
            String peticionRota = "{\"action\":\"CONNECT\", \"payload\":{\"username\":\"hacker\"}"; // Falta la llave final
            logger.info(" -> Recibido desde Red : {}", peticionRota);

            String respuestaRota = router.routeRequest(peticionRota, clientIp);
            logger.info(" <- Respuesta generada : {}", respuestaRota);

            // --- PRUEBA 4: Acción no soportada ---
            logger.info("--------------------------------------------------");
            logger.info("PRUEBA 4: Simulando una acción que no existe");
            String peticionDesconocida = "{\"action\":\"HACKEAR_NASA\", \"payload\":{}}";
            logger.info(" -> Recibido desde Red : {}", peticionDesconocida);

            String respuestaDesconocida = router.routeRequest(peticionDesconocida, clientIp);
            logger.info(" <- Respuesta generada : {}", respuestaDesconocida);

            logger.info("--------------------------------------------------");
            logger.info("=== TEST DE PROTOCOLO FINALIZADO EXITOSAMENTE ===");

        } catch (Exception e) {
            logger.error("Error crítico en la prueba de protocolo", e);
        } finally {
            DatabaseConnectionManager.getInstance().close();
        }
    }

  */
}