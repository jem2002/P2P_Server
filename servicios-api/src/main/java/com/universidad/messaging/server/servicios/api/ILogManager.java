package com.universidad.messaging.server.servicios.api;

import com.universidad.messaging.server.shared.logs.LogEntry;

import java.util.List;

public interface ILogManager {

    void registrarAccion(Long docId, long senderId, String action, String status, String details);

    List<LogEntry> listarLogs();

}
