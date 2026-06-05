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
INSERT  IGNORE INTO `client_connections` VALUES (1,1,'192.168.1.24','node-1',56407,'2026-06-05 07:53:34',NULL,'TCP',1);
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
INSERT  IGNORE INTO `document_hashes` VALUES (1,1,'SHA256','f79658045e3eb334931fa0b0ef1c7586c8c4448015a19c7a72a2302b00158d7b','2026-06-05 07:53:37');
/*!40000 ALTER TABLE `document_hashes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT  IGNORE INTO `documents` VALUES (1,'msg_u1_1780646017965.txt',5,'.txt','text/plain','MESSAGE','C:\\Users\\daniel MC\\Documents\\P2P_Server\\.\\storage\\original\\3db00cd5-d631-4728-9ac5-7dfc3c3fb3d1.txt',1,'/192.168.1.24:56407','2026-06-05 07:53:37');
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `encrypted_documents`
--

LOCK TABLES `encrypted_documents` WRITE;
/*!40000 ALTER TABLE `encrypted_documents` DISABLE KEYS */;
INSERT  IGNORE INTO `encrypted_documents` VALUES (1,1,'AES256','C:\\Users\\daniel MC\\Documents\\P2P_Server\\.\\storage\\encrypted\\c4996b40-3d01-407e-940e-4b27390a6cf0.enc','SERVER_STATIC_KEY','2026-06-05 07:53:37');
/*!40000 ALTER TABLE `encrypted_documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `logs`
--

LOCK TABLES `logs` WRITE;
/*!40000 ALTER TABLE `logs` DISABLE KEYS */;
INSERT  IGNORE INTO `logs` VALUES (1,NULL,1,NULL,'CONNECT','TCP','2026-06-05 02:53:35','SUCCESS','Usuario u1 conectado desde 192.168.1.24:56407'),(2,1,1,NULL,'UPLOAD_COMPLETE','TCP','2026-06-05 02:53:38','SUCCESS','Archivo subido exitosamente por: u1'),(3,NULL,1,NULL,'SEND_MESSAGE','TCP','2026-06-05 02:53:38','SUCCESS','Mensaje de u1 (broadcast)');
/*!40000 ALTER TABLE `logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT  IGNORE INTO `users` VALUES (1,'u1','192.168.1.24','2026-06-05 07:53:34');
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

-- Dump completed on 2026-06-05  7:55:18
