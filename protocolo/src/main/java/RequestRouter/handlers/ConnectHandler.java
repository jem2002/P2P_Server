package RequestRouter.handlers;

import JsonSchema.ClientAddress;
import JsonSchema.JsonSchema;
import JsonSerializer.ResponseBuilder;
import LogService.LogManager;
import RequestRouter.ActionHandler;
import UserService.UserManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Maneja la acción CONNECT: registra o recupera un usuario y crea su sesión activa.
 */
public class ConnectHandler implements ActionHandler {

    private final UserManager userManager;
    private final LogManager logManager;
    private final ResponseBuilder serializer;

    public ConnectHandler(UserManager userManager, LogManager logManager, ResponseBuilder serializer) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.serializer = serializer;
    }

    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        if (payload == null || !payload.has(JsonSchema.PAYLOAD_USERNAME)) {
            return serializer.buildErrorResponse("Falta el username.");
        }

        String username = payload.get(JsonSchema.PAYLOAD_USERNAME).asText();
        ClientAddress address = ClientAddress.parse(clientIp);

        long userId = userManager.conectarUsuario(username, address.getIp(), address.getPort());

        logManager.registrarAccion(null, userId, "CONNECT", "SUCCESS",
                "Usuario " + username + " conectado desde " + address);

        return serializer.buildSuccessResponse(JsonSchema.ACTION_CONNECT, "Usuario ID: " + userId);
    }
}
