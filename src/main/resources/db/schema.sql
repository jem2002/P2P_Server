-- ============================================================
-- SCRIPT 2: Esquema Relacional — messaging_system
-- Proyecto: Messaging Server
-- Motor:    MySQL 8.0+
-- Charset:  utf8mb4 / utf8mb4_unicode_ci
-- Propósito: Crear el esquema completo con integridad
--            referencial explícita y políticas ON DELETE
--            definidas por arquitectura.
-- Ejecución:
--   mysql -u messaging_app -p messaging_system < schema.sql
-- ============================================================

USE messaging_system;

-- ============================================================
-- FASE 1: Limpieza — DROP en orden INVERSO de dependencias
-- (las tablas hijas se eliminan primero para evitar errores
--  de llaves foráneas).
-- ============================================================
DROP TABLE IF EXISTS logs;
DROP TABLE IF EXISTS encrypted_documents;
DROP TABLE IF EXISTS document_hashes;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS client_connections;
DROP TABLE IF EXISTS users;

-- ============================================================
-- FASE 2: Creación — en orden ESTRICTO de dependencias
-- (las tablas padre se crean primero).
-- ============================================================

-- ------------------------------------------------------------
-- Tabla: users
-- Descripción: Registro de usuarios del sistema de mensajería.
-- Dependencias: Ninguna (tabla raíz).
-- ------------------------------------------------------------
CREATE TABLE users (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE,
    ip_address  VARCHAR(45),                             -- IPv6 compatible (máx. 45 chars)
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: client_connections
-- Descripción: Historial de conexiones de cada usuario.
-- Dependencias: users (user_id)
-- Política ON DELETE: CASCADE — al eliminar un usuario,
--   sus registros de conexión se eliminan automáticamente.
-- ------------------------------------------------------------
CREATE TABLE client_connections (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    ip_address      VARCHAR(45)  NOT NULL,
    port            INT          NOT NULL,
    connected_at    DATETIME     NOT NULL,
    disconnected_at DATETIME,
    protocol        ENUM('TCP','UDP') NOT NULL,
    is_active       BOOLEAN      DEFAULT TRUE,

    -- Índices de rendimiento
    INDEX idx_cc_ip     (ip_address),
    INDEX idx_cc_active (is_active),

    -- Llave foránea explícita
    CONSTRAINT fk_cc_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: documents
-- Descripción: Metadatos de mensajes y archivos transferidos.
-- Dependencias: users (owner_user_id)
-- Política ON DELETE: CASCADE — al eliminar el usuario
--   propietario, sus documentos se eliminan en cascada.
-- Notas:
--   - size_bytes usa BIGINT para soportar archivos > 1 GB.
--   - original_path almacena la ruta en el filesystem del
--     servidor.
-- ------------------------------------------------------------
CREATE TABLE documents (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(500)  NOT NULL,
    size_bytes      BIGINT        NOT NULL,                    -- BIGINT: soporta archivos > 1 GB
    extension       VARCHAR(20),
    mime_type       VARCHAR(100),
    doc_type        ENUM('MESSAGE','FILE') NOT NULL,
    original_path   VARCHAR(1000),                             -- Ruta en filesystem
    owner_user_id   BIGINT        NOT NULL,
    owner_ip        VARCHAR(45),                               -- IP del cliente emisor
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,

    -- Índices de rendimiento
    INDEX idx_doc_owner (owner_user_id),
    INDEX idx_doc_type  (doc_type),

    -- Llave foránea explícita
    CONSTRAINT fk_doc_owner
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: document_hashes
-- Descripción: Hash de integridad para cada documento.
--              Relación 1:1 con documents (UNIQUE en
--              document_id).
-- Dependencias: documents (document_id)
-- Política ON DELETE: CASCADE — al eliminar el documento,
--   su hash se elimina automáticamente.
-- Notas:
--   - hash_value usa VARCHAR(128) para blindar el futuro
--     soporte de SHA-512 (128 caracteres hexadecimales).
-- ------------------------------------------------------------
CREATE TABLE document_hashes (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    document_id  BIGINT       NOT NULL UNIQUE,                 -- Relación 1:1
    algorithm    ENUM('SHA256','MD5','SHA512') NOT NULL,
    hash_value   VARCHAR(128) NOT NULL,                        -- 128 chars cubre SHA-512 hex
    generated_at DATETIME     DEFAULT CURRENT_TIMESTAMP,

    -- Llave foránea explícita
    CONSTRAINT fk_dh_document
        FOREIGN KEY (document_id) REFERENCES documents(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: encrypted_documents
-- Descripción: Metadatos de cifrado para cada documento.
--              Relación 1:1 con documents (UNIQUE en
--              document_id).
-- Dependencias: documents (document_id)
-- Política ON DELETE: CASCADE — al eliminar el documento,
--   su registro de cifrado se elimina automáticamente.
-- ------------------------------------------------------------
CREATE TABLE encrypted_documents (
    id              BIGINT        AUTO_INCREMENT PRIMARY KEY,
    document_id     BIGINT        NOT NULL UNIQUE,             -- Relación 1:1
    algorithm       ENUM('AES256','AES128') NOT NULL,
    encrypted_path  VARCHAR(1000) NOT NULL,                    -- Ruta en filesystem
    key_reference   VARCHAR(500),                              -- ID o path de la clave
    encrypted_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,

    -- Llave foránea explícita
    CONSTRAINT fk_ed_document
        FOREIGN KEY (document_id) REFERENCES documents(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: logs
-- Descripción: Bitácora de auditoría de todas las acciones
--              del sistema.
-- Dependencias: documents (document_id), users (sender_user_id,
--               receiver_user_id)
-- Políticas ON DELETE:
--   - document_id      → SET NULL (preservar log aunque se
--                         elimine el documento)
--   - sender_user_id   → CASCADE (si se elimina el emisor,
--                         sus logs de envío se eliminan)
--   - receiver_user_id → SET NULL (preservar log aunque se
--                         elimine el receptor)
-- ------------------------------------------------------------
CREATE TABLE logs (
    id               BIGINT   AUTO_INCREMENT PRIMARY KEY,
    document_id      BIGINT,                                   -- Nullable para SET NULL
    sender_user_id   BIGINT   NOT NULL,
    receiver_user_id BIGINT,                                   -- Nullable para SET NULL
    action           ENUM('SEND','RECEIVE','HASH','ENCRYPT','DECRYPT','CONNECT','DISCONNECT') NOT NULL,
    protocol         ENUM('TCP','UDP'),
    timestamp        DATETIME DEFAULT CURRENT_TIMESTAMP,
    status           ENUM('SUCCESS','FAILED','IN_PROGRESS') NOT NULL,
    details          TEXT,

    -- Índices de rendimiento
    INDEX idx_log_ts     (timestamp),
    INDEX idx_log_action (action),

    -- Llaves foráneas explícitas
    CONSTRAINT fk_log_document
        FOREIGN KEY (document_id) REFERENCES documents(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_log_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_log_receiver
        FOREIGN KEY (receiver_user_id) REFERENCES users(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
