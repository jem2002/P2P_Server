package console;

import api.ServerAdminAPI;
import discovery.MemberEntry;
import discovery.MembershipList;
import health.ClusterHealthService;
import protocolSelector.ProtocolSelector;
import topology.RoutingTable;
import java.util.Scanner;

/**
 * Consola interactiva para administración del servidor.
 *
 * Extensión P2P: incluye comandos para:
 *   - peers      → Estado del cluster (vivos, sospechosos, caídos)
 *   - topology   → Tabla de enrutamiento cliente→servidor
 *   - peer-info  → Información detallada de todos los servidores conocidos
 *
 * Los componentes de cluster son opcionales (null si cluster deshabilitado).
 *
 * Requerimiento cumplido: "Adicionar servicios para mostrar la información
 * y los logs de otros servidores."
 */
public class InteractiveConsole implements Runnable {
    private final ServerAdminAPI adminAPI;
    private final ProtocolSelector networkServer;
    private final ClusterHealthService healthService;   // Nullable
    private final RoutingTable routingTable;             // Nullable
    private final MembershipList membershipList;         // Nullable

    public InteractiveConsole(ServerAdminAPI adminAPI, ProtocolSelector networkServer,
                              ClusterHealthService healthService, RoutingTable routingTable,
                              MembershipList membershipList) {
        this.adminAPI = adminAPI;
        this.networkServer = networkServer;
        this.healthService = healthService;
        this.routingTable = routingTable;
        this.membershipList = membershipList;
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

                case "peer-info":
                    // Requerimiento: "Detección de servidores amigos: listar servidores conectados y desconectados"
                    if (membershipList != null) {
                        printPeerInfo();
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

    /**
     * Imprime información detallada de todos los servidores conocidos.
     * Requerimiento: "Detección de servidores amigos: listar servidores conectados y desconectados."
     */
    private void printPeerInfo() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║          INFORMACIÓN DE SERVIDORES PEERS                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ %-15s | %-20s | %-10s | %-12s ║%n",
                "NODE ID", "DIRECCIÓN", "ESTADO", "HEARTBEAT");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");

        boolean hayPeers = false;
        for (MemberEntry entry : membershipList.getAllEntries()) {
            hayPeers = true;
            long ms = entry.getTimeSinceLastHeartbeatMs();
            String heartbeat = ms < 1000 ? ms + "ms" : (ms / 1000) + "s";
            System.out.printf("║ %-15s | %-20s | %-10s | %-12s ║%n",
                    entry.getNodeInfo().getNodeId(),
                    entry.getNodeInfo().getHost() + ":" + entry.getNodeInfo().getClusterPort(),
                    entry.getState().name(),
                    heartbeat);
        }

        if (!hayPeers) {
            System.out.println("║  Sin servidores peers conocidos aún.                            ║");
        }
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
    }

    private void printHelp() {
        System.out.println("\n--- COMANDOS DISPONIBLES ---");
        System.out.println("  clientes    — Listar usuarios registrados en BD");
        System.out.println("  documentos  — Listar documentos del sistema");
        System.out.println("  logs        — Mostrar logs de auditoría");
        if (healthService != null) {
            System.out.println("  peers       — Estado del cluster P2P (resumen: vivos/sospechosos/caídos)");
            System.out.println("  topology    — Tabla de enrutamiento (cliente → servidor)");
            System.out.println("  peer-info   — Información detallada de todos los servidores conocidos");
        }
        System.out.println("  stop        — Apagar servidor de forma segura");
        System.out.println("  help        — Mostrar esta ayuda");
        System.out.println();
        System.out.println("--- PROTOCOLO CLIENTE (acciones JSON disponibles) ---");
        System.out.println("  CONNECT, LIST_CLIENTS, LIST_DOCUMENTS, LIST_MESSAGES, LIST_LOGS");
        System.out.println("  SEND_MESSAGE (campo 'targetUsername' opcional — null = broadcast a todos)");
        if (healthService != null) {
            System.out.println("  LIST_PEER_INFO  — Info de todos los servidores desde el cliente");
            System.out.println("  LIST_PEER_LOGS  — Logs de todos los servidores desde el cliente");
        }
    }
}