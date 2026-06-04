package ports.api;

import java.io.InputStream;
import java.io.OutputStream;

public interface IFileRequestDispatcher {
    void routeAndProcess(TransferTicket ticket, InputStream in, OutputStream out) throws Exception;
}
