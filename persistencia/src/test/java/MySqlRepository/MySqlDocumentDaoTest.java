package MySqlRepository;

import MySqlRepository.db.IDatabaseConnectionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MySqlDocumentDaoTest {

    @Mock
    private IDatabaseConnectionManager dbManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private MySqlDocumentDao documentDao;

    @BeforeEach
    void setUp() throws Exception {
        when(dbManager.getConnection()).thenReturn(connection);
    }

    @Test
    void testRegistrarDocumento() throws Exception {
        // Arrange
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(100L);

        // Act
        long docId = documentDao.registrarDocumento("doc1.txt", 1024L, ".txt", "text/plain", "FILE", "/path", 1L, "127.0.0.1");

        // Assert
        assertEquals(100L, docId);
        verify(preparedStatement).setString(1, "doc1.txt");
        verify(preparedStatement).setLong(2, 1024L);
        verify(preparedStatement).setString(3, ".txt");
        verify(preparedStatement).setString(4, "text/plain");
        verify(preparedStatement).setString(5, "FILE");
        verify(preparedStatement).setString(6, "/path");
        verify(preparedStatement).setLong(7, 1L);
        verify(preparedStatement).setString(8, "127.0.0.1");
        verify(preparedStatement).executeUpdate();
    }
}
