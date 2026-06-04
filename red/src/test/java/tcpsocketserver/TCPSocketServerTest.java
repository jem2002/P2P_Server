package tcpsocketserver;

import DocumentService.DocumentManager;
import executor.ThreadPoolManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pool.IConnectionPool;
import pool.PooledClientConnection;
import ports.api.IBroadcastManager;
import ports.api.IClientRequestDispatcher;
import ports.api.ITransferDispatcher;
import LogService.LogManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TCPSocketServerTest {

    private static final int TEST_PORT = 19001;

    @Mock private IConnectionPool pool;
    @Mock private ThreadPoolManager threadPool;
    @Mock private IClientRequestDispatcher router;
    @Mock private IBroadcastManager broadcastManager;
    @Mock private ITransferDispatcher transferManager;
    @Mock private DocumentManager documentManager;
    @Mock private LogManager logManager;

    private TCPSocketServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws InterruptedException {
        server = new TCPSocketServer(TEST_PORT, pool, threadPool, router, broadcastManager, transferManager, documentManager, logManager);
        serverThread = new Thread(server);
        serverThread.start();
        
        // Wait for server to bind
        Thread.sleep(200);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        server.stopServer();
        serverThread.join(2000);
    }

    @Test
    void testRechazoConexionPorPoolLleno() throws Exception {
        // Arrange: The pool returns null when acquire() is called
        when(pool.acquire()).thenReturn(null);

        // Act: Connect to the server
        try (Socket clientSocket = new Socket("127.0.0.1", TEST_PORT)) {
            clientSocket.setSoTimeout(2000);
            
            // Send a control message (JSON format) to trigger despacharConexionControl
            OutputStream out = clientSocket.getOutputStream();
            out.write("{\"action\":\"TEST\"}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String response = in.readLine();

            // Assert: Server should reject connection with JSON error message
            assertTrue(response.contains("ERROR_ACK"));
            assertTrue(response.contains("Servidor lleno"));
        }
        
        // Verify acquire was called
        verify(pool, atLeastOnce()).acquire();
        // And thread pool was never asked to execute a handler
        verify(threadPool, never()).execute(any(Runnable.class));
    }
    
    @Test
    void testAceptacionConexionExitosamente() throws Exception {
        // Arrange: The pool returns a valid connection
        PooledClientConnection mockedConn = mock(PooledClientConnection.class);
        when(pool.acquire()).thenReturn(mockedConn);

        // Act: Connect to the server
        try (Socket clientSocket = new Socket("127.0.0.1", TEST_PORT)) {
            clientSocket.setSoTimeout(2000);
            
            // Send a control message
            OutputStream out = clientSocket.getOutputStream();
            out.write("{\"action\":\"TEST\"}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();

            // Allow time for server to dispatch
            Thread.sleep(100);
        }
        
        // Assert:
        verify(pool, atLeastOnce()).acquire();
        // Since it acquired a connection, it should set the socket
        verify(mockedConn).setSocket(any(Socket.class));
        // And submit the handler to the thread pool
        verify(threadPool).execute(any(Runnable.class));
    }
}
