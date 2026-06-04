package ports.api;

import java.io.InputStream;
import java.io.OutputStream;

public interface FileActionHandler {
    void process(TransferTicket ticket, InputStream in, OutputStream out) throws Exception;
}
