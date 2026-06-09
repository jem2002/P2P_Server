package com.universidad.messaging.server.servicios.api;

import com.universidad.messaging.server.shared.schema.userSchema.ActiveClient;

import java.util.List;

public interface IUserManager {

    long conectarUsuario(String username, String ipAddress, int port, String nodeId, String protocol) throws Exception;

    long obtenerORegistrarUsuario(String username, String ipAddress) throws Exception;

    long desconectarPorCaidaDeRed(String ipAddress, int port);

    void cerrarSesionPorUsername(String username);

    void desconectarClientesPorNodo(String nodeId);

    List<ActiveClient> obtenerClientesActivos();

    long obtenerIdUsuario(String username) throws Exception;

    String obtenerNombreUsuario(long userId);

}
