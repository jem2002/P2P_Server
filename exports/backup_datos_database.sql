-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: messaging_system
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `client_connections`
--

LOCK TABLES `client_connections` WRITE;
/*!40000 ALTER TABLE `client_connections` DISABLE KEYS */;
INSERT  IGNORE INTO `client_connections` VALUES (1,1,'172.18.16.1','node-1',51291,'2026-06-09 02:24:07',NULL,'TCP',1);
/*!40000 ALTER TABLE `client_connections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `comments`
--

LOCK TABLES `comments` WRITE;
/*!40000 ALTER TABLE `comments` DISABLE KEYS */;
/*!40000 ALTER TABLE `comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `document_hashes`
--

LOCK TABLES `document_hashes` WRITE;
/*!40000 ALTER TABLE `document_hashes` DISABLE KEYS */;
INSERT  IGNORE INTO `document_hashes` VALUES (1,1,'SHA256','b1152a1708fe6eca1779a9edb69a0e4c65b39bd2d22a5183db7817a29fba2d72','2026-06-09 02:24:16');
/*!40000 ALTER TABLE `document_hashes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT  IGNORE INTO `documents` VALUES (1,'docker-compose.yml',467,'yml','application/octet-stream','FILE','storage/original/4efbc230-ebcd-4dbd-9a0c-8a0a25730b7a.yml',1,'/172.18.16.1:51291','2026-06-09 02:24:16');
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `encrypted_documents`
--

LOCK TABLES `encrypted_documents` WRITE;
/*!40000 ALTER TABLE `encrypted_documents` DISABLE KEYS */;
INSERT  IGNORE INTO `encrypted_documents` VALUES (1,1,'AES256','storage/encrypted/12c3f996-9f22-4522-9862-82adbe3a1015.enc','SERVER_STATIC_KEY','2026-06-09 02:24:16');
/*!40000 ALTER TABLE `encrypted_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `logs`
--

LOCK TABLES `logs` WRITE;
/*!40000 ALTER TABLE `logs` DISABLE KEYS */;
INSERT  IGNORE INTO `logs` VALUES (1,NULL,1,NULL,'CONNECT','TCP','2026-06-08 21:24:08','SUCCESS','Usuario u1 conectado desde 172.18.16.1:51291'),(2,NULL,1,NULL,'UPLOAD_INIT','TCP','2026-06-08 21:24:17','SUCCESS','Ticket de subida generado para u1 (Archivo: docker-compose.yml)'),(3,1,1,NULL,'UPLOAD_COMPLETE','TCP','2026-06-08 21:24:17','SUCCESS','Archivo subido exitosamente por: u1');
/*!40000 ALTER TABLE `logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT  IGNORE INTO `users` VALUES (1,'u1','172.18.16.1','2026-06-09 02:24:07');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-09  2:24:40
