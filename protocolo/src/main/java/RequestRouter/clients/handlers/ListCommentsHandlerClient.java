package RequestRouter.clients.handlers;

import ports.api.ClientActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

public class ListCommentsHandlerClient implements ClientActionHandler {


    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        return "";
    }
}
