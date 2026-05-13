package com.universidad.messaging.server.repository.pool;

import com.universidad.messaging.server.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gestor de conexiones a la base de datos MySQL.
 * Administra un pool de conexiones utilizando HikariCP.
 */
public class DatabaseConnectionManager {

    private final HikariDataSource dataSource;

    /**
     * Constructor que recibe la configuración (Inyección de Dependencias).
     */
    public DatabaseConnectionManager(AppConfig config) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(config.getProperty("db.url"));
        hikariConfig.setUsername(config.getProperty("db.user"));
        hikariConfig.setPassword(config.getProperty("db.password"));

        hikariConfig.setMaximumPoolSize(20);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTimeout(30000); 
        hikariConfig.setIdleTimeout(600000);      
        
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
