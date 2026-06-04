package ports.api;

/**
 * Interfaz para el enrutador principal de solicitudes (Dependency Inversion).
 * Permite a la capa de red comunicarse con la capa de protocolo sin acoplamiento.
 */
public interface IClientRequestDispatcher {
    String routeRequest(String rawJson, String clientIp);
    ClientActionHandler getHandler(String action);
}
