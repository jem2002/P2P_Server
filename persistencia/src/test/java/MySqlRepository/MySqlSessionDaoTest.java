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
public class MySqlSessionDaoTest {

    @Mock
    private IDatabaseConnectionManager dbManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @InjectMocks
    private MySqlSessionDao sessionDao;

    @BeforeEach
    void setUp() throws Exception {
        when(dbManager.getConnection()).thenReturn(connection);
    }

    @Test
    void testCerrarSesionPorUsername() throws Exception {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        // Act
        sessionDao.cerrarSesionPorUsername("user1");

        // Assert
        verify(preparedStatement).setString(1, "user1");
        verify(preparedStatement).executeUpdate();
    }
}
