package console;

import api.ServerAdminAPI;
import health.ClusterHealthService;
import protocolSelector.ProtocolSelector;
import topology.RoutingTable;
import java.util.Scanner;

/**
 * Consola interactiva para administración del servidor.
 *
 * Extensión P2P: incluye comandos para ver el estado del cluster,
 * la tabla de enrutamiento y los peers conectados.
 * Los componentes de cluster son opcionales (null si cluster deshabilitado).
 */
public class InteractiveConsole implements Runnable {
    private final ServerAdminAPI adminAPI;
    private final ProtocolSelector networkServer;
    private final ClusterHealthService healthService;  // Nullable
    private final RoutingTable routingTable;            // Nullable

    public InteractiveConsole(ServerAdminAPI adminAPI, ProtocolSelector networkServer,
                              ClusterHealthService healthService, RoutingTable routingTable) {
        this.adminAPI = adminAPI;
        this.networkServer = networkServer;
        this.healthService = healthService;
        this.routingTable = routingTable;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=================================================");
        System.out.println("  MESSAGING SERVER CONSOLE ACTIVA (Escriba 'help')");
        System.out.println("=================================================");

        while (true) {
            System.out.print("server> ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "clientes":
                    adminAPI.listarClientes();
                    break;
                case "documentos":
                    adminAPI.listarDocumentos();
                    break;
                case "logs":
                    adminAPI.mostrarLogs();
                    break;

                // --- Comandos de Cluster P2P ---
                case "peers":
                    if (healthService != null) {
                        healthService.printClusterStatus();
                    } else {
                        System.out.println("Cluster P2P no habilitado. Configure cluster.enabled=true");
                    }
                    break;
                case "topology":
                    if (routingTable != null) {
                        routingTable.printTable();
                    } else {
                        System.out.println("Cluster P2P no habilitado. Configure cluster.enabled=true");
                    }
                    break;

                case "help":
                    printHelp();
                    break;
                case "stop":
                    System.out.println("Iniciando apagado seguro del servidor...");
                    networkServer.detenerServidores();
                    System.exit(0);
                    break;
                default:
                    if (!command.isEmpty()) {
                        System.out.println("Comando no reconocido. Escriba 'help'.");
                    }
            }
        }
    }

    private void printHelp() {
        System.out.println("\n--- COMANDOS DISPONIBLES ---");
        System.out.println("  clientes    — Listar usuarios registrados en BD");
        System.out.println("  documentos  — Listar documentos del sistema");
        System.out.println("  logs        — Mostrar logs de auditoría");
        if (healthService != null) {
            System.out.println("  peers       — Estado del cluster P2P (nodos vivos/caídos)");
            System.out.println("  topology    — Tabla de enrutamiento (cliente → servidor)");
        }
        System.out.println("  stop        — Apagar servidor de forma segura");
        System.out.println("  help        — Mostrar esta ayuda");
    }
}