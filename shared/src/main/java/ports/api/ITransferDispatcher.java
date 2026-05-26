package ports.api;

/**
 * Interfaz para el gestor de transferencias (Dependency Inversion).
 */
public interface ITransferDispatcher {
    TransferTicket validarYConsumirTicket(String token);
    void registrarTicket(TransferTicket ticket);
}
