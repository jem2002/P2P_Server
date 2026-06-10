package com.universidad.messaging.server.config;

import java.util.Scanner;

/**
 * Asistente de configuración interactivo que se ejecuta al inicio del servidor.
 *
 * Pregunta al operador si es el primer nodo de la red o un nodo adicional y,
 * en base a esa respuesta, solicita los parámetros necesarios mostrando una
 * descripción breve y el nombre exacto de la propiedad entre paréntesis.
 *
 * Los valores recogidos se inyectan en {@link ServerConfig} antes de que
 * la aplicación arranque, anulando los valores del archivo config.properties.
 *
 * Flujo:
 * ¿Es el primer nodo?
 * SÍ  → aplica valores fijos: port=8081, nodeId=node-1, clusterPort=9090, seeds=""
 * NO  → pide cada parámetro al usuario
 */
public class NodeSetupWizard {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";
    private static final String CYAN  = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW= "\u001B[33m";
    private static final String RED   = "\u001B[31m";
    private static final String DIM   = "\u001B[2m";

    /**
     * Ejecuta el asistente y aplica los valores recogidos al {@code config}.
     *
     * @param config instancia de ServerConfig ya cargada desde el archivo.
     */
    public static void run(ServerConfig config) {
        Scanner scanner = new Scanner(System.in);

        printBanner();

        System.out.print(BOLD + "¿Es este el PRIMER nodo de la red? (s/n): " + RESET);
        String resp = scanner.nextLine().trim().toLowerCase();

        String defaultHost = "localhost";
        try {
            defaultHost = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {}

        if (resp.equals("s") || resp.equals("si") || resp.equals("sí") || resp.equals("y") || resp.equals("yes")) {
            // ── Primer nodo: valores predeterminados ──────────────────────
            System.out.println();
            System.out.println(CYAN + "Configurando primer nodo..." + RESET);
            String host = ask(scanner, "Dirección IP / Host (server.host)", defaultHost);

            System.setProperty("server.host",      host);
            System.setProperty("server.port",      "8080"); // Puerto HTTP Spring
            System.setProperty("socket.port",      "8081"); // Puerto Socket P2P
            System.setProperty("cluster.nodeId",   "node-1");
            System.setProperty("cluster.port",     "9090");
            System.setProperty("cluster.seedNodes","");

            System.out.println();
            System.out.println(GREEN + "✔  Configuración de nodo primario aplicada:" + RESET);
            printSummary(host, "8080 (HTTP) / 8081 (Socket)", "node-1", "9090", "(ninguno — este es el primer nodo)");

        } else {
            // ── Nodo adicional: pedir cada parámetro ──────────────────────
            System.out.println();
            System.out.println(CYAN + "Introduce los parámetros para este nodo:" + RESET);
            System.out.println(DIM + "(Presiona Enter para conservar el valor actual entre [corchetes])" + RESET);
            System.out.println();

            String host        = ask(scanner, "Dirección IP / Host (server.host)", defaultHost);
            String httpPort    = ask(scanner, "Puerto HTTP API (server.port)", "8080");
            String socketPort  = ask(scanner, "Puerto de sockets (socket.port)", "8081");
            String nodeId      = ask(scanner, "ID único del nodo   (cluster.nodeId)", config.getNodeId());
            String clusterPort = ask(scanner, "Puerto del cluster  (cluster.port)",   String.valueOf(config.getClusterPort()));
            String seedNodes   = askSeeds(scanner, config);

            System.setProperty("server.host",       host);
            System.setProperty("server.port",       httpPort); // Para Spring Boot
            System.setProperty("socket.port",       socketPort); // Para ServerConfig
            System.setProperty("cluster.nodeId",    nodeId);
            System.setProperty("cluster.port",      clusterPort);
            System.setProperty("cluster.seedNodes", seedNodes);

            System.out.println();
            System.out.println(GREEN + "✔  Configuración aplicada:" + RESET);
            printSummary(host, httpPort + " (HTTP) / " + socketPort + " (Socket)", nodeId, clusterPort,
                    seedNodes.isEmpty() ? "(ninguno)" : seedNodes);
        }

        System.out.println();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static String ask(Scanner scanner, String label, String defaultValue) {
        System.out.print("  " + BOLD + label + RESET
                + " [" + YELLOW + defaultValue + RESET + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    /**
     * Pide los seed nodes con una descripción especial:
     * acepta varios separados por coma o uno por línea con "+" para añadir más.
     */
    private static String askSeeds(Scanner scanner, ServerConfig config) {
        String defaultSeeds = String.join(",", config.getSeedNodes());
        System.out.println("  " + BOLD + "Nodos semilla del cluster (cluster.seedNodes)" + RESET);
        System.out.println("  " + DIM + "Formato: host:puerto  — separa varios con coma  (ej: localhost:9090,localhost:9091)" + RESET);
        System.out.print("  Semillas" + " [" + YELLOW + (defaultSeeds.isEmpty() ? "ninguno" : defaultSeeds) + RESET + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultSeeds : input;
    }

    private static void printSummary(String host, String port, String nodeId,
                                     String clusterPort, String seeds) {
        System.out.println(DIM + "  ┌────────────────────────────────────────────");
        System.out.println("  │  server.host       = " + host);
        System.out.println("  │  server.port       = " + port);
        System.out.println("  │  cluster.nodeId    = " + nodeId);
        System.out.println("  │  cluster.port      = " + clusterPort);
        System.out.println("  │  cluster.seedNodes = " + seeds);
        System.out.println("  └────────────────────────────────────────────" + RESET);
    }

    private static void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD
                + "╔══════════════════════════════════════════════════════╗\n"
                + "║         CONFIGURACIÓN DEL NODO P2P                  ║\n"
                + "╚══════════════════════════════════════════════════════╝"
                + RESET);
        System.out.println();
    }
}