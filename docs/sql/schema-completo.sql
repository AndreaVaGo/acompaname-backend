-- Esquema COMPLETO de la base de datos de Acompáñame (MySQL 8.0), solo estructura, sin datos.
--
-- Para qué: producción arranca con ddl-auto=validate, es decir, Hibernate NO crea ni cambia tablas:
-- solo comprueba que coinciden con el código. Con este script se deja una base de datos VACÍA lista.
--
-- Cómo se usa (una sola vez, con la base de datos creada y vacía):
--   mysql -u <usuario> -p <nombre_de_la_base> < docs/sql/schema-completo.sql
-- No lleva DROP TABLE: si alguna tabla ya existe, falla en vez de borrarla.
--
-- Qué NO lleva: datos. Los roles (FAMILIA, CUIDADOR, ADMIN) y la cuenta de administración
-- (ADMIN_EMAIL / ADMIN_PASSWORD) los crea la propia aplicación al arrancar.
--
-- Cómo se generó: sobre una base de datos vacía, la aplicación creó las tablas desde el código
-- (JPA_DDL_AUTO=update) y se volcó con mysqldump --no-data (25 nov 2026, estado de ACOM-250).
-- Incluye ya lo de acom-241-fichajes.sql y acom-243-fecha-alta.sql; esos dos scripts quedan
-- solo como historial para bases de datos que ya existían.
--
-- MANTENIMIENTO: cada vez que cambie una entidad hay que (1) añadir un script de migración
-- nuevo en docs/sql/ para las bases que ya existen y (2) actualizar este archivo.

