package Services;

import com.universidad.messaging.server.shared.logs.LogEntry;
import com.universidad.messaging.server.persistencia.api.IAuditLogRepository;
import com.universidad.messaging.server.servicios.api.ILogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Servicio de dominio para la bitácora de auditoría.
 *
 * Principios aplicados:
 *   - DIP: depende de IAuditLogRepository (abstracción).
 *   - Clean Code: reemplazado e.printStackTrace() por logger.error().
 */
public class LogManager implements ILogManager {

    private static final Logger logger = LoggerFactory.getLogger(LogManager.class);

    private final IAuditLogRepository auditLogRepository;

    public LogManager(IAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrarAccion(Long docId, long senderId, String action, String status, String details) {
        if (senderId <= 0) {
            logger.debug("Omitiendo registro de log de auditoria para accion de sistema (ID<=0): {} - {}", action, details);
            return;
        }
        auditLogRepository.registrarLog(docId, senderId, null, action, "TCP", status, details);
    }

    public List<LogEntry> listarLogs() {
        try {
            return auditLogRepository.listarLogs();
        } catch (Exception e) {
            logger.error("Error al listar logs de auditoría", e);
            return Collections.emptyList();
        }
    }
}