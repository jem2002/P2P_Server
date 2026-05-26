package ports.api;

import java.io.OutputStream;

/**
 * Interfaz para el enrutador principal de solicitudes (Dependency Inversion).
 * Permite a la capa de red comunicarse con la capa de protocolo sin acoplamiento.
 */
public interface IRequestDispatcher {
    void setCurrentClientOutputStream(OutputStream out);
    String routeRequest(String rawJson, String clientIp);
    void notificarDesconexionFisica(String rawClientIp, OutputStream out);
    
    // Métodos utilitarios usados por FileTransferHandler para hacer broadcast
    String handleListDocuments();
    String handleListMessages();
    String handleListLogs();
    String handleListClients();
}
