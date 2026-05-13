package RequestRouter;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Contrato para los manejadores de acciones del protocolo JSON.
 * Cada implementación encapsula la lógica de una acción específica.
 *
 * Principio aplicado: OCP — agregar una nueva acción = crear una nueva clase
 * que implemente esta interfaz, sin modificar MainRouter.
 */
public interface ActionHandler {

    /**
     * Procesa una acción del protocolo y retorna la respuesta JSON serializada.
     *
     * @param payload contenido del campo "payload" del mensaje entrante (puede ser null)
     * @param clientIp dirección IP del cliente que envió la solicitud
     * @return String con la respuesta JSON serializada
     * @throws Exception si ocurre un error durante el procesamiento
     */
    String handle(JsonNode payload, String clientIp) throws Exception;
}
