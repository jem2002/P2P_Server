package MySqlRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MySqlAuditLogDaoTest {

    @Mock
    private IDatabaseConnectionManager dbManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @InjectMocks
    private MySqlAuditLogDao auditLogDao;

    @BeforeEach
    void setUp() throws Exception {
        when(dbManager.getConnection()).thenReturn(connection);
    }

    @Test
    void testRegistrarLog() throws Exception {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        // Act
        auditLogDao.registrarLog(1L, 10L, 20L, "TEST_ACTION", "TCP", "SUCCESS", "Test details");

        // Assert
        verify(preparedStatement).setLong(1, 1L);
        verify(preparedStatement).setLong(2, 10L);
        verify(preparedStatement).setLong(3, 20L);
        verify(preparedStatement).setString(4, "TEST_ACTION");
        verify(preparedStatement).setString(5, "TCP");
        verify(preparedStatement).setString(6, "SUCCESS");
        verify(preparedStatement).setString(7, "Test details");
        verify(preparedStatement).setTimestamp(eq(8), any());
        verify(preparedStatement).executeUpdate();
    }
}
