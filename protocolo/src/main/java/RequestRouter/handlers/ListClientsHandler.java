package RequestRouter.handlers;

import JsonSchema.ActiveClient;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import RequestRouter.ActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Maneja la acción LIST_CLIENTS: retorna la lista de clientes activos.
 */
public class ListClientsHandler implements ActionHandler {

    private final UserManager userManager;
    private final ResponseBuilder serializer;

    public ListClientsHandler(UserManager userManager, ResponseBuilder serializer) {
        this.userManager = userManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        List<ActiveClient> activos = userManager.obtenerClientesActivos();
        List<Map<String, String>> mapped = new ArrayList<>();
        for (ActiveClient c : activos) {
            Map<String, String> item = new HashMap<>();
            item.put("username", c.getUsername());
            item.put("ip", c.getIp());
            item.put("fecha_inicio", c.getConnectedAt());
            mapped.add(item);
        }
        return serializer.buildListResponse(JsonSchema.ACTION_LIST_CLIENTS, mapped, "clientes");
    }
}
