package MessageParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BroadcastManagerTest {

    private BroadcastManager broadcastManager;

    @BeforeEach
    void setUp() {
        broadcastManager = new BroadcastManager();
    }

    @Test
    void testBroadcastLocalOnly() throws Exception {
        OutputStream mockOut1 = mock(OutputStream.class);
        OutputStream mockOut2 = mock(OutputStream.class);

        broadcastManager.addStream(mockOut1);
        broadcastManager.addStream(mockOut2);

        broadcastManager.broadcastLocalOnly("{\"msg\":\"test\"}");

        // Broadcast is asynchronous via ExecutorService, so we sleep briefly to allow it to execute
        Thread.sleep(100);

        verify(mockOut1).write(any(byte[].class));
        verify(mockOut1).flush();
        
        verify(mockOut2).write(any(byte[].class));
        verify(mockOut2).flush();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testBroadcastPropagatesToPeers() throws Exception {
        OutputStream mockOut = mock(OutputStream.class);
        Consumer<String> mockHook = mock(Consumer.class);

        broadcastManager.addStream(mockOut);
        broadcastManager.setFederatedHook(mockHook);

        broadcastManager.broadcast("{\"msg\":\"federated\"}");

        Thread.sleep(100);

        verify(mockOut).write(any(byte[].class));
        verify(mockHook).accept("{\"msg\":\"federated\"}");
    }
    
    @Test
    void testBroadcastRemovesFailingStream() throws Exception {
        OutputStream mockOut = mock(OutputStream.class);
        
        // Arrange to throw an exception when writing
        doThrow(new java.io.IOException("Socket closed")).when(mockOut).write(any(byte[].class));

        broadcastManager.addStream(mockOut);

        // Act
        broadcastManager.broadcastLocalOnly("{\"msg\":\"test\"}");
        Thread.sleep(100);

        // Try broadcasting again. If the stream was removed, it shouldn't be interacted with again.
        broadcastManager.broadcastLocalOnly("{\"msg\":\"test2\"}");
        Thread.sleep(100);

        // Assert write was called only once (for the first attempt)
        verify(mockOut, times(1)).write(any(byte[].class));
    }
}
