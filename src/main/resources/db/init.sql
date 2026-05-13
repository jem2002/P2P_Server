-- ============================================================
-- SCRIPT 1: Inicialización del Motor MySQL 8.0+
-- Proyecto: Messaging Server
-- Propósito: Crear la base de datos, usuario dedicado y
--            otorgar privilegios mínimos necesarios (Principio
--            de Menor Privilegio).
-- Ejecución: Conectarse como root o usuario con GRANT OPTION.
--   mysql -u root -p < init.sql
-- ============================================================

-- 1. Crear la base de datos con soporte completo para
--    emojis y caracteres especiales (utf8mb4).
CREATE DATABASE IF NOT EXISTS messaging_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Crear usuario dedicado de aplicación.
--    - Se restringe el acceso únicamente desde localhost.
--    - La contraseña usa el plugin de autenticación por defecto
--      de MySQL 8.0+ (caching_sha2_password).
--    IMPORTANTE: Cambiar la contraseña antes de desplegar en
--    producción. Usar variables de entorno o un vault de secretos.
CREATE USER IF NOT EXISTS 'messaging_app'@'localhost'
    IDENTIFIED BY 'M3ss@g1ng_S3cur3_2026!';

-- 3. Otorgar únicamente privilegios CRUD + DDL básico.
--    - SELECT, INSERT, UPDATE, DELETE  → operaciones CRUD
--    - CREATE, ALTER, DROP, INDEX      → DDL básico (migraciones)
--    - REFERENCES                      → gestión de llaves foráneas
--    NO se otorga: GRANT OPTION, SUPER, FILE, PROCESS, etc.
GRANT SELECT, INSERT, UPDATE, DELETE,
      CREATE, ALTER, DROP, INDEX, REFERENCES
    ON messaging_system.*
    TO 'messaging_app'@'localhost';

-- 4. Aplicar los cambios de privilegios inmediatamente.
FLUSH PRIVILEGES;
