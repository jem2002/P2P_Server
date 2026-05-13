package console;

import api.ServerAdminAPI;
import protocolSelector.ProtocolSelector;
import java.util.Scanner;

public class InteractiveConsole implements Runnable {
    private final ServerAdminAPI adminAPI;
    private final ProtocolSelector networkServer;

    public InteractiveConsole(ServerAdminAPI adminAPI, ProtocolSelector networkServer) {
        this.adminAPI = adminAPI;
        this.networkServer = networkServer;
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
                case "help":
                    System.out.println("Comandos: clientes, documentos, logs, stop");
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
}