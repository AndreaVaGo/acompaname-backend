-- ACOM-260: cerrar las sesiones abiertas al cambiar o restablecer la contraseña
-- Producción usa ddl-auto=validate, así que esta columna hay que crearla A MANO
-- ANTES de desplegar la versión del back que trae el campo sesionesValidasDesde.
-- En local (ddl-auto=update) Hibernate la crea solo.
-- Vacía (NULL) significa que a esa persona no se le ha invalidado ningún token.

ALTER TABLE usuarios ADD COLUMN sesiones_validas_desde DATETIME(6) NULL;
