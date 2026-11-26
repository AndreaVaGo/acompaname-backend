-- ACOM-236: calendario sincronizado con uno externo (Google, Outlook, iCloud)
-- Producción usa ddl-auto=validate, así que esta tabla hay que crearla A MANO
-- ANTES de desplegar la versión del back que trae la sincronización.
-- En local (ddl-auto=update) Hibernate la crea solo.
-- La columna url guarda la dirección SECRETA del calendario: no se devuelve nunca por la API.

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
