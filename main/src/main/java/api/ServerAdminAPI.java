package api;

import JsonSchema.DocumentInfo;
import JsonSchema.UserRecord;
import MySqlRepository.IDocumentRepository;
import MySqlRepository.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Expone las capacidades del servidor para el Administrador del sistema.
 *
 * Principio aplicado: DIP — depende de IUserRepository e IDocumentRepository,
 * no de la implementación concreta MySqlDao.
 */
public class ServerAdminAPI {

    private static final Logger logger = LoggerFactory.getLogger(ServerAdminAPI.class);

    private final IUserRepository userRepository;
    private final IDocumentRepository documentRepository;

    public ServerAdminAPI(IUserRepository userRepository, IDocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
    }

    public void listarClientes() {
        System.out.println("\n--- CLIENTES REGISTRADOS EN LA BASE DE DATOS ---");
        System.out.printf("%-5s | %-20s | %-15s | %-20s%n", "ID", "USUARIO", "IP", "FECHA REGISTRO");
        System.out.println("---------------------------------------------------------------------------");
        try {
            List<UserRecord> usuarios = userRepository.listarUsuariosRegistrados();
            if (usuarios.isEmpty()) {
                System.out.println("No hay usuarios registrados aún.");
            } else {
                for (UserRecord u : usuarios) {
                    System.out.printf("%-5s | %-20s | %-15s | %-20s%n",
                            u.getId(), u.getUsername(), u.getIp(), u.getCreatedAt());
                }
            }
        } catch (Exception e) {
            logger.error("Error al listar clientes desde la API", e);
        }
        System.out.println("---------------------------------------------------------------------------");
    }

    public void listarDocumentos() {
        System.out.println("\n--- DOCUMENTOS EN EL SISTEMA ---");
        try {
            List<DocumentInfo> docs = documentRepository.listarDocumentosDisponibles();
            if (docs.isEmpty()) {
                System.out.println("No hay documentos almacenados.");
            } else {
                for (DocumentInfo doc : docs) {
                    System.out.printf("ID: %s | Archivo: %s | Tamaño: %s bytes | Propietario: %s%n",
                            doc.getId(), doc.getNombre(), doc.getSizeBytes(), doc.getPropietario());
                }
            }
        } catch (Exception e) {
            logger.error("Error listando documentos", e);
        }
    }

    public void mostrarLogs() {
        System.out.println("\n--- LOGS RECIENTES ---");
        System.out.println("Mostrando los últimos 10 eventos del sistema (Auditoría)...");
    }
}