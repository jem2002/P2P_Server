package com.universidad.messaging.server.api.config;

import Services.CryptoManager;
import Services.DocumentManager;
import Services.LocalFileManager;
import Services.LogManager;
import com.universidad.messaging.server.persistencia.api.IAuditLogRepository;
import com.universidad.messaging.server.persistencia.api.IDocumentRepository;
import com.universidad.messaging.server.persistencia.api.IUserRepository;

import com.universidad.messaging.server.servicios.api.ICryptoManager;
import com.universidad.messaging.server.servicios.api.IDocumentManager;
import com.universidad.messaging.server.servicios.api.ILocalFileManager;
import com.universidad.messaging.server.servicios.api.ILogManager;
import com.universidad.messaging.server.shared.utils.EncryptionUtils.EncryptionUtils;
import com.universidad.messaging.server.shared.utils.EncryptionUtils.IEncryptionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {


    @Bean
    public ILogManager logManager(IAuditLogRepository logRepository){
        return new LogManager(logRepository);
    }


    @Bean
    public ILocalFileManager localFileManager(){
        return new LocalFileManager();
    }


    @Bean
    public IEncryptionUtils encryptionUtils(){
        return new EncryptionUtils();
    }

    @Bean
    public ICryptoManager cryptoManager(IEncryptionUtils encryptionUtils)
    {
        return new CryptoManager(encryptionUtils);
    }



    @Bean
    public IDocumentManager documentManager(ILocalFileManager localFileManager, ICryptoManager cryptoManager,
                                            IDocumentRepository documentRepository,
                                            IUserRepository userRepository,
                                            ILogManager logManager
    ){

        return new DocumentManager(localFileManager,cryptoManager, documentRepository, userRepository, logManager);

    }

}
