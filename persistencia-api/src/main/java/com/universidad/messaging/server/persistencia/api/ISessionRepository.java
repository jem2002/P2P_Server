package com.universidad.messaging.server.persistencia.api;

import java.sql.SQLException;

/**
 * Contrato de persistencia para sesiones de conexión de clientes.
 *
 * Principio aplicado: ISP — separado de usuarios, documentos y logs.
 */
public interface ISessionRepository {

    long registrarSesionActiva(long userId, String ipAddress, int port, String protocol, String nodeId) throws SQLException;

    void cerrarSesionActiva(long sessionId) throws SQLException;

    long cerrarSesionPorIpYPuerto(String ipAddress, int port) throws Exception;

    void cerrarSesionPorUsername(String username) throws Exception;

    void cerrarSesionesPorNodo(String nodeId) throws Exception;

    void limpiarConexionesMuertas();
}
