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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MySqlUserDaoTest {

    @Mock
    private IDatabaseConnectionManager dbManager;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement selectStmt;

    @Mock
    private PreparedStatement insertStmt;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSet generatedKeysResultSet;

    @InjectMocks
    private MySqlUserDao userDao;

    @BeforeEach
    void setUp() throws Exception {
        when(dbManager.getConnection()).thenReturn(connection);
    }

    @Test
    void testObtenerORegistrarUsuario_UsuarioExiste() throws Exception {
        // Arrange
        String selectSql = "SELECT id FROM users WHERE username = ?";
        when(connection.prepareStatement(selectSql)).thenReturn(selectStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("id")).thenReturn(42L);

        // Act
        long userId = userDao.obtenerORegistrarUsuario("testUser", "127.0.0.1");

        // Assert
        assertEquals(42L, userId);
        verify(selectStmt).setString(1, "testUser");
        verify(connection, never()).prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS));
    }

    @Test
    void testObtenerORegistrarUsuario_UsuarioNoExiste() throws Exception {
        // Arrange
        String selectSql = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, ip_address) VALUES (?, ?)";

        when(connection.prepareStatement(selectSql)).thenReturn(selectStmt);
        when(selectStmt.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No existe

        when(connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)).thenReturn(insertStmt);
        when(insertStmt.getGeneratedKeys()).thenReturn(generatedKeysResultSet);
        when(generatedKeysResultSet.next()).thenReturn(true);
        when(generatedKeysResultSet.getLong(1)).thenReturn(99L);

        // Act
        long userId = userDao.obtenerORegistrarUsuario("newUser", "192.168.1.5");

        // Assert
        assertEquals(99L, userId);
        verify(selectStmt).setString(1, "newUser");
        verify(insertStmt).setString(1, "newUser");
        verify(insertStmt).setString(2, "192.168.1.5");
        verify(insertStmt).executeUpdate();
    }
}
