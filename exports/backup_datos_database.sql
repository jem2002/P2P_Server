-- MySQL dump 10.13  Distrib 8.0.43, for Linux (x86_64)
--
-- Host: localhost    Database: messaging_system
-- ------------------------------------------------------
-- Server version	8.0.43

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
INSERT  IGNORE INTO `client_connections` VALUES (1,1,'10.140.20.156','node-1',51488,'2026-06-05 15:53:33',NULL,'TCP',1);
/*!40000 ALTER TABLE `client_connections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `comments`
--

LOCK TABLES `comments` WRITE;
/*!40000 ALTER TABLE `comments` DISABLE KEYS */;
INSERT  IGNORE INTO `comments` VALUES (1,1,1,'muy bueno','POSITIVO',97.7300,'2026-06-05 15:53:54');
/*!40000 ALTER TABLE `comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `document_hashes`
--

LOCK TABLES `document_hashes` WRITE;
/*!40000 ALTER TABLE `document_hashes` DISABLE KEYS */;
INSERT  IGNORE INTO `document_hashes` VALUES (1,1,'SHA256','af7848de2c739130955f33ae28ba4d141b25975b8c3029b103988b1524ceaf9b','2026-06-05 15:53:43');
/*!40000 ALTER TABLE `document_hashes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT  IGNORE INTO `documents` VALUES (1,'1. Use Cases-iter4.drawio.png',97340,'png','application/octet-stream','FILE','storage/original/5621ea09-0a4d-42ee-9c32-ca62a5b35053.png',1,'/10.140.20.156:51488','2026-06-05 15:53:43');
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `encrypted_documents`
--

LOCK TABLES `encrypted_documents` WRITE;
/*!40000 ALTER TABLE `encrypted_documents` DISABLE KEYS */;
INSERT  IGNORE INTO `encrypted_documents` VALUES (1,1,'AES256','storage/encrypted/363cc960-46c8-4fcc-a898-f10e8af13d6b.enc','SERVER_STATIC_KEY','2026-06-05 15:53:43');
/*!40000 ALTER TABLE `encrypted_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `logs`
--

LOCK TABLES `logs` WRITE;
/*!40000 ALTER TABLE `logs` DISABLE KEYS */;
INSERT  IGNORE INTO `logs` VALUES (1,NULL,1,NULL,'CONNECT','TCP','2026-06-05 10:53:33','SUCCESS','Usuario u1 conectado desde 10.140.20.156:51488'),(2,NULL,1,NULL,'UPLOAD_INIT','TCP','2026-06-05 10:53:44','SUCCESS','Ticket de subida generado para u1 (Archivo: 1. Use Cases-iter4.drawio.png)'),(3,1,1,NULL,'UPLOAD_COMPLETE','TCP','2026-06-05 10:53:44','SUCCESS','Archivo subido exitosamente por: u1');
/*!40000 ALTER TABLE `logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT  IGNORE INTO `users` VALUES (1,'u1','10.140.20.156','2026-06-05 15:53:33');
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

-- Dump completed on 2026-06-05 15:54:02
