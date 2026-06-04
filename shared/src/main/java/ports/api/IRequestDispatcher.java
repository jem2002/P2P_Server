package ports.api;
import java.io.OutputStream;

/**
 * Interfaz para el enrutador principal de solicitudes (Dependency Inversion).
 * Permite a la capa de red comunicarse con la capa de protocolo sin acoplamiento.
 */
public interface IRequestDispatcher {
    String routeRequest(String rawJson, String clientIp);
    ActionHandler getHandler(String action);
}
