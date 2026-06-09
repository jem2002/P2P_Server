package RequestRouter.files;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.universidad.messaging.server.shared.schema.documentSchema.TransferTicket;
    import com.universidad.messaging.server.protocolo.api.dispatcher.files.ITransferDispatcher;

public class TransferManager implements ITransferDispatcher {
    // Usamos ConcurrentHashMap porque muchos hilos (clientes) podrían pedir tickets al mismo tiempo
    private final Map<String, TransferTicket> pendingTransfers = new ConcurrentHashMap<>();

    public void registrarTicket(TransferTicket ticket) {
        pendingTransfers.put(ticket.getToken(), ticket);
    }

    public TransferTicket validarYConsumirTicket(String token) {
        // Devuelve el ticket y lo borra de la memoria para que no se use dos veces
        return pendingTransfers.remove(token);
    }
}