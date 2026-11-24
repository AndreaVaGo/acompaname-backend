-- ACOM-241: fichaje de inicio y fin del servicio
-- Producción usa ddl-auto=validate, así que esta tabla hay que crearla A MANO
-- ANTES de desplegar la versión del back que trae los fichajes.
-- En local (ddl-auto=update) Hibernate la crea sola.
--
-- IMPORTANTE: antes de aplicarlo, comprueba que coincide con lo que genera Hibernate en local:
--   SHOW CREATE TABLE fichajes;
-- (por ejemplo, Hibernate puede crear las columnas "tipo" y "ubicacion" como ENUM en vez de VARCHAR).
--
-- Privacidad: la tabla NO tiene columnas de latitud ni longitud. Solo guarda la hora y el resultado.

CREATE TABLE fichajes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    solicitud_id BIGINT NOT NULL,
    tipo ENUM ('INICIO', 'FIN'),
    momento DATETIME(6),
    ubicacion ENUM ('EN_EL_LUGAR', 'FUERA_DEL_LUGAR', 'SIN_UBICACION', 'NO_VERIFICABLE'),
    PRIMARY KEY (id),
    UNIQUE KEY uk_fichajes_solicitud_tipo (solicitud_id, tipo),
    CONSTRAINT fk_fichajes_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes (id)
);