SET NAMES utf8mb4;
-- Las tablas se crean en orden alfabético y se referencian entre sí: se apagan las comprobaciones
-- de claves foráneas mientras se crean y se vuelven a encender al final.
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `bloques_ocupados` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fecha` date DEFAULT NULL,
  `hora_fin` time DEFAULT NULL,
  `hora_inicio` time DEFAULT NULL,
  `motivo` varchar(100) DEFAULT NULL,
  `cuidador_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmnwqxa9bi637pcdwkkno0jgle` (`cuidador_id`),
  CONSTRAINT `FKmnwqxa9bi637pcdwkkno0jgle` FOREIGN KEY (`cuidador_id`) REFERENCES `perfilcuidadores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `codigos_login` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `codigo_hash` varchar(64) NOT NULL,
  `desafio_hash` varchar(64) NOT NULL,
  `expira_en` datetime(6) NOT NULL,
  `intentos` int NOT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo4y2d5vycxsjfb98bmqxdyebr` (`desafio_hash`),
  KEY `FKr0k7hl43gct24c5v848rxapcf` (`usuario_id`),
  CONSTRAINT `FKr0k7hl43gct24c5v848rxapcf` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `fichajes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `momento` datetime(6) DEFAULT NULL,
  `tipo` enum('FIN','INICIO') DEFAULT NULL,
  `ubicacion` enum('EN_EL_LUGAR','FUERA_DEL_LUGAR','NO_VERIFICABLE','SIN_UBICACION') DEFAULT NULL,
  `solicitud_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3697bycjs3re6jpiimersbpxo` (`solicitud_id`,`tipo`),
  CONSTRAINT `FK47w546bgf8agijo46bjkpabgp` FOREIGN KEY (`solicitud_id`) REFERENCES `solicitudes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `mensajes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fecha` datetime(6) NOT NULL,
  `leido` tinyint(1) NOT NULL DEFAULT '1',
  `texto` varchar(1000) NOT NULL,
  `emisor_id` bigint NOT NULL,
  `solicitud_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmg4oe68p9t40440gdttlg0fny` (`emisor_id`),
  KEY `FKtcuvfnvpluna41t35dk8mx3no` (`solicitud_id`),
  CONSTRAINT `FKmg4oe68p9t40440gdttlg0fny` FOREIGN KEY (`emisor_id`) REFERENCES `usuarios` (`id`),
  CONSTRAINT `FKtcuvfnvpluna41t35dk8mx3no` FOREIGN KEY (`solicitud_id`) REFERENCES `solicitudes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `pagos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `estado` enum('CANCELADO','COMPLETADO','PENDIENTE') DEFAULT NULL,
  `fecha` date DEFAULT NULL,
  `importe` decimal(38,2) DEFAULT NULL,
  `solicitud_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKiqe6oe7nxrdv74myqfm8ksjkt` (`solicitud_id`),
  CONSTRAINT `FKrg4hyplvmw9xko7u16b6ifce0` FOREIGN KEY (`solicitud_id`) REFERENCES `solicitudes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `perfilcuidadores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `anos_experiencia` int DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `ciudad` varchar(255) DEFAULT NULL,
  `disponible_ahora` bit(1) NOT NULL,
  `especialidad` varchar(255) DEFAULT NULL,
  `foto` longtext,
  `grupos_edad` varchar(255) DEFAULT NULL,
  `otras_titulaciones` varchar(255) DEFAULT NULL,
  `recargo_persona_extra` int DEFAULT NULL,
  `servicios` varchar(255) DEFAULT NULL,
  `sexo` varchar(255) DEFAULT NULL,
  `tarifa_hora` decimal(38,2) DEFAULT NULL,
  `tiene_vehiculo` bit(1) NOT NULL,
  `tipo_cuidado` varchar(255) DEFAULT NULL,
  `titulaciones` varchar(255) DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKriu4kled1idxg1ta4yn45okbh` (`usuario_id`),
  CONSTRAINT `FKbqep039c4ger5rrn2uaq71tip` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles` (
  `id_role` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `roles_usuarios` (
  `usuario_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`usuario_id`,`role_id`),
  KEY `FKcdoes04g3akhuoe7s9lovkhlp` (`role_id`),
  CONSTRAINT `FKcdoes04g3akhuoe7s9lovkhlp` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id_role`),
  CONSTRAINT `FKghiaab413trbw9sho9o48klhp` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `solicitud_personas` (
  `solicitud_id` bigint NOT NULL,
  `edad` int DEFAULT NULL,
  `grupo_edad` enum('ADOLESCENTES','ADULTOS','BEBES','DISCAPACIDAD_DEPENDENCIA','MAYORES','NINOS') DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `orden` int NOT NULL,
  PRIMARY KEY (`solicitud_id`,`orden`),
  CONSTRAINT `FKm66ayq3gg519671h5u3dnjm9j` FOREIGN KEY (`solicitud_id`) REFERENCES `solicitudes` (`id`),
  CONSTRAINT `solicitud_personas_chk_1` CHECK ((`orden` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `solicitudes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `direccion` varchar(255) DEFAULT NULL,
  `edad_paciente` int DEFAULT NULL,
  `estado` enum('ACEPTADA','COMPLETADA','PENDIENTE','RECHAZADA') DEFAULT NULL,
  `fecha_aceptacion` datetime(6) DEFAULT NULL,
  `fecha_creacion` datetime(6) DEFAULT NULL,
  `fecha_cuidado` date DEFAULT NULL,
  `fecha_fin` date DEFAULT NULL,
  `grupo_edad` enum('ADOLESCENTES','ADULTOS','BEBES','DISCAPACIDAD_DEPENDENCIA','MAYORES','NINOS') DEFAULT NULL,
  `hora_fin` time DEFAULT NULL,
  `hora_inicio` time DEFAULT NULL,
  `necesidades` varchar(255) DEFAULT NULL,
  `nombre_paciente` varchar(255) DEFAULT NULL,
  `notas` varchar(255) DEFAULT NULL,
  `tipo_cuidado` varchar(255) DEFAULT NULL,
  `cuidador_id` bigint DEFAULT NULL,
  `familia_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1hckrlsto6f13gvtmq8m0m6a9` (`cuidador_id`),
  KEY `FKcb9145nr5j5nbki1ae319ohr6` (`familia_id`),
  CONSTRAINT `FK1hckrlsto6f13gvtmq8m0m6a9` FOREIGN KEY (`cuidador_id`) REFERENCES `perfilcuidadores` (`id`),
  CONSTRAINT `FKcb9145nr5j5nbki1ae319ohr6` FOREIGN KEY (`familia_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `sugerencias` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fecha` datetime(6) DEFAULT NULL,
  `texto` varchar(1000) DEFAULT NULL,
  `usuario_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjr3r2tpm7t2u1t1vdntjoqsjw` (`usuario_id`),
  CONSTRAINT `FKjr3r2tpm7t2u1t1vdntjoqsjw` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `suscripciones_calendario` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cuidador_id` bigint NOT NULL,
  `ultima_sincronizacion` datetime(6) DEFAULT NULL,
  `ultimo_error` varchar(255) DEFAULT NULL,
  `url` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_suscripciones_calendario_cuidador` (`cuidador_id`),
  CONSTRAINT `FK_suscripciones_calendario_cuidador` FOREIGN KEY (`cuidador_id`) REFERENCES `perfilcuidadores` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tokens_recuperacion_password` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expira_en` datetime(6) NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `usuario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjh8nj9ha4ti36iva7x7egenup` (`token_hash`),
  KEY `FK617xct0c4f4gg49twhspimkfr` (`usuario_id`),
  CONSTRAINT `FK617xct0c4f4gg49twhspimkfr` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activo` tinyint(1) NOT NULL DEFAULT '1',
  `dos_pasos` tinyint(1) NOT NULL DEFAULT '0',
  `email` varchar(255) NOT NULL,
  `fecha_alta` datetime(6) DEFAULT NULL,
  `nombre` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `telefono` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuarios_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `valoraciones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comentario` varchar(255) DEFAULT NULL,
  `fecha` date DEFAULT NULL,
  `puntuacion` int DEFAULT NULL,
  `solicitud_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKtgg8bkinxwuaja378c0hdp2v5` (`solicitud_id`),
  CONSTRAINT `FKf3suplmilfwip95l2u46brmsy` FOREIGN KEY (`solicitud_id`) REFERENCES `solicitudes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `verificaciones` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `estado` enum('PENDIENTE','RECHAZADO','VERIFICADO') NOT NULL,
  `motivo_rechazo` varchar(200) DEFAULT NULL,
  `referencia` varchar(100) NOT NULL,
  `revisada_en` datetime(6) DEFAULT NULL,
  `revisada_por` varchar(100) DEFAULT NULL,
  `solicitada_en` datetime(6) NOT NULL,
  `tipo` enum('ANTECEDENTES','TITULACION') NOT NULL,
  `cuidador_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKd369f34bnwhfriepp14v1xv3f` (`cuidador_id`,`tipo`),
  CONSTRAINT `FKd5wui52h5jl1akfu5x69fib9s` FOREIGN KEY (`cuidador_id`) REFERENCES `perfilcuidadores` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
