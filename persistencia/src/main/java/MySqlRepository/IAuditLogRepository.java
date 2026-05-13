package MySqlRepository;

import JsonSchema.LogEntry;

import java.util.List;

/**
 * Contrato de persistencia para la bitácora de auditoría.
 *
 * Principio aplicado: ISP — LogManager solo ve operaciones de logs.
 */
public interface IAuditLogRepository {

    void registrarLog(Long documentId, long senderId, Long receiverId,
                      String action, String protocol, String status, String details);

    List<LogEntry> listarLogs() throws Exception;
}
