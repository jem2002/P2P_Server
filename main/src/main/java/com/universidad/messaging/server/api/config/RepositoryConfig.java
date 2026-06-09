package com.universidad.messaging.server.api.config;


import MySqlRepository.MySqlAuditLogDao;
import MySqlRepository.MySqlCommentDao;
import MySqlRepository.MySqlDocumentDao;
import MySqlRepository.MySqlUserDao;
import MySqlRepository.db.DatabaseConnectionManager;
import MySqlRepository.db.IDatabaseConnectionManager;
import com.universidad.messaging.server.persistencia.api.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfig {

    @Bean
    public IDatabaseConnectionManager databaseConnectionManager() {
        return DatabaseConnectionManager.getInstance();
    }

    @Bean
    public IUserRepository userRepository(IDatabaseConnectionManager connectionManager) {
        return new MySqlUserDao(connectionManager);
    }

    @Bean
    public IDocumentRepository documentRepository(IDatabaseConnectionManager connectionManager) {
        return new MySqlDocumentDao(connectionManager);
    }

    @Bean
    public IAuditLogRepository auditLogRepository(IDatabaseConnectionManager connectionManager) {
        return new MySqlAuditLogDao(connectionManager);
    }

    @Bean
    public ICommentRepository commentRepository(IDatabaseConnectionManager connectionManager) {
        return new MySqlCommentDao(connectionManager);
    }


}
