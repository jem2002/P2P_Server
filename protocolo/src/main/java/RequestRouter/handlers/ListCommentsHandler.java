package RequestRouter.handlers;

import ports.api.ActionHandler;
import com.fasterxml.jackson.databind.JsonNode;

public class ListCommentsHandler implements ActionHandler {


    @Override
    public String handle(JsonNode payload, String clientIp) throws Exception {
        return "";
    }
}
