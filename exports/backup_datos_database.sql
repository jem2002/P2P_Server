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
INSERT  IGNORE INTO `client_connections` VALUES (1,1,'192.168.1.24','node-1',55345,'2026-06-05 08:23:21','2026-06-05 08:28:23','TCP',0),(2,2,'192.168.1.24','node-2',55397,'2026-06-05 08:24:40',NULL,'TCP',1);
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
INSERT  IGNORE INTO `document_hashes` VALUES (1,1,'SHA256','3c1d9213f8765a2a3628d1c7721fa46d60f0601845ad1892374e6d32ee270be1','2026-06-05 08:23:26'),(2,2,'SHA256','b1152a1708fe6eca1779a9edb69a0e4c65b39bd2d22a5183db7817a29fba2d72','2026-06-05 08:26:06');
/*!40000 ALTER TABLE `document_hashes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT  IGNORE INTO `documents` VALUES (1,'msg_u1_1780647806410.txt',4,'.txt','text/plain','MESSAGE','storage/original/101b297f-f543-45af-a5f4-d6d487403650.txt',1,'/192.168.1.24:55345','2026-06-05 08:23:26'),(2,'docker-compose.yml',467,'yml','application/octet-stream','FILE','storage/original/817f6cc4-184c-4a18-8e84-8200fca189b5.yml',1,'replicado','2026-06-05 08:26:06');
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `encrypted_documents`
--

LOCK TABLES `encrypted_documents` WRITE;
/*!40000 ALTER TABLE `encrypted_documents` DISABLE KEYS */;
INSERT  IGNORE INTO `encrypted_documents` VALUES (1,1,'AES256','storage/encrypted/d5f67d42-a67c-47e9-a583-9b9bc5806f35.enc','SERVER_STATIC_KEY','2026-06-05 08:23:26'),(2,2,'AES256','storage/encrypted/f45aa9a1-8b20-4bd7-b721-0d2318e890a8.enc','SERVER_STATIC_KEY','2026-06-05 08:26:06');
/*!40000 ALTER TABLE `encrypted_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `logs`
--

LOCK TABLES `logs` WRITE;
/*!40000 ALTER TABLE `logs` DISABLE KEYS */;
INSERT  IGNORE INTO `logs` VALUES (1,NULL,1,NULL,'CONNECT','TCP','2026-06-05 03:23:22','SUCCESS','Usuario u1 conectado desde 192.168.1.24:55345'),(2,1,1,NULL,'UPLOAD_COMPLETE','TCP','2026-06-05 03:23:26','SUCCESS','Archivo subido exitosamente por: u1'),(3,NULL,1,NULL,'SEND_MESSAGE','TCP','2026-06-05 03:23:26','SUCCESS','Mensaje de u1 (broadcast)'),(4,NULL,2,NULL,'CONNECT','TCP','2026-06-05 03:24:40','SUCCESS','Usuario u2 conectado desde 192.168.1.24:55397'),(5,1,2,NULL,'DOWNLOAD_INIT','TCP','2026-06-05 03:24:50','SUCCESS','Ticket de descarga (HSH) generado para u2 (ID doc: 1)'),(6,1,2,NULL,'DOWNLOAD_COMPLETE','TCP','2026-06-05 03:24:50','SUCCESS','Descarga finalizada en modo: HASH'),(7,1,2,NULL,'DOWNLOAD_INIT','TCP','2026-06-05 03:25:14','SUCCESS','Ticket de descarga (ENC) generado para u2 (ID doc: 1)'),(8,1,2,NULL,'DOWNLOAD_COMPLETE','TCP','2026-06-05 03:25:14','SUCCESS','Descarga finalizada en modo: ENCRYPTED'),(9,2,1,NULL,'UPLOAD_COMPLETE','TCP','2026-06-05 03:26:06','SUCCESS','Archivo subido exitosamente por: u1'),(10,2,2,NULL,'DOWNLOAD_INIT','TCP','2026-06-05 03:27:16','SUCCESS','Ticket de descarga (ORG) generado para u2 (ID doc: 2)'),(11,2,2,NULL,'DOWNLOAD_COMPLETE','TCP','2026-06-05 03:27:16','SUCCESS','Descarga finalizada en modo: ORIGINAL'),(12,2,2,NULL,'DOWNLOAD_INIT','TCP','2026-06-05 03:27:21','SUCCESS','Ticket de descarga (HSH) generado para u2 (ID doc: 2)'),(13,2,2,NULL,'DOWNLOAD_COMPLETE','TCP','2026-06-05 03:27:21','SUCCESS','Descarga finalizada en modo: HASH'),(14,2,2,NULL,'DOWNLOAD_INIT','TCP','2026-06-05 03:27:55','SUCCESS','Ticket de descarga (ORG) generado para u2 (ID doc: 2)'),(15,2,2,NULL,'DOWNLOAD_COMPLETE','TCP','2026-06-05 03:27:55','SUCCESS','Descarga finalizada en modo: ORIGINAL');
/*!40000 ALTER TABLE `logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT  IGNORE INTO `users` VALUES (1,'u1','192.168.1.24','2026-06-05 08:23:21'),(2,'u2','192.168.1.24','2026-06-05 08:24:40');
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

-- Dump completed on 2026-06-05  8:28:59
