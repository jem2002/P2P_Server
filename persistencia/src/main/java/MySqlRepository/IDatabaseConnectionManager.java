package MySqlRepository;

import java.sql.Connection;
import java.sql.SQLException;

public interface IDatabaseConnectionManager {
    Connection getConnection() throws SQLException;
}
